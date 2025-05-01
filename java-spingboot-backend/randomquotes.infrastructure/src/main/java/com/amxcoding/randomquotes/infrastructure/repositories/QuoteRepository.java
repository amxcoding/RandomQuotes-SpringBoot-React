package com.amxcoding.randomquotes.infrastructure.repositories;

import com.amxcoding.randomquotes.application.exceptions.repositories.QuotePersistenceException;
import com.amxcoding.randomquotes.application.interfaces.repositories.IQuoteRepository;
import com.amxcoding.randomquotes.domain.entities.Quote;
import com.amxcoding.randomquotes.infrastructure.persistence.mappers.QuoteEntityMapper;
import com.amxcoding.randomquotes.infrastructure.persistence.models.QuoteEntity;
import com.amxcoding.randomquotes.infrastructure.persistence.r2dbcs.IQuoteR2dbcRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.Optional;

@SuppressWarnings("LoggingPlaceholderCountMatchesArgumentCount")
@Repository
public class QuoteRepository implements IQuoteRepository {

    private final QuoteEntityMapper quoteEntityMapper;
    private final IQuoteR2dbcRepository quoteR2dbcRepository;
    private static final Logger logger = LoggerFactory.getLogger(QuoteRepository.class);

    public QuoteRepository(IQuoteR2dbcRepository quoteR2dbcRepository, QuoteEntityMapper quoteEntityMapper) {
        this.quoteR2dbcRepository = quoteR2dbcRepository;
        this.quoteEntityMapper = quoteEntityMapper;
    }


    @Override
    public Mono<Quote> saveQuote(Quote domainQuote) {
        QuoteEntity entityToSave = quoteEntityMapper.toQuoteEntity(domainQuote);

        return quoteR2dbcRepository.save(entityToSave)
                .onErrorMap(ex -> {
                    // Can throw DupllicateKeyException, due to unique constraint on text_author_hash
                    // just log
                    logger.error("QuoteRepository-createQuote", "Error creating a quote: {}", ex.getMessage(), ex);

                    return new QuotePersistenceException("Error saving quote: " + ex);
                })
                .map(quoteEntityMapper::toDomainQuote);
    }

    @Override
    public Mono<Optional<Quote>> findById(Long id) {
        return quoteR2dbcRepository.findById(id)
                .map(quoteEntityMapper::toDomainQuote)
                .map(Optional::of) // Wrap in Optional
                .defaultIfEmpty(Optional.empty()); // Return empty Optional if not found
    }

}