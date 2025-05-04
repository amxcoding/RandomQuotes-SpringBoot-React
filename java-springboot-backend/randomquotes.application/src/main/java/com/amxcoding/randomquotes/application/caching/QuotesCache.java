package com.amxcoding.randomquotes.application.caching;

import com.amxcoding.randomquotes.application.common.Constants;
import com.amxcoding.randomquotes.application.interfaces.caching.IQuotesCache;
import com.amxcoding.randomquotes.application.interfaces.providers.IQuoteProvider;
import com.amxcoding.randomquotes.application.interfaces.repositories.IQuoteRepository;
import com.amxcoding.randomquotes.domain.entities.Quote;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

@Component
public class QuotesCache implements IQuotesCache {

    private final IQuoteProvider quoteProvider;
    private final IQuoteRepository quoteRepository;
    private static final Logger logger = LoggerFactory.getLogger(QuotesCache.class);

    public QuotesCache(@Qualifier("zenquotes") IQuoteProvider quoteProvider,
                       IQuoteRepository quoteRepository) {
        this.quoteProvider = quoteProvider;
        this.quoteRepository = quoteRepository;
    }


    /**
     * Fetches quotes, attempts to persist them ignoring conflicts,
     * and returns the fetched quotes. The result is cached.
     * Persistence only happens on cache miss when quotes are actually fetched.
     *
     * @return Mono emitting an Optional containing the list of fetched quotes, or empty Optional.
     */
    @Override
    @Cacheable(value = Constants.Cache.QUOTES_CACHE, key = "'" + Constants.Cache.RANDOM_QUOTES_KEY + "'")
    public Mono<Optional<List<Quote>>> getQuotes() {
        return quoteProvider.fetchQuotes()
                .flatMap(optionalQuotes -> {
                    // Check if the Optional contains a non-empty list of quotes
                    if (optionalQuotes.isPresent() && !optionalQuotes.get().isEmpty()) {
                        List<Quote> fetchedQuotes = optionalQuotes.get();

                        // Perform the bulk insert. After it completes (.then), return the original fetched quotes.
                        return quoteRepository.bulkInsertQuotesIgnoreConflicts(fetchedQuotes)
                                .doOnError(err -> logger.error("Error during bulk insert attempt after fetching quotes.", err))
                                .then(Mono.just(optionalQuotes));

                    } else {
                        // If fetch returned empty Optional or empty List, do nothing and return it.
                        return Mono.just(optionalQuotes);
                    }
                })
                .doOnError(error -> logger.error("Error occurred in the getQuotes reactive chain: ", error));
    }
}
