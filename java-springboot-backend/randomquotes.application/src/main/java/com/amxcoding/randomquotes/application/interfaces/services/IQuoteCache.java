package com.amxcoding.randomquotes.application.interfaces.services;

import com.amxcoding.randomquotes.domain.entities.Quote;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

public interface IQuoteCache {
    Mono<Optional<List<Quote>>> getQuotes();
}

