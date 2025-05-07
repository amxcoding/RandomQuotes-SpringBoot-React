package com.amxcoding.randomquotes.infrastructure.repositories;

import com.amxcoding.randomquotes.application.exceptions.repositories.QuotePersistenceException;
import com.amxcoding.randomquotes.application.interfaces.repositories.IQuoteRepository;
import com.amxcoding.randomquotes.domain.entities.Quote;
import com.amxcoding.randomquotes.infrastructure.persistence.mappers.QuoteEntityMapper;
import com.amxcoding.randomquotes.infrastructure.persistence.models.QuoteEntity;
import com.amxcoding.randomquotes.infrastructure.persistence.r2dbcs.IQuoteR2dbcRepository;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.function.BiFunction;

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

    // package private
    static final BiFunction<Row, RowMetadata, QuoteEntity> QUOTE_ENTITY_MAPPING = (row, rowMetadata) -> {
        QuoteEntity entity = new QuoteEntity();
        entity.setId(row.get("id", Long.class));
        entity.setAuthor(row.get("author", String.class));
        entity.setText(row.get("text", String.class));
        entity.setLikes(Optional.ofNullable(row.get("likes", Integer.class)).orElse(0));
        entity.setTextAuthorHash(row.get("text_author_hash", String.class));
        entity.setProvider(row.get("provider", String.class));
        return entity;
    };
    /**
     * Inserts a batch of quotes into the database, skipping any duplicates based on the text_author_hash.
     */
    @Transactional
    @Override
    public Mono<Void> bulkInsertQuotesIgnoreConflicts(List<Quote> quotes, String providerName) {
        if (quotes.isEmpty()) {
            return Mono.empty();
        }

        List<QuoteEntity> quoteEntities =  quotes.stream()
                .map(quoteEntityMapper::toQuoteEntity)
                .toList();
        if (quoteEntities.isEmpty()) return Mono.empty();

        StringBuilder sql = new StringBuilder("INSERT INTO quotes (author, text, likes, text_author_hash, provider) VALUES ");
        Map<String, Object> bindings = new HashMap<>();

        for (int i = 0; i < quoteEntities.size(); i++) {
            QuoteEntity q = quoteEntities.get(i);

            if (i > 0) sql.append(", ");

            sql.append("(:author").append(i)
                    .append(", :text").append(i)
                    .append(", :likes").append(i)
                    .append(", :hash").append(i)
                    .append(", :provider").append(i)
                    .append(")");

            // bindings/ prepared statements against sqli
            bindings.put("author" + i, q.getAuthor());
            bindings.put("text" + i, q.getText());
            bindings.put("likes" + i, q.getLikes());
            bindings.put("hash" + i, q.getTextAuthorHash());
            bindings.put("provider" + i, providerName);
        }

        sql.append(" ON CONFLICT (text_author_hash, provider) DO NOTHING");
        DatabaseClient.GenericExecuteSpec spec = databaseClient.sql(sql.toString());

        for (Map.Entry<String, Object> entry : bindings.entrySet()) {
            spec = spec.bind(entry.getKey(), entry.getValue() != null ? entry.getValue() : "");
        }

        return spec.then()
                .onErrorMap(ex -> {
                    String finalSql = sql.toString();
                    logger.error("Error during bulk quote insert from provider {}. Number of quotes: {}, SQL (snippet): [{}]",
                            providerName,
                            quoteEntities.size(),
                            finalSql.substring(0, Math.min(finalSql.length(), 100)),
                            ex);

                    return new QuotePersistenceException("Error during bulk quote insert from provider " + providerName + ". " + ex.getMessage(), ex);
                });// error will propagate up as intended
    }

    /**
     * Finds a quote by its unique hash derived from the text and author.
     */
    @Override
    public Mono<Optional<Quote>> findByTextAuthorHash(String textAuthorHash) {
        return quoteR2dbcRepository.findByTextAuthorHash(textAuthorHash)
                .onErrorMap(ex -> {
                    logger.error("Error getting quote by textAuthorHash: {}", ex.getMessage(), ex);
                    return new QuotePersistenceException("Error getting quote by textAuthorHash: " + ex.getMessage(), ex);
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
                .defaultIfEmpty(Optional.empty())
                .onErrorMap(ex -> !(ex instanceof QuotePersistenceException), ex -> { // Avoid double-wrapping
                    logger.error("Error finding quote by id {}: {}", id, ex.getMessage(), ex);
                    return new QuotePersistenceException("Error finding quote by id: " + ex.getMessage(), ex);
                });
    }


    /**
     * Increases the like count for the specified quote by one using atomic operations.
     * Update directly in the db, to avoid race conditions
     */
    @Override
    public Mono<Boolean> incrementLikeCount(Long quoteId) {
        if (quoteId == null) {
            return Mono.error(new IllegalArgumentException("Quote ID cannot be null for incrementing likes."));
        }

        String incrementSql = "UPDATE quotes SET likes = likes + 1 WHERE id = :quoteId";
        return executeCounterUpdate(incrementSql, quoteId, "increment");
    }

    /**
     * Decrease the like count for the specified quote by one using atomic operations.
     * Update directly in the db, to avoid race conditions
     */
    @Override
    public Mono<Boolean> decrementLikeCount(Long quoteId) {
        if (quoteId == null) {
            return Mono.error(new IllegalArgumentException("Quote ID cannot be null for decrementing likes."));
        }

        String decrementSql = "UPDATE quotes SET likes = likes - 1 WHERE id = :quoteId AND likes > 0";
        return executeCounterUpdate(decrementSql, quoteId, "decrement");
    }


    @Override
    public Mono<Long> count() {
        return quoteR2dbcRepository.count()
                .onErrorMap(ex -> {
                    logger.error("Error counting quotes in repository: {}", ex.getMessage(), ex);
                    return new QuotePersistenceException("Database error counting quotes", ex);
                });
    }

    @Override
    public Mono<Long> countByProvider(String providerName) {
        return quoteR2dbcRepository.countByProvider(providerName)
                .onErrorMap(ex -> {
                    logger.error("Error counting quotes for provider {}: {}", providerName, ex.getMessage(), ex);
                    return new QuotePersistenceException("Database error counting quotes for provider " + providerName, ex);
                })
                .switchIfEmpty(Mono.just(0L));
    }

    @Override
    public Mono<List<Quote>> findRandomQuotes(int amount) {
        if (amount <= 0) {
            return Mono.error(new IllegalArgumentException("Amount cannot be 0"));
        }

        String randomQuoteSql = "SELECT id, author, text, likes, text_author_hash, provider FROM quotes ORDER BY RANDOM() LIMIT :amount";

        return this.databaseClient.sql(randomQuoteSql)
                .bind("amount", amount)
                .map(QUOTE_ENTITY_MAPPING)
                .all()
                .map(quoteEntityMapper::toQuote)
                .collectList()
                .onErrorMap(ex -> {
                    logger.error("Error fetching {} random quotes: {}", amount, ex.getMessage(), ex);
                    return new QuotePersistenceException(String.format("Database error fetching %d random quotes", amount), ex);
                });
    }

    @Override
    public Mono<List<Quote>> findRandomQuotesByProvider(int limit, String provider) {
        if (limit <= 0) {
            return Mono.error(new IllegalArgumentException("Limit cannot be 0"));
        }

        String sql = "SELECT id, author, text, likes, text_author_hash, provider FROM quotes WHERE provider = :provider ORDER BY RANDOM() LIMIT :limit";

        return this.databaseClient.sql(sql)
                .bind("limit", limit)
                .bind("provider", provider)
                .map(QUOTE_ENTITY_MAPPING)
                .all()
                .map(quoteEntityMapper::toQuote)
                .collectList()
                .onErrorMap(ex -> {
                    logger.error("Error fetching all quotes: {}", ex.getMessage(), ex);
                    return new QuotePersistenceException("Database error fetching all quotes", ex);
                });
    }


    private Mono<Boolean> executeCounterUpdate(String sql, Long quoteId, String operationDescription) {
        return this.databaseClient.sql(sql)
                .bind("quoteId", quoteId)
                .fetch()
                .rowsUpdated()
                .map(updatedCount -> updatedCount > 0)
                .onErrorMap(ex -> {
                    logger.error("Database error performing {} likes for quoteId {}: {}", operationDescription, quoteId, ex.getMessage(), ex);
                    return new QuotePersistenceException(String.format("Failed to %s like count for quote %d: ", operationDescription, quoteId) + ex.getMessage(), ex);
                });
    }
}