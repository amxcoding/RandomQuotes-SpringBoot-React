package com.amxcoding.randomquotes.application.interfaces.repositories;

import com.amxcoding.randomquotes.domain.entities.Quote;
import reactor.core.publisher.Mono;

import javax.swing.text.html.Option;
import java.util.List;
import java.util.Optional;

public interface IQuoteRepository {
    Mono<Void> bulkInsertQuotesIgnoreConflicts(List<Quote> quotes);
    Mono<Optional<Quote>> findByTextAuthorHash(String textAuthorHash);
    Mono<Optional<Quote>> findById(Long id);
    Mono<Quote> saveQuote(Quote domainQuote);
    Mono<Boolean> incrementLikeCount(Long id);
    Mono<Boolean> decrementLikeCount(Long id);
}
