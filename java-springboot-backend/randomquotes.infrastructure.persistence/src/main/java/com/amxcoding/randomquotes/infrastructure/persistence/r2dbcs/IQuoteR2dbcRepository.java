package com.amxcoding.randomquotes.infrastructure.persistence.r2dbcs;

import com.amxcoding.randomquotes.infrastructure.persistence.models.ZenQuoteEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

public interface IQuoteR2dbcRepository extends R2dbcRepository<ZenQuoteEntity, Long> {
    Mono<ZenQuoteEntity> findByTextAuthorHash(String textAuthorHash);
}
