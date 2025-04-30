package com.amxcoding.randomquotes.application.services;

import com.amxcoding.randomquotes.domain.entities.Quote;
import com.amxcoding.randomquotes.application.interfaces.providers.IQuoteProvider;
import com.amxcoding.randomquotes.application.interfaces.services.IQuoteService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
public class QuoteService implements IQuoteService {
    private final IQuoteProvider quoteProvider;

    public QuoteService(@Qualifier("zenquotes") IQuoteProvider quoteProvider) {
        this.quoteProvider = quoteProvider;
    }

    @Override
    public CompletableFuture<Optional<Quote>> fetchRandomQuote() {
        return quoteProvider.fetchRandomQuote();
    }
}
