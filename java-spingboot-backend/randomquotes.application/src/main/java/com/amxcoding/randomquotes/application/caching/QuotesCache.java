package com.amxcoding.randomquotes.application.caching;

import com.amxcoding.randomquotes.application.common.Constants;
import com.amxcoding.randomquotes.application.interfaces.caching.IQuotesCache;
import com.amxcoding.randomquotes.application.interfaces.providers.IQuoteProvider;
import com.amxcoding.randomquotes.domain.entities.Quote;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

@Component
public class QuotesCache implements IQuotesCache {

    private final IQuoteProvider quoteProvider;

    public QuotesCache(@Qualifier("zenquotes") IQuoteProvider quoteProvider) {
        this.quoteProvider = quoteProvider;
    }

    @Override
    @Cacheable(value = Constants.Cache.QUOTES_CACHE, key = "'" + Constants.Cache.RANDOM_QUOTES_KEY + "'")
    public Mono<Optional<List<Quote>>> getQuotes() {
        return quoteProvider.fetchQuotes();
    }
}
