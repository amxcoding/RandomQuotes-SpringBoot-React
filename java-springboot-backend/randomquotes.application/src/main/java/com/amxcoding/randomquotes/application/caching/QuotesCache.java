package com.amxcoding.randomquotes.application.caching;

import com.amxcoding.randomquotes.application.common.Constants;
import com.amxcoding.randomquotes.application.interfaces.caching.IQuotesCache;
import com.amxcoding.randomquotes.application.interfaces.providers.IQuoteProvider;
import com.amxcoding.randomquotes.application.interfaces.repositories.IQuoteRepository;
import com.amxcoding.randomquotes.domain.entities.Quote;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

@Component
public class QuotesCache implements IQuotesCache {

    private final int QUOTE_FETCH_THRESHOLD =  3237;
    private final IQuoteProvider quoteProvider;
    private final IQuoteRepository quoteRepository;
    private final Cache quotesCacheInstance;
    private static final Logger logger = LoggerFactory.getLogger(QuotesCache.class);

    public QuotesCache(@Qualifier("zenquotes") IQuoteProvider quoteProvider,
                       IQuoteRepository quoteRepository,
                       CacheManager cacheManager) {
        this.quoteProvider = quoteProvider;
        this.quoteRepository = quoteRepository;
        this.quotesCacheInstance = cacheManager.getCache(Constants.Cache.QUOTES_CACHE);
        if (this.quotesCacheInstance == null) {
            logger.error("Cache '{}' not found!", Constants.Cache.QUOTES_CACHE);
            throw new IllegalStateException("Required cache not configured: " + Constants.Cache.QUOTES_CACHE);
        }
    }

    /**
     * Gets quotes, first checking the cache. On miss, checks local DB count.
     * If DB count >= threshold, fetches random quotes LOCALLY and caches/returns them.
     * If DB count < threshold, fetches from the external provider, persists new ones,
     * caches/returns the fetched quotes.
     *
     * @return
     */
    @Override
    @SuppressWarnings("unchecked")
    public Mono<Optional<List<Quote>>> getQuotes() {
        final String cacheKey = Constants.Cache.RANDOM_QUOTES_KEY;

        return Mono.fromCallable(() -> Optional.ofNullable(quotesCacheInstance.get(cacheKey, Optional.class)))
                .flatMap(Mono::justOrEmpty)
                .map(rawOptional -> (Optional<List<Quote>>) rawOptional)
                .doOnNext(cachedResult -> logger.debug("Cache hit for key '{}'", cacheKey))
                .switchIfEmpty(Mono.defer(() -> quoteRepository.count()
                                .flatMap(currentCount -> {
                                    // get from database
                                    if (currentCount >= QUOTE_FETCH_THRESHOLD) {
                                        logger.info("Quote threshold ({}) reached ({} found). Fetching random quotes from local DB.", QUOTE_FETCH_THRESHOLD, currentCount);
                                        return quoteRepository.findRandomQuotes(50);
                                    } else {
                                        // get from url
                                        logger.info("Quote threshold ({}) not reached ({} found). Fetching from provider.", QUOTE_FETCH_THRESHOLD, currentCount);
                                        return quoteProvider.fetchQuotes()
                                                .flatMap(optionalQuotes -> {
                                                    if (optionalQuotes.isPresent() && !optionalQuotes.get().isEmpty()) {
                                                        List<Quote> fetchedQuotes = optionalQuotes.get();

                                                        return quoteRepository.bulkInsertQuotesIgnoreConflicts(fetchedQuotes)
                                                                .thenReturn(optionalQuotes);
                                                    } else {
                                                        logger.debug("Provider returned no quotes or an empty Optional.");
                                                        return Mono.just(optionalQuotes);
                                                    }
                                                });
                                    }
                                })
                                // Cache the result
                                .doOnNext(resultToCache -> {
                                    // Check if the Optional is present AND the List inside it is not empty
                                    if (resultToCache.isPresent() && !resultToCache.get().isEmpty()) {
                                        quotesCacheInstance.put(cacheKey, resultToCache);
                                    } else {
                                        logger.debug("Result is empty (Optional.empty or contains empty list). Skipping cache for key '{}'.", cacheKey);
                                    }
                                }))
                )
                .doOnError(error -> logger.error("Error occurred in the getQuotes reactive chain: ", error));
    }
}
