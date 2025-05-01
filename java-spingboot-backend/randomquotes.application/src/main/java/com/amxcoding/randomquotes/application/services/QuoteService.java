package com.amxcoding.randomquotes.application.services;

import com.amxcoding.randomquotes.application.exceptions.repositories.QuotePersistenceException;
import com.amxcoding.randomquotes.application.interfaces.caching.IQuotesCache;
import com.amxcoding.randomquotes.application.interfaces.repositories.IQuoteRepository;
import com.amxcoding.randomquotes.domain.entities.Quote;
import com.amxcoding.randomquotes.application.interfaces.services.IQuoteService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class QuoteService implements IQuoteService {
    private final IQuotesCache quotesCache;
    private final IQuoteRepository quoteRepository;

    public QuoteService(IQuotesCache quotesCache, IQuoteRepository quoteRepository) {
        this.quotesCache = quotesCache;
        this.quoteRepository = quoteRepository;
    }

    @Override
    public Mono<Optional<Quote>> getRandomQuote() {
        // Fetch quotes from cache (assuming this returns Mono<Optional<List<Quote>>>)
        return quotesCache.getQuotes()
                .flatMap(optionalQuotesList -> {
                    // Use reactive map instead of blocking CompletableFuture
                    if (optionalQuotesList.isPresent() && !optionalQuotesList.get().isEmpty()) {
                        List<Quote> quotes = optionalQuotesList.get();
                        int randomIndex = ThreadLocalRandom.current().nextInt(quotes.size());
                        Quote randomQuote = quotes.get(randomIndex);

                        return Mono.just(Optional.of(randomQuote));
                    } else {
                        return Mono.just(Optional.empty());
                    }
                });
    }

    // Return the quote so we can track the likes correctly
    @Override
    public Mono<Quote> createQuote(Quote quote) {
        return quoteRepository.saveQuote(quote);
    }

    @Override
    public Mono<Optional<Quote>> getQuoteById(Long id) {
        return quoteRepository.findById(id);
    }

    @Override
    public Mono<Quote> updateQuote(Quote quote) {
        if (quote.getId() == null) {
            return Mono.error(new QuotePersistenceException("Quote ID must not be null for update"));
        }

        return quoteRepository.saveQuote(quote);
    }


}
