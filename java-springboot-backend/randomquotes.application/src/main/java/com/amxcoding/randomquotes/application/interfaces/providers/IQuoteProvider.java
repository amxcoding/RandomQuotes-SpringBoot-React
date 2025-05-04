package com.amxcoding.randomquotes.application.interfaces.providers;

import com.amxcoding.randomquotes.domain.entities.Quote;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

public interface IQuoteProvider {
    Mono<Optional<List<Quote>>> fetchQuotes();
    String getProviderName();
}
