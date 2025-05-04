package com.amxcoding.randomquotes.infrastructure.providers;

import com.amxcoding.randomquotes.application.exceptions.providers.QuoteProviderException;
import com.amxcoding.randomquotes.application.interfaces.providers.IQuoteProvider;
import com.amxcoding.randomquotes.domain.entities.Quote;
import com.amxcoding.randomquotes.infrastructure.common.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("LoggingPlaceholderCountMatchesArgumentCount")
@Component
@Qualifier(Constants.QuoteProviders.ZEN_QUOTES)
public class ZenQuotesProvider implements IQuoteProvider {

    private static final Logger logger = LoggerFactory.getLogger(ZenQuotesProvider.class);
    private final WebClient webClient;

    public ZenQuotesProvider(WebClient.Builder webClientBuilder, @Value("${quoteprovider.zenquotes}") String baseUrl) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
    }

    /**
     * Fetch 50 quotes by default from zenquotes.
     * On error throw custom error which will be handled by the global error handler
     */
    @Override
    public Mono<Optional<List<Quote>>> fetchQuotes() {
        return webClient
                .get()
                .uri("/quotes")
                .retrieve()
                .bodyToMono(Quote[].class)
                .map(quotes -> {
                    if (quotes != null && quotes.length > 0) {
                        return Optional.of(Arrays.asList(quotes));
                    } else {
                        return Optional.<List<Quote>>empty();

                    }
                })
                .doOnError(ex -> {
                    if (ex instanceof WebClientResponseException webEx) {
                        logger.error("ZenQuotesProvider-fetchQuotes", "HTTP Error (status {})  {} - {}", String.valueOf(webEx.getStatusCode()), webEx.getResponseBodyAsString(), ex);
                    } else {
                        logger.error("ZenQuotesProvider-fetchQuotes","Other errors on getting the quote {}: {}", ex.getMessage(), ex);
                    }
                })
                .onErrorMap(ex -> new QuoteProviderException("Error fetching all quotes: " + ex.getMessage(), ex))
                .defaultIfEmpty(Optional.empty());
    }
}
