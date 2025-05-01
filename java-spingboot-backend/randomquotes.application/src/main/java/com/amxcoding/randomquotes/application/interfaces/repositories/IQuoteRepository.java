package com.amxcoding.randomquotes.application.interfaces.repositories;

import com.amxcoding.randomquotes.domain.entities.Quote;
import reactor.core.publisher.Mono;

import java.util.Optional;

public interface IQuoteRepository {
    Mono<Quote> saveQuote(Quote domainQuote);
    Mono<Optional<Quote>> findById(Long id);
//    Mono<Quote> findByTextAuthorHash(String textAuthorHash);
}
