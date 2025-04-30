package com.amxcoding.randomquotes.application.interfaces.services;

import com.amxcoding.randomquotes.domain.entities.Quote;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface IQuoteService {
    CompletableFuture<Optional<Quote>> fetchRandomQuote();
}
