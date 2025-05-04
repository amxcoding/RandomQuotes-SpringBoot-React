package com.amxcoding.randomquotes.infrastructure.providers;

import com.amxcoding.randomquotes.application.exceptions.providers.QuoteProviderException;
import com.amxcoding.randomquotes.application.interfaces.providers.IQuoteProvider;
import com.amxcoding.randomquotes.application.interfaces.repositories.IQuoteRepository;
import com.amxcoding.randomquotes.domain.entities.Quote;
import com.amxcoding.randomquotes.infrastructure.common.Constants;
import com.amxcoding.randomquotes.infrastructure.mappers.ZenQuoteMapper;
import com.amxcoding.randomquotes.infrastructure.models.ZenQuote;
import io.netty.handler.timeout.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@SuppressWarnings("loggergingPlaceholderCountMatchesArgumentCount")
@Component
@Qualifier(Constants.QuoteProviders.ZEN_QUOTES)
@Order(1)
public class ZenQuotesProvider implements IQuoteProvider {

    private static final Logger logger = LoggerFactory.getLogger(ZenQuotesProvider.class);
    private final WebClient webClient;
    private final ZenQuoteMapper mapper;
    private final IQuoteRepository quoteRepository;

    // Configuration for retries
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final Duration MIN_RETRY_BACKOFF = Duration.ofSeconds(1);
    private static final Duration MAX_RETRY_BACKOFF = Duration.ofSeconds(5);

    // Max available quotes to fetch
    private static final long PROVIDER_QUOTE_THRESHOLD = 3237;

    public ZenQuotesProvider(WebClient.Builder webClientBuilder,
                             @Value("${quoteprovider.zenquotes}") String baseUrl,
                             ZenQuoteMapper mapper,
                             IQuoteRepository quoteRepository) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.mapper = mapper;
        this.quoteRepository = quoteRepository;
    }

    /**
     * Checks local DB count for this provider. If threshold is met, returns empty Optional.
     * Otherwise, fetches 50 quotes by default from zenquotes, persists them, and returns them.
     * Retries on transient errors (5xx, timeouts) with exponential backoff.
     * On final error, throws custom QuoteProviderException.
     */
    @Override
    public Mono<Optional<List<Quote>>> fetchQuotes() {
        // let possible errors propagate
        return quoteRepository.countByProvider(getProviderName())
                .flatMap(currentCount -> {
                    // Get from the database we have already fetched all available quotes
                    if (currentCount >= PROVIDER_QUOTE_THRESHOLD) {
                        return Mono.just(Optional.<List<Quote>>empty());
                    } else {
                        return doFetchAndPersistQuotes();
                    }
                });
    }

    /**
     * Performs the actual WebClient call, retry logic, mapping, and persistence.
     * This is called only if the quote count threshold is not met.
     */
    private Mono<Optional<List<Quote>>> doFetchAndPersistQuotes() {
        return webClient
                .get()
                .uri("/quotes")
                .retrieve()
                .bodyToMono(ZenQuote[].class)
                .flatMap(zenQuotes -> {
                    if (zenQuotes != null && zenQuotes.length > 0) {
                        List<Quote> fetchedQuotes = Arrays.stream(zenQuotes)
                                .map(mapper::toQuote)
                                .collect(Collectors.toList());

                        // propagate errors
                        return quoteRepository.bulkInsertQuotesIgnoreConflicts(fetchedQuotes, getProviderName())
                                .then(Mono.just(Optional.of(fetchedQuotes)));
                    } else {
                        return Mono.just(Optional.<List<Quote>>empty());
                    }
                })
                // Error on fetching
                .doOnError(error -> logger.warn("Error during ZenQuotes fetch: {}", error.getMessage()))
                .retryWhen(buildRetrySpec())
                .onErrorMap(
                        throwable -> !(throwable instanceof QuoteProviderException),
                        error -> {
                            logger.error("Failed to fetch quotes from ZenQuotes after retries, error: {}", error.getMessage(), error);

                            if (error instanceof WebClientResponseException webEx) {
                                return new QuoteProviderException("Error fetching ZenQuotes (status: " + webEx.getStatusCode() + "): " + webEx.getResponseBodyAsString(), webEx);
                            }
                            return new QuoteProviderException("Error fetching ZenQuotes: " + error.getMessage(), error);
                        }
                )
                .defaultIfEmpty(Optional.empty());
    }


    @Override
    public String getProviderName() {
        return Constants.QuoteProviders.ZEN_QUOTES;
    }

    /**
     * Builds the Retry specification for fetching quotes.
     * Retries on 5xx server errors and specific timeout/network exceptions.
     */
    private Retry buildRetrySpec() {
        return Retry.backoff(MAX_RETRY_ATTEMPTS, MIN_RETRY_BACKOFF)
                .maxBackoff(MAX_RETRY_BACKOFF)
                .filter(this::isRetryableError)
                .doBeforeRetry(retrySignal ->
                        logger.warn("Retrying ZenQuotes fetch attempt #{}. Error: {}",
                                retrySignal.totalRetries() + 1,
                                retrySignal.failure().getMessage()))
                .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) ->
                        retrySignal.failure() // Throw the last encountered error
                );
    }

    /**
     * Checks if an error is suitable for retrying.
     */
    private boolean isRetryableError(Throwable throwable) {
        if (throwable instanceof WebClientResponseException responseException) {
            HttpStatusCode statusCode = responseException.getStatusCode();

            return responseException.getStatusCode().is5xxServerError() ||
                    statusCode.value() == HttpStatus.TOO_MANY_REQUESTS.value();
        }

        return throwable instanceof TimeoutException ||
                throwable instanceof WebClientRequestException;
    }
}
