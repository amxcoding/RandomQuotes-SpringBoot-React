package com.amxcoding.randomquotes.application.services;

import com.amxcoding.randomquotes.application.interfaces.services.IQuoteCache;
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
    private final IQuoteCache quotesCache;
    private final IQuoteRepository quoteRepository;
    private static final Logger logger = LoggerFactory.getLogger(QuoteService.class);

    public QuoteService(IQuoteCache quotesCache,
                        IQuoteRepository quoteRepository) {
        this.quotesCache = quotesCache;
        this.quoteRepository = quoteRepository;
    }


    /**
     * Get random quote from a provider or the database
     * If Id is null it was from a provider else from the database
     */
    @Override
    public Mono<Optional<Quote>> getRandomQuote() {
        return quotesCache.getQuotes()
                .flatMap(cachedQuotes -> {
                    if (!cachedQuotes.isEmpty()) {
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

}
