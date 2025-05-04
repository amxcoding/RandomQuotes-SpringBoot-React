package com.amxcoding.randomquotes.application.interfaces.services;

import com.amxcoding.randomquotes.domain.entities.Quote;
import reactor.core.publisher.Mono;

import java.util.Optional;

public interface IQuoteService {
    Mono<Optional<Quote>> getRandomQuote();
    Mono<Optional<Quote>> getQuoteById(Long quoteId);

}
