package com.amxcoding.randomquotes.infrastructure.persistence.r2dbcs;

import com.amxcoding.randomquotes.infrastructure.persistence.models.QuoteEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

public interface IQuoteR2dbcRepository extends R2dbcRepository<QuoteEntity, Long> {
    Mono<QuoteEntity> findByTextAuthorHash(String textAuthorHash);
}
