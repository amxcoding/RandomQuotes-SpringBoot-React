package com.amxcoding.randomquotes.infrastructure.repositories;

import com.amxcoding.randomquotes.application.exceptions.repositories.QuoteLikePersistenceException;
import com.amxcoding.randomquotes.application.interfaces.repositories.IQuoteLikeRepository;
import com.amxcoding.randomquotes.domain.entities.QuoteLike;
import com.amxcoding.randomquotes.infrastructure.persistence.mappers.QuoteLikeEntityMapper;
import com.amxcoding.randomquotes.infrastructure.persistence.models.QuoteLikeEntity;
import com.amxcoding.randomquotes.infrastructure.persistence.r2dbcs.IQuoteLikeR2dbcRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Repository
public class QuoteLikeRepository implements IQuoteLikeRepository {

    private final QuoteLikeEntityMapper quoteLikeEntityMapper;
    private final IQuoteLikeR2dbcRepository quoteLikeR2dbcRepository;
    private static final Logger logger = LoggerFactory.getLogger(QuoteLikeRepository.class);

    public QuoteLikeRepository(IQuoteLikeR2dbcRepository quoteLikeR2dbcRepository,
                           QuoteLikeEntityMapper quoteLikeEntityMapper) {
        this.quoteLikeR2dbcRepository = quoteLikeR2dbcRepository;
        this.quoteLikeEntityMapper = quoteLikeEntityMapper;
    }


    @Override
    public Mono<QuoteLike> saveQuoteLike(QuoteLike quoteLike) {
        QuoteLikeEntity entityToSave = quoteLikeEntityMapper.toQuoteLikeEntity(quoteLike);

        return quoteLikeR2dbcRepository.save(entityToSave)
                .onErrorMap(ex -> {
                    // Can throw DuplicateKeyException, due to unique constraint on (userId, quoteId)
                    logger.error("Error creating a quote: {}", ex.getMessage(), ex);
                    return new QuoteLikePersistenceException("Error saving quoteLike: " + ex);
                })
                .map(quoteLikeEntityMapper::toQuoteLike);
    }


    @Override
    public Mono<Optional<QuoteLike>> findByUserIdAndQuoteId(String userId, Long quoteId) {
        return quoteLikeR2dbcRepository.findByUserIdAndQuoteId(userId, quoteId)
                .onErrorMap(ex -> {
                    logger.error("Error getting quoteLike by (userId, quoteId): ({}, {})", userId, quoteId, ex);
                    return new QuoteLikePersistenceException("Error getting quoteLike by userId and quoteId: " + ex);
                })
                .map(quoteLikeEntityMapper::toQuoteLike)
                .map(Optional::of)
                .defaultIfEmpty(Optional.empty());
    }


    @Override
    public Mono<Void> deleteByUserIdAndQuoteId(String userId, Long quoteId) {
        return quoteLikeR2dbcRepository.deleteByUserIdAndQuoteId(userId, quoteId)
                .onErrorMap(ex -> {
                    logger.error("Error deleting quoteLike by (userId, quoteId): ({}, {})", userId, quoteId, ex);
                    return new QuoteLikePersistenceException("Error deleting quoteLike by userId and quoteId: " + ex);
                });
    }
}
