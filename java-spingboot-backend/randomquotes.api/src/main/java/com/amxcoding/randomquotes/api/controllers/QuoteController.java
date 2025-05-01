package com.amxcoding.randomquotes.api.controllers;


import com.amxcoding.randomquotes.api.interfaces.IAnonymousUserService;
import com.amxcoding.randomquotes.api.mappers.quote.QuoteMapper;
import com.amxcoding.randomquotes.api.models.quote.CreateQuoteRequest;
import com.amxcoding.randomquotes.api.models.quote.QuoteResponse;
import com.amxcoding.randomquotes.application.exceptions.repositories.QuotePersistenceException;
import com.amxcoding.randomquotes.application.interfaces.services.IQuoteService;
import com.amxcoding.randomquotes.domain.entities.Quote;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("api/v1/quotes")
public class QuoteController {

    private final IQuoteService quoteService;
    private final QuoteMapper quoteMapper;
    private final IAnonymousUserService anonymousUserService;
    private final ObjectMapper objectMapper; // Inject ObjectMapper

    public QuoteController(IQuoteService quoteService,
                           QuoteMapper quoteMapper,
                           IAnonymousUserService anonymousUserService,
                           ObjectMapper objectMapper) {
        this.quoteService = quoteService;
        this.quoteMapper = quoteMapper;
        this.anonymousUserService = anonymousUserService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/random")
    public Mono<ResponseEntity<QuoteResponse>> getRandomQuote(ServerHttpRequest request, ServerHttpResponse response) {
        // Only interested in side effect of creating a userId and setting the cookies
        anonymousUserService.getOrCreateAnonymousUserId(request, response);

        return quoteService.getRandomQuote()
                .map(optionalQuote -> optionalQuote
                        .map(quoteMapper::toQuoteResponse)
                        .map(ResponseEntity::ok)
                        .orElseGet(() -> ResponseEntity.notFound().build()))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Mono<ResponseEntity<QuoteResponse>> createQuote(@RequestBody CreateQuoteRequest quoteRequest) {
        Quote quoteToCreate = quoteMapper.toQuote(quoteRequest);

        return quoteService
                .createQuote(quoteToCreate)
                .map(createdQuote -> {
                    QuoteResponse response = quoteMapper.toQuoteResponse(createdQuote);
                    return ResponseEntity
                            .status(HttpStatus.CREATED)
                            .body(response);
                });
    }

    @PatchMapping(path = "/{id}", consumes = "application/json-patch+json")
    public Mono<ResponseEntity<QuoteResponse>> patchQuote(
            @PathVariable("id") Long id,
            @RequestBody Mono<JsonPatch> patchMono) { // Receive JsonPatch reactively

        return quoteService.getQuoteById(id)
                .flatMap(optionalQuote -> optionalQuote
                        .map(Mono::just) // If found, wrap in Mono
                        .orElseGet(() -> Mono.error(new QuotePersistenceException("Quote not found")))
                )
                .zipWith(patchMono)
                .flatMap(tuple -> {
                    Quote originalQuote = tuple.getT1();
                    JsonPatch patch = tuple.getT2();

                    return applyPatch(originalQuote, patch);
                })
                .map(updatedQuote -> ResponseEntity.ok(quoteMapper.toQuoteResponse(updatedQuote)));
        // No need to handle errors locally, global exception handler will take care of them
    }


    private Mono<Quote> applyPatch(Quote originalQuote, JsonPatch patch) {
        try {
            JsonNode originalNode = objectMapper.valueToTree(originalQuote);
            JsonNode patchedNode = patch.apply(originalNode);
            Quote patchedQuote = objectMapper.treeToValue(patchedNode, Quote.class);
            patchedQuote.setId(originalQuote.getId());
            return quoteService.updateQuote(patchedQuote);

        } catch (JsonPatchException | com.fasterxml.jackson.core.JsonProcessingException e) {
            return Mono.error(new QuotePersistenceException("Invalid patch format or application failed: "+ e));
        } catch (Exception e) {
            return Mono.error(new QuotePersistenceException("Error applying patch: " + e));
        }
    }
}
