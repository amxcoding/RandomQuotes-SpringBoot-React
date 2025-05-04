package com.amxcoding.randomquotes.application.interfaces.repositories;

import com.amxcoding.randomquotes.domain.entities.Quote;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

public interface IQuoteRepository {
    Mono<Void> bulkInsertQuotesIgnoreConflicts(List<Quote> quotes, String providerName);
    Mono<Optional<Quote>> findByTextAuthorHash(String textAuthorHash);
    Mono<Optional<Quote>> findById(Long id);
    Mono<Boolean> incrementLikeCount(Long id);
    Mono<Boolean> decrementLikeCount(Long id);
    Mono<Long> count();
    Mono<Long> countByProvider(String provider);
    Mono<Optional<List<Quote>>>findRandomQuotes(int amount);
    Flux<Quote> findAllQuotes(int limit);
}
