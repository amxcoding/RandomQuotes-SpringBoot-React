package services;

import entities.Quote;
import interfaces.providers.IQuoteProvider;
import interfaces.services.IQuoteService;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class QuoteService implements IQuoteService {
    private final IQuoteProvider quoteProvider;

    public QuoteService(IQuoteProvider quoteProvider) {
        this.quoteProvider = quoteProvider;
    }

    @Override
    public CompletableFuture<Optional<Quote>> fetchRandomQuote() {
        return quoteProvider.fetchRandomQuote();
    }
}
