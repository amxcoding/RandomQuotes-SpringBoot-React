package com.amxcoding.randomquotes.application.services;

import com.amxcoding.randomquotes.application.exceptions.providers.QuoteProviderException;
import com.amxcoding.randomquotes.application.exceptions.repositories.QuotePersistenceException;
import com.amxcoding.randomquotes.application.exceptions.services.QuoteFetchOrchestratorException;
import com.amxcoding.randomquotes.application.interfaces.providers.IQuoteProvider;
import com.amxcoding.randomquotes.application.interfaces.repositories.IQuoteRepository;
import com.amxcoding.randomquotes.application.interfaces.services.IQuoteFetchOrchestrator;
import com.amxcoding.randomquotes.domain.entities.Quote;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

/**
 * Goal of this class is the manage multiple providers and fallback on the database
 */
@Service
public class QuoteFetchOrchestrator implements IQuoteFetchOrchestrator {

    private final List<IQuoteProvider> quoteProviders;
    private final IQuoteRepository quoteRepository;
    private static final Logger logger = LoggerFactory.getLogger(QuoteFetchOrchestrator.class);

    private static final int FALLBACK_RANDOM_QUOTE_AMOUNT = 50;

    public QuoteFetchOrchestrator(List<IQuoteProvider> quoteProviders,
                                  IQuoteRepository quoteRepository) {
        // Sort providers based on @Order annotation if present
        quoteProviders.sort(AnnotationAwareOrderComparator.INSTANCE);
        this.quoteProviders = quoteProviders;
        this.quoteRepository = quoteRepository;
    }

    /**
     * Orchestrates fetching quotes by trying available providers sequentially.
     * If a provider returns quotes, those are returned.
     * If a provider fails or returns empty, the next provider is tried.
     * If all providers fail or return empty, falls back to fetching random quotes
     * from the local database repository.
     *
     */
    @Override
    public Mono<List<Quote>> getQuotes() {
        if (quoteProviders.isEmpty()) {
            return fetchRandomFromDb();
        }

        // Chain providers sequentially, taking the first non-empty result
        return Flux.fromIterable(quoteProviders)
                .concatMap(provider -> {
                    return provider.fetchQuotes()
                            .onErrorResume(error -> {
                                if (error instanceof QuotePersistenceException) {
                                    // Critical DB persistence error, propagate
                                    logger.error("CRITICAL - Provider {} encountered non-recoverable persistence error: {}",
                                            provider.getProviderName(), error.getMessage(), error);
                                    return Mono.error(error);
                                } else if (error instanceof QuoteProviderException) {
                                    // Provider fetch error (e.g., network, API error)
                                    logger.warn("Provider {} failed (QuoteProviderException): {}",
                                            provider.getProviderName(), error.getMessage());
                                    // return empty list s.t. we can try next provider
                                    return Mono.empty();
                                } else {
                                    // Propagate error
                                    logger.error("Unexpected error during fetch from provider {}: {}",
                                            provider.getProviderName(), error.getMessage(), error);
                                    return Mono.error(new QuoteFetchOrchestratorException("Unexpected error: " + error.getMessage(), error));
                                }
                            });
                })
                .filter(quotes -> !quotes.isEmpty())
                .next()
                .switchIfEmpty(Mono.defer(this::fetchRandomFromDb))
                .defaultIfEmpty(List.of())
                .onErrorMap(error -> {
                    logger.error("Critical error during quote fetch orchestration: {}", error.getMessage(), error);
                    return new QuoteFetchOrchestratorException("CRITICAL error failed to get quotes: " + error.getMessage(), error);
                });
    }

    /**
     * Fallback method to fetch random quotes from the database.
     * based on how many are available
     */
    private Mono<List<Quote>> fetchRandomFromDb() {
        // Let errors propagate
        return quoteRepository.findRandomQuotes(FALLBACK_RANDOM_QUOTE_AMOUNT);
    }
}