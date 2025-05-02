package com.amxcoding.randomquotes.application.interfaces.providers;

import com.amxcoding.randomquotes.domain.entities.Quote;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface IQuoteProvider {
//    Mono<Optional<Quote>> fetchRandomQuote();
    Mono<Optional<List<Quote>>> fetchQuotes();
}
