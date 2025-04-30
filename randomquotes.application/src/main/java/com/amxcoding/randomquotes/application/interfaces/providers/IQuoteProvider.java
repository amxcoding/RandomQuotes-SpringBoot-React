package com.amxcoding.randomquotes.application.interfaces.providers;

import com.amxcoding.randomquotes.domain.entities.Quote;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface IQuoteProvider {
    CompletableFuture<Optional<Quote>> fetchRandomQuote();
}
