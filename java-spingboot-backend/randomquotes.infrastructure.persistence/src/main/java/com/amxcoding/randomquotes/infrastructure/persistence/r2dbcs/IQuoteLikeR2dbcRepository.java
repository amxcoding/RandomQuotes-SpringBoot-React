package com.amxcoding.randomquotes.infrastructure.persistence.r2dbcs;

import com.amxcoding.randomquotes.infrastructure.persistence.models.QuoteLikeEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

public interface IQuoteLikeR2dbcRepository extends R2dbcRepository<QuoteLikeEntity, Long> {
    Mono<QuoteLikeEntity> findByUserIdAndQuoteId(String userId, Long quoteId);
    Mono<Void> deleteByUserIdAndQuoteId(String userId, Long quoteId);

}