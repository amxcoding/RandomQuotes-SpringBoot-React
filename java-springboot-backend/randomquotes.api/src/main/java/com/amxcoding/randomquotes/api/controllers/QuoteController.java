package com.amxcoding.randomquotes.api.controllers;


import com.amxcoding.randomquotes.api.interfaces.IAnonymousUserService;
import com.amxcoding.randomquotes.api.interfaces.IQuoteBroadCaster;
import com.amxcoding.randomquotes.api.mappers.QuoteMapper;
import com.amxcoding.randomquotes.api.models.quote.QuoteResponse;
import com.amxcoding.randomquotes.application.interfaces.services.IQuoteLikeService;
import com.amxcoding.randomquotes.application.interfaces.services.IQuoteService;
import com.amxcoding.randomquotes.domain.entities.Quote;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Optional;

@RestController
@RequestMapping("api/v1/quotes")
public class QuoteController {

    private final IQuoteService quoteService;
    private final IQuoteLikeService quoteLikeService;
    private final QuoteMapper quoteMapper;
    private final IAnonymousUserService anonymousUserService;
    private final IQuoteBroadCaster quoteBroadCaster;

    public QuoteController(IQuoteService quoteService,
                           IQuoteLikeService quoteLikeService,
                           QuoteMapper quoteMapper,
                           IAnonymousUserService anonymousUserService,
                           IQuoteBroadCaster quoteBroadCaster) {
        this.quoteService = quoteService;
        this.quoteLikeService = quoteLikeService;
        this.quoteMapper = quoteMapper;
        this.anonymousUserService = anonymousUserService;
        this.quoteBroadCaster = quoteBroadCaster;
    }

    /**
     * Returns a random quote and whether the current user has liked it based on a tracking cookie.
     */
    @GetMapping(path = "/random", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public Mono<ResponseEntity<QuoteResponse>> getRandomQuote(ServerHttpRequest request, ServerHttpResponse response) {
        String userId = anonymousUserService.getOrCreateAnonymousUserId(request, response);

        return fetchQuoteAndSetUserLike(quoteService.getRandomQuote(), userId)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }


    /**
     * Likes the specified quote for the current anonymous user and returns the updated quote.
     * Also broadcast the liked quote
     */
    @GetMapping(path = "/{id}/like", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public Mono<ResponseEntity<QuoteResponse>> likeQuoteByHash(@PathVariable("id") Long id,
                                                               ServerHttpRequest request,
                                                               ServerHttpResponse response) {
        String userId = anonymousUserService.getOrCreateAnonymousUserId(request, response);

        return quoteLikeService.likeQuote(userId, id)
                .then(Mono.defer(() -> fetchQuoteAndSetUserLike(quoteService.getQuoteById(id), userId)
                                .flatMap(quoteResponse ->
                                        quoteBroadCaster.emit(quoteResponse).thenReturn(quoteResponse)))
                        .map(ResponseEntity::ok)
                        .defaultIfEmpty(ResponseEntity.notFound().build()));

    }

    /**
     * Unlikes the specified quote for the current anonymous user and returns the updated quote.
     * The entry (userId, quoteId) is deleted
     */
    @DeleteMapping(path = "/{id}/like", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public Mono<ResponseEntity<QuoteResponse>> unlikeQuoteByHash(@PathVariable("id") Long id,
                                                                 ServerHttpRequest request,
                                                                 ServerHttpResponse response) {

        String userId = anonymousUserService.getOrCreateAnonymousUserId(request, response);

        return quoteLikeService.unlikeQuote(userId, id)
                .then(Mono.defer(() ->
                        fetchQuoteAndSetUserLike(quoteService.getQuoteById(id), userId)
                ))
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * Converts the given quote to a response object and attaches the user's like status.
     */
    private Mono<QuoteResponse> fetchQuoteAndSetUserLike(Mono<Optional<Quote>> monoOptionalQuote, String userId) {
        return monoOptionalQuote
                .flatMap(optionalQuote -> {
                    if (optionalQuote.isPresent()) {
                        Quote quote = optionalQuote.get();

                        return quoteLikeService.checkUserLike(userId, quote.getId())
                                .map(isLiked -> {
                                    QuoteResponse quoteResponse = quoteMapper.toQuoteResponse(quote);
                                    quoteResponse.setIsLiked(isLiked);
                                    return quoteResponse;
                                });

                    } else {
                        // If quoteService returned an empty Optional
                        return Mono.empty();
                    }
                });
    }


}
