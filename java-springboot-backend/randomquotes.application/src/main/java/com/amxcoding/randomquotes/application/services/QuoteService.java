package com.amxcoding.randomquotes.application.services;

import com.amxcoding.randomquotes.application.exceptions.repositories.QuotePersistenceException;
import com.amxcoding.randomquotes.application.interfaces.caching.IQuotesCache;
import com.amxcoding.randomquotes.application.interfaces.repositories.IQuoteRepository;
import com.amxcoding.randomquotes.application.interfaces.services.IQuoteService;
import com.amxcoding.randomquotes.domain.entities.Quote;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class QuoteService implements IQuoteService {
    private final IQuotesCache quotesCache;
    private final IQuoteRepository quoteRepository;
    private static final Logger logger = LoggerFactory.getLogger(QuoteService.class);

    public QuoteService(IQuotesCache quotesCache,
                        IQuoteRepository quoteRepository) {
        this.quotesCache = quotesCache;
        this.quoteRepository = quoteRepository;
    }


    @Override
    public Mono<Optional<Quote>> getRandomQuote() {
        return quotesCache.getQuotes()
                .flatMap(optionalQuotesList -> {
                    if (optionalQuotesList.isPresent() && !optionalQuotesList.get().isEmpty()) {
                        List<Quote> cachedQuotes = optionalQuotesList.get();
                        int randomIndex = ThreadLocalRandom.current().nextInt(cachedQuotes.size());
                        Quote cachedRandomQuote = cachedQuotes.get(randomIndex);

                        // we have hit the threshold the result is from the database
                        if (cachedRandomQuote.getId() != null) {
                            return Mono.just(Optional.of(cachedRandomQuote));
                        }

                        // use the hash to get the quote from db
                        // needed to show correct likes if the quote was added already before
                        String quoteHash = cachedRandomQuote.generateTextAuthorHash();

                        if (!StringUtils.hasText(quoteHash)) {
                            logger.warn("Hash  missing. Cannot fetch from DB. Cached quote details: author='{}', text='{}...'",
                                    cachedRandomQuote.getAuthor(),
                                    cachedRandomQuote.getText());
                            throw new IllegalStateException("Quotes hash is missing");
                        }

                        return quoteRepository.findByTextAuthorHash(quoteHash);
                    } else {
                        return Mono.just(Optional.empty());
                    }
                });
    }

    @Override
    public Mono<Optional<Quote>> getQuoteById(Long quoteId) {
        // Error handled by global error handler
        return quoteRepository.findById(quoteId);
    }

    @Override
    public Mono<Quote> updateQuote(Quote quote) {
        if (quote.getId() == null) {
            return Mono.error(new QuotePersistenceException("Quote ID must not be null for update"));
        }

        return quoteRepository.saveQuote(quote);
    }

}
