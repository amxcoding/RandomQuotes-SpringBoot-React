package com.amxcoding.randomquotes.infrastructure.repositories;

import com.amxcoding.randomquotes.application.exceptions.repositories.QuotePersistenceException;
import com.amxcoding.randomquotes.application.interfaces.repositories.IQuoteRepository;
import com.amxcoding.randomquotes.domain.entities.Quote;
import com.amxcoding.randomquotes.infrastructure.persistence.mappers.QuoteEntityMapper;
import com.amxcoding.randomquotes.infrastructure.persistence.models.QuoteEntity;
import com.amxcoding.randomquotes.infrastructure.persistence.r2dbcs.IQuoteR2dbcRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class QuoteRepository implements IQuoteRepository {

    private final DatabaseClient databaseClient;
    private final QuoteEntityMapper quoteEntityMapper;
    private final IQuoteR2dbcRepository quoteR2dbcRepository;
    private static final Logger logger = LoggerFactory.getLogger(QuoteRepository.class);

    public QuoteRepository(IQuoteR2dbcRepository quoteR2dbcRepository,
                           QuoteEntityMapper quoteEntityMapper,
                           DatabaseClient databaseClient) {
        this.quoteR2dbcRepository = quoteR2dbcRepository;
        this.quoteEntityMapper = quoteEntityMapper;
        this.databaseClient = databaseClient;
    }


    @Transactional
    @Override
    public Mono<Void> bulkInsertQuotesIgnoreConflicts(List<Quote> quotes) {
        if (quotes.isEmpty()) {
            return Mono.empty();
        }

        List<QuoteEntity> quoteEntities =  quotes.stream()
                .map(quoteEntityMapper::toQuoteEntity)
                .toList();
        if (quoteEntities.isEmpty()) return Mono.empty();

        StringBuilder sql = new StringBuilder("INSERT INTO quotes (author, text, likes, text_author_hash) VALUES ");
        Map<String, Object> bindings = new HashMap<>();

        for (int i = 0; i < quoteEntities.size(); i++) {
            QuoteEntity q = quoteEntities.get(i);

            if (i > 0) sql.append(", ");

            sql.append("(:author").append(i)
                    .append(", :text").append(i)
                    .append(", :likes").append(i)
                    .append(", :hash").append(i).append(")");

            // bindings/ prepared statements against sqli
            bindings.put("author" + i, q.getAuthor());
            bindings.put("text" + i, q.getText());
            bindings.put("likes" + i, q.getLikes());
            bindings.put("hash" + i, q.getTextAuthorHash());
        }

        sql.append(" ON CONFLICT (text_author_hash) DO NOTHING");

        DatabaseClient.GenericExecuteSpec spec = databaseClient.sql(sql.toString());
        for (Map.Entry<String, Object> entry : bindings.entrySet()) {
            spec = spec.bind(entry.getKey(), entry.getValue());
        }

        return spec.then()
                .onErrorMap(ex -> {
                    String finalSql = sql.toString();

                    logger.error("Error during bulk quote insert. Number of quotes: {}, SQL (snippet): [{}]",
                            quoteEntities.size(),
                            finalSql.substring(0, Math.min(finalSql.length(), 100)), // Log a snippet
                            ex);

                    return new QuotePersistenceException("Error during bulk quote insert. " + ex);
                }); // error will propagate up as intended
    }


    @Override
    public Mono<Optional<Quote>> findByTextAuthorHash(String textAuthorHash) {
        return quoteR2dbcRepository.findByTextAuthorHash(textAuthorHash)
                .onErrorMap(ex -> {
                    logger.error("Error getting quote by textAuthorHash: {}", ex.getMessage(), ex);
                    return new QuotePersistenceException("Error getting quote by textAuthorHash: " + ex);
                })
                .map(quoteEntityMapper::toQuote)
                .map(Optional::of)
                .defaultIfEmpty(Optional.empty());
    }


    @Override
    public Mono<Optional<Quote>> findById(Long id) {
        return quoteR2dbcRepository.findById(id)
                .map(quoteEntityMapper::toQuote)
                .map(Optional::of)
                .defaultIfEmpty(Optional.empty());
    }


    @Override
    public Mono<Quote> saveQuote(Quote domainQuote) {
        QuoteEntity entityToSave = quoteEntityMapper.toQuoteEntity(domainQuote);

        return quoteR2dbcRepository.save(entityToSave)
                .onErrorMap(ex -> {
                    // Can throw DuplicateKeyException, due to unique constraint on text_author_hash
                    logger.error("Error creating a quote: {}", ex.getMessage(), ex);

                    return new QuotePersistenceException("Error saving quote: " + ex);
                })
                .map(quoteEntityMapper::toQuote);
    }


    @Override
    public Mono<Boolean> incrementLikeCount(Long quoteId) {
        if (quoteId == null) {
            return Mono.error(new IllegalArgumentException("Quote ID cannot be null for incrementing likes."));
        }

        String incrementSql = "UPDATE quotes SET likes = likes + 1 WHERE id = :quoteId";
        return executeCounterUpdate(incrementSql, quoteId, "increment");
    }


    @Override
    public Mono<Boolean> decrementLikeCount(Long quoteId) {
        if (quoteId == null) {
            return Mono.error(new IllegalArgumentException("Quote ID cannot be null for decrementing likes."));
        }

        String decrementSql = "UPDATE quotes SET likes = likes - 1 WHERE id = :quoteId AND likes > 0";
        return executeCounterUpdate(decrementSql, quoteId, "decrement");
    }


    private Mono<Boolean> executeCounterUpdate(String sql, Long quoteId, String operationDescription) {
        return this.databaseClient.sql(sql)
                .bind("quoteId", quoteId)
                .fetch()
                .rowsUpdated()
                .map(updatedCount -> updatedCount > 0)
                .onErrorMap(ex -> {
                    logger.error("Database error performing {} likes for quoteId {}: {}", operationDescription, quoteId, ex.getMessage(), ex);
                    return new QuotePersistenceException(String.format("Failed to %s like count for quote %d: ", operationDescription, quoteId) + ex);
                });
    }

}