package com.amxcoding.randomquotes.application.interfaces.repositories;

import com.amxcoding.randomquotes.domain.entities.QuoteLike;
import reactor.core.publisher.Mono;

import java.util.Optional;

public interface IQuoteLikeRepository {
    Mono<QuoteLike> saveQuoteLike(QuoteLike quoteLike);
    Mono<Optional<QuoteLike>> findByUserIdAndQuoteId(String userId, Long quoteId);
    Mono<Void> deleteByUserIdAndQuoteId(String userId, Long quoteId);
}