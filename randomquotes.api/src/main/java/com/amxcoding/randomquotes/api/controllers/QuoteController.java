package com.amxcoding.randomquotes.api.controllers;


import com.amxcoding.randomquotes.api.mappers.QuoteMapper;
import com.amxcoding.randomquotes.api.models.QuoteResponse;
import com.amxcoding.randomquotes.application.interfaces.services.IQuoteService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

@RestController
public class QuoteController {

    private final IQuoteService quoteService;
    private final QuoteMapper quoteMapper;

    public QuoteController(IQuoteService quoteService, QuoteMapper quoteMapper) {
        this.quoteService = quoteService;
        this.quoteMapper = quoteMapper;
    }

    @GetMapping("/random")
    public CompletableFuture<ResponseEntity<QuoteResponse>> getRandomQuote() {
        return quoteService.fetchRandomQuote()
                .thenApply(optionalQuote -> optionalQuote
                        .map(quoteMapper::toQuoteResponse)
                        .map(ResponseEntity::ok)
                        .orElseGet(() -> ResponseEntity.notFound().build()));
    }
}
