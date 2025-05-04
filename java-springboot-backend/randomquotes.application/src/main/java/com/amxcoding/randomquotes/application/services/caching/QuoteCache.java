package com.amxcoding.randomquotes.application.services.caching;

import com.amxcoding.randomquotes.application.common.Constants;
import com.amxcoding.randomquotes.application.exceptions.services.QuoteCacheException;
import com.amxcoding.randomquotes.application.interfaces.services.IQuoteFetchOrchestrator;
import com.amxcoding.randomquotes.application.interfaces.services.IQuoteCache;
import com.amxcoding.randomquotes.domain.entities.Quote;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class QuoteCache implements IQuoteCache {

    private static final Logger logger = LoggerFactory.getLogger(QuoteCache.class);
    private final IQuoteFetchOrchestrator quoteFetchOrchestrator;
    private final Cache quotesCacheInstance;


    public QuoteCache(IQuoteFetchOrchestrator quoteFetchOrchestrator,
                      CacheManager cacheManager) {
        this.quoteFetchOrchestrator = quoteFetchOrchestrator;
        this.quotesCacheInstance = Objects.requireNonNull(
                cacheManager.getCache(Constants.Cache.QUOTES_CACHE),
                "Required cache not configured: " + Constants.Cache.QUOTES_CACHE
        );

        logger.info("QuotesCache initialized with cache '{}' ({})",
                this.quotesCacheInstance.getName(), this.quotesCacheInstance.getNativeCache().getClass().getName());
    }

    /**
     * Gets quotes, checking the cache first.
     * On cache miss, delegates fetching to the QuoteFetchOrchestrator,
     * caches the result (unless it's an Optional containing an empty list),
     * and returns it.
     */
    @Override
    @SuppressWarnings("unchecked")
    public Mono<Optional<List<Quote>>> getQuotes() {
        final String cacheKey = Constants.Cache.RANDOM_QUOTES_KEY;

        return Mono.fromCallable(() -> Optional.ofNullable(quotesCacheInstance.get(cacheKey)))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(optionalWrapper -> {
                    if (optionalWrapper.isPresent()) {
                        Cache.ValueWrapper wrapper = optionalWrapper.get();
                        Object cachedValue = wrapper.get();

                        // Try get cached values
                        if (cachedValue instanceof Optional) {
                            try {
                                Optional<List<Quote>> typedResult = (Optional<List<Quote>>) cachedValue;
                                logger.debug("Cache hit for key '{}'", cacheKey);
                                return Mono.just(typedResult);
                            } catch (ClassCastException e) {
                                logger.error("Cache hit for key '{}' but value has unexpected type: {}",
                                        cacheKey, cachedValue.getClass().getName(), e);
                                return Mono.error(new QuoteCacheException("Error getting from cache: " + e.getMessage(), e));
                            }
                        } else {
                            logger.error("Cache hit for key '{}' but value is not an Optional: {}",
                                    cacheKey, cachedValue != null ? cachedValue.getClass().getName() : "null");
                            quotesCacheInstance.evict(cacheKey);

                            return Mono.error(new QuoteCacheException("Error getting from cache!"));
                        }
                    } else {
                        return Mono.<Optional<List<Quote>>>empty();
                    }
                })
                // fetch from orchestrator
                .switchIfEmpty(Mono.defer(() -> {
                    return quoteFetchOrchestrator.getQuotes()
                            .flatMap(resultFromOrchestrator -> {
                                boolean shouldCache = !resultFromOrchestrator
                                        .map(List::isEmpty)
                                        .orElse(false);

                                if (shouldCache) {
                                    try {
                                        return Mono.fromRunnable(() -> quotesCacheInstance.put(cacheKey, resultFromOrchestrator))
                                                .subscribeOn(Schedulers.boundedElastic())
                                                .thenReturn(resultFromOrchestrator);
                                    } catch (Exception e) {
                                        logger.error("Failed to put result into cache for key '{}': {}", cacheKey, e.getMessage(), e);
                                        return Mono.error(new QuoteCacheException("Error putting result into cache: " + e.getMessage(), e));
                                    }
                                } else {
                                    // No quotes throw
                                    logger.error("Failed getting quotes from orchestrator '{}'", cacheKey);
                                    return Mono.error(new QuoteCacheException("Failed getting quotes from orchestrator"));
                                }
                            });
                }));
        // Let errors propagate as is
    }
}