package com.amxcoding.randomquotes.infrastructure.providers;

import com.amxcoding.randomquotes.application.exceptions.providers.QuoteProviderException;
import com.amxcoding.randomquotes.application.exceptions.repositories.QuotePersistenceException;
import com.amxcoding.randomquotes.application.interfaces.repositories.IQuoteRepository;
import com.amxcoding.randomquotes.domain.entities.Quote;
import com.amxcoding.randomquotes.infrastructure.common.Constants;
import com.amxcoding.randomquotes.infrastructure.mappers.ZenQuoteMapper;
import com.amxcoding.randomquotes.infrastructure.models.ZenQuote;
import io.netty.handler.timeout.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.verification.VerificationMode;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ZenQuotesProviderTest {

    @Mock
    private WebClient.Builder webClientBuilder;
    @Mock
    private WebClient webClient;
    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;
    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;
    @Mock
    private WebClient.ResponseSpec responseSpec;
    @Mock
    private ZenQuoteMapper mapper;
    @Mock
    private IQuoteRepository quoteRepository;

    private ZenQuotesProvider zenQuotesProvider;

    private ZenQuote zenQuote1;
    private ZenQuote zenQuote2;
    private Quote domainQuote1;
    private Quote domainQuote2;

    private final String BASE_URL = "http://zenquotes.test";
    private final String PROVIDER_NAME = Constants.QuoteProviders.ZEN_QUOTES;
    private final long PROVIDER_THRESHOLD = 3237; // Match the constant in the class


    @BeforeEach
    void setUp() {
        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);

        zenQuotesProvider = new ZenQuotesProvider(
                webClientBuilder,
                BASE_URL,
                mapper,
                quoteRepository
        );

        zenQuote1 = new ZenQuote("Author Zen 1", "Quote Zen 1");
        zenQuote2 = new ZenQuote("Author Zen 2", "Quote Zen 2");
        domainQuote1 = new Quote("Author Zen 1", "Quote Zen 1");
        domainQuote2 = new Quote("Author Zen 2", "Quote Zen 2");
    }

    @Nested
    @DisplayName("fetchQuotes() Tests")
    class FetchQuotesTests {

        @Test
        @DisplayName("1. Should fetch random quotes from DB when quote count threshold is met")
        void fetchQuotes_whenThresholdMet_shouldFetchRandomQuotesFromDB() {
            // Arrange
            when(quoteRepository.countByProvider(PROVIDER_NAME)).thenReturn(Mono.just(PROVIDER_THRESHOLD));
            List<Quote> randomQuotesFromDB = List.of(domainQuote1);
            // Use the actual constant value from ZenQuotesProvider.RANDOM_QUOTE_AMOUNT if accessible (package-private)
            // Otherwise, use the literal value 50 directly.
            when(quoteRepository.findRandomQuotesByProvider(eq(ZenQuotesProvider.RANDOM_QUOTE_AMOUNT), eq(PROVIDER_NAME)))
                    .thenReturn(Mono.just(randomQuotesFromDB));

            // Act
            Mono<List<Quote>> resultMono = zenQuotesProvider.fetchQuotes();

            // Assert
            StepVerifier.create(resultMono)
                    .expectNext(randomQuotesFromDB)
                    .verifyComplete();

            verify(quoteRepository).countByProvider(PROVIDER_NAME);
            verify(quoteRepository).findRandomQuotesByProvider(ZenQuotesProvider.RANDOM_QUOTE_AMOUNT, PROVIDER_NAME);
            verifyNoInteractions(webClient, mapper);
            verify(quoteRepository, never()).bulkInsertQuotesIgnoreConflicts(anyList(), anyString());
        }

        @Test
        @DisplayName("2. Should fetch, map, persist, and return quotes when threshold not met")
        void fetchQuotes_whenThresholdNotMetAndFetchSuccess_shouldFetchMapPersistAndReturn() {
            // Arrange
            when(quoteRepository.countByProvider(PROVIDER_NAME)).thenReturn(Mono.just(PROVIDER_THRESHOLD - 10));
            arrangeWebClientChainUpToRetrieve();
            ZenQuote[] apiResponse = {zenQuote1, zenQuote2};
            when(responseSpec.bodyToMono(eq(ZenQuote[].class))).thenReturn(Mono.just(apiResponse));
            when(mapper.toQuote(zenQuote1)).thenReturn(domainQuote1);
            when(mapper.toQuote(zenQuote2)).thenReturn(domainQuote2);
            when(quoteRepository.bulkInsertQuotesIgnoreConflicts(anyList(), eq(PROVIDER_NAME)))
                    .thenReturn(Mono.empty()); // Simulate successful void persistence

            // Act
            Mono<List<Quote>> resultMono = zenQuotesProvider.fetchQuotes();

            // Assert
            StepVerifier.create(resultMono)
                    .assertNext(resultList -> assertThat(resultList).containsExactlyInAnyOrder(domainQuote1, domainQuote2))
                    .verifyComplete();

            verify(quoteRepository).countByProvider(PROVIDER_NAME);
            verifyWebClientInteractions();
            verify(mapper).toQuote(zenQuote1);
            verify(mapper).toQuote(zenQuote2);
            ArgumentCaptor<List<Quote>> listCaptor = ArgumentCaptor.forClass(List.class);
            verify(quoteRepository).bulkInsertQuotesIgnoreConflicts(listCaptor.capture(), eq(PROVIDER_NAME));
            assertThat(listCaptor.getValue()).containsExactlyInAnyOrder(domainQuote1, domainQuote2);
        }

        @Test
        @DisplayName("3. Should return empty List when fetch succeeds but returns empty array")
        void fetchQuotes_whenFetchReturnsEmptyArray_shouldReturnEmptyList() {
            // Arrange
            when(quoteRepository.countByProvider(PROVIDER_NAME)).thenReturn(Mono.just(0L));
            arrangeWebClientChainUpToRetrieve();
            ZenQuote[] emptyApiResponse = {};
            when(responseSpec.bodyToMono(eq(ZenQuote[].class))).thenReturn(Mono.just(emptyApiResponse));

            // Act
            Mono<List<Quote>> resultMono = zenQuotesProvider.fetchQuotes();

            // Assert
            StepVerifier.create(resultMono)
                    .expectNext(Collections.emptyList()) // Expect an empty list due to .defaultIfEmpty(List.of())
                    .verifyComplete();

            verify(quoteRepository).countByProvider(PROVIDER_NAME);
            verifyWebClientInteractions();
            verifyNoInteractions(mapper);
            verify(quoteRepository, never()).bulkInsertQuotesIgnoreConflicts(anyList(), anyString());
        }

        @Test
        @DisplayName("4. Should propagate repository error during count check")
        void fetchQuotes_whenCountCheckFails_shouldPropagateError() {
            // Arrange
            RuntimeException dbError = new RuntimeException("Database connection failed");
            when(quoteRepository.countByProvider(PROVIDER_NAME)).thenReturn(Mono.error(dbError));

            // Act
            Mono<List<Quote>> resultMono = zenQuotesProvider.fetchQuotes();

            // Assert
            StepVerifier.create(resultMono)
                    .expectErrorMatches(throwable -> throwable == dbError)
                    .verify();

            verify(quoteRepository).countByProvider(PROVIDER_NAME);
            verifyNoInteractions(webClient, mapper);
            verify(quoteRepository, never()).bulkInsertQuotesIgnoreConflicts(anyList(), anyString());
        }

        @Test
        @DisplayName("5. Should map repository error during bulk insert to QuoteProviderException")
        void fetchQuotes_whenBulkInsertFails_shouldMapToProviderException() {
            // Arrange
            when(quoteRepository.countByProvider(PROVIDER_NAME)).thenReturn(Mono.just(0L));
            arrangeWebClientChainUpToRetrieve();
            ZenQuote[] apiResponse = {zenQuote1};
            when(responseSpec.bodyToMono(eq(ZenQuote[].class))).thenReturn(Mono.just(apiResponse));
            when(mapper.toQuote(zenQuote1)).thenReturn(domainQuote1);
            QuotePersistenceException persistError = new QuotePersistenceException(
                    "DB write failed", new RuntimeException("Simulated DB Error")
            );
            // Ensure the argument to bulkInsert is specific for verification later if needed
            when(quoteRepository.bulkInsertQuotesIgnoreConflicts(eq(List.of(domainQuote1)), eq(PROVIDER_NAME)))
                    .thenReturn(Mono.error(persistError));

            // Act
            Mono<List<Quote>> resultMono = zenQuotesProvider.fetchQuotes();

            // Assert
            StepVerifier.create(resultMono)
                    .expectErrorSatisfies(throwable -> {
                        assertThat(throwable)
                                .isInstanceOf(QuoteProviderException.class)
                                .hasCause(persistError);
                    })
                    .verify();

            verify(quoteRepository).countByProvider(PROVIDER_NAME);
            verifyWebClientInteractions(times(1));
            verify(mapper).toQuote(zenQuote1);
            verify(quoteRepository).bulkInsertQuotesIgnoreConflicts(eq(List.of(domainQuote1)), eq(PROVIDER_NAME));
        }

        @Test
        @DisplayName("6. Should map WebClient 4xx error to QuoteProviderException without retry")
        void fetchQuotes_whenWebClient4xxError_shouldMapToProviderExceptionNoRetry() {
            // Arrange
            when(quoteRepository.countByProvider(PROVIDER_NAME)).thenReturn(Mono.just(0L));
            arrangeWebClientChainUpToRetrieve();
            WebClientResponseException clientError = WebClientResponseException.create(
                    HttpStatus.NOT_FOUND.value(), "Not Found", HttpHeaders.EMPTY, "Not Found Body".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
            when(responseSpec.bodyToMono(eq(ZenQuote[].class))).thenReturn(Mono.error(clientError));

            // Act
            Mono<List<Quote>> resultMono = zenQuotesProvider.fetchQuotes();

            // Assert
            StepVerifier.create(resultMono)
                    .expectErrorSatisfies(throwable -> {
                        assertThat(throwable)
                                .isInstanceOf(QuoteProviderException.class)
                                .hasCause(clientError);
                    })
                    .verify();

            verify(quoteRepository).countByProvider(PROVIDER_NAME);
            verifyWebClientInteractions(times(1));
            verifyNoInteractions(mapper);
            verify(quoteRepository, never()).bulkInsertQuotesIgnoreConflicts(anyList(), anyString());
        }

        @Test
        @DisplayName("7. Should retry on 5xx error then succeed")
        void fetchQuotes_whenWebClient5xxErrorThenSuccess_shouldRetryAndSucceed() {
            // Arrange
            when(quoteRepository.countByProvider(PROVIDER_NAME)).thenReturn(Mono.just(0L));
            arrangeWebClientChainUpToRetrieve();

            WebClientResponseException serverError = WebClientResponseException.create(
                    HttpStatus.SERVICE_UNAVAILABLE.value(), "Service Unavailable", HttpHeaders.EMPTY, "".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
            ZenQuote[] successResponse = {zenQuote1};

            AtomicInteger effectiveCallCount = new AtomicInteger(0);

            when(responseSpec.bodyToMono(eq(ZenQuote[].class)))
                    .thenAnswer(invocation -> {
                        return Mono.defer(() -> {
                            int count = effectiveCallCount.incrementAndGet();
                            if (count == 1) {
                                System.out.println("Mocked bodyToMono (via Mono.defer): Attempt #" + count + " -> Returning ERROR");
                                return Mono.error(serverError);
                            } else {
                                System.out.println("Mocked bodyToMono (via Mono.defer): Attempt #" + count + " -> Returning SUCCESS");
                                return Mono.just(successResponse);
                            }
                        });
                    });

            when(mapper.toQuote(zenQuote1)).thenReturn(domainQuote1);
            when(quoteRepository.bulkInsertQuotesIgnoreConflicts(List.of(domainQuote1), PROVIDER_NAME)).thenReturn(Mono.empty());

            // Act
            Mono<List<Quote>> resultMono = zenQuotesProvider.fetchQuotes();

            // Assert
            StepVerifier.create(resultMono)
                    .assertNext(resultList -> assertThat(resultList).containsExactly(domainQuote1))
                    .expectComplete()
                    .verify(Duration.ofSeconds(10));

            verify(quoteRepository).countByProvider(PROVIDER_NAME);
            verifyWebClientInteractions(times(1));
            verify(mapper).toQuote(zenQuote1);
            verify(quoteRepository).bulkInsertQuotesIgnoreConflicts(List.of(domainQuote1), PROVIDER_NAME);
        }

        @Test
        @DisplayName("8. Should retry on TimeoutException then succeed")
        void fetchQuotes_whenNettyTimeoutErrorThenSuccess_shouldRetryAndSucceed() {
            // Arrange
            when(quoteRepository.countByProvider(PROVIDER_NAME)).thenReturn(Mono.just(0L));
            arrangeWebClientChainUpToRetrieve();
            TimeoutException nettyTimeoutError = mock(TimeoutException.class);
            ZenQuote[] successResponse = {zenQuote1};

            AtomicInteger effectiveCallCount = new AtomicInteger(0);

            when(responseSpec.bodyToMono(eq(ZenQuote[].class)))
                    .thenAnswer(invocation -> {
                        return Mono.defer(() -> {
                            int count = effectiveCallCount.incrementAndGet();
                            if (count == 1) {
                                System.out.println("Mocked bodyToMono (Netty Timeout Test): Attempt #" + count + " -> Returning Netty Timeout ERROR");
                                return Mono.error(nettyTimeoutError);
                            } else {
                                System.out.println("Mocked bodyToMono (Netty Timeout Test): Attempt #" + count + " -> Returning SUCCESS");
                                return Mono.just(successResponse);
                            }
                        });
                    });

            when(mapper.toQuote(zenQuote1)).thenReturn(domainQuote1);
            when(quoteRepository.bulkInsertQuotesIgnoreConflicts(List.of(domainQuote1), PROVIDER_NAME)).thenReturn(Mono.empty());

            // Act
            Mono<List<Quote>> resultMono = zenQuotesProvider.fetchQuotes();

            // Assert
            StepVerifier.create(resultMono)
                    .assertNext(resultList -> assertThat(resultList).containsExactly(domainQuote1))
                    .expectComplete()
                    .verify(Duration.ofSeconds(10));

            // Verifications
            verify(quoteRepository).countByProvider(PROVIDER_NAME);
            verifyWebClientInteractions(times(1));

            verify(mapper).toQuote(zenQuote1);
            verify(quoteRepository).bulkInsertQuotesIgnoreConflicts(List.of(domainQuote1), PROVIDER_NAME);
        }

        @Test
        @DisplayName("9. Should retry on WebClientRequestException (e.g. ConnectException) then succeed")
        void fetchQuotes_whenWebClientRequestExceptionThenSuccess_shouldRetryAndSucceed() {
            // Arrange
            when(quoteRepository.countByProvider(PROVIDER_NAME)).thenReturn(Mono.just(0L));
            arrangeWebClientChainUpToRetrieve();

            // Simulate a WebClientRequestException, e.g., a connection timeout/error
            WebClientRequestException requestError = new WebClientRequestException(
                    new java.net.ConnectException("Simulated Connection refused"),
                    HttpMethod.GET,
                    null,
                    HttpHeaders.EMPTY
            );

            ZenQuote[] successResponse = {zenQuote1};

            // Use AtomicInteger to count effective calls to the deferred Mono logic
            AtomicInteger effectiveCallCount = new AtomicInteger(0);

            when(responseSpec.bodyToMono(eq(ZenQuote[].class)))
                    .thenAnswer(invocation -> {
                        return Mono.defer(() -> {
                            int count = effectiveCallCount.incrementAndGet();
                            if (count == 1) {
                                System.out.println("Mocked bodyToMono (WebClientRequestException Test): Attempt #" + count + " -> Returning WebClientRequestException ERROR");
                                return Mono.error(requestError);
                            } else {
                                System.out.println("Mocked bodyToMono (WebClientRequestException Test): Attempt #" + count + " -> Returning SUCCESS");
                                return Mono.just(successResponse);
                            }
                        });
                    });

            when(mapper.toQuote(zenQuote1)).thenReturn(domainQuote1);
            when(quoteRepository.bulkInsertQuotesIgnoreConflicts(List.of(domainQuote1), PROVIDER_NAME)).thenReturn(Mono.empty());

            // Act
            Mono<List<Quote>> resultMono = zenQuotesProvider.fetchQuotes();

            // Assert
            StepVerifier.create(resultMono)
                    .assertNext(resultList -> assertThat(resultList).containsExactly(domainQuote1))
                    .expectComplete()
                    .verify(Duration.ofSeconds(10));

            // Verifications
            verify(quoteRepository).countByProvider(PROVIDER_NAME);
            verifyWebClientInteractions(times(1));

            verify(mapper).toQuote(zenQuote1);
            verify(quoteRepository).bulkInsertQuotesIgnoreConflicts(List.of(domainQuote1), PROVIDER_NAME);
        }


        @Test
        @DisplayName("10. Should fail with QuoteProviderException after exhausting retries on 5xx error")
        void fetchQuotes_whenRetryExhaustedOn5xx_shouldFailWithProviderException() {
            // Arrange
            final String providerNameForExceptionMessage = "ZenQuotes";

            when(quoteRepository.countByProvider(PROVIDER_NAME)).thenReturn(Mono.just(0L));
            arrangeWebClientChainUpToRetrieve();

            WebClientResponseException serverError = WebClientResponseException.create(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(), // 500
                    "Internal Server Error",
                    HttpHeaders.EMPTY,
                    "Actual Server Error Body".getBytes(StandardCharsets.UTF_8),
                    StandardCharsets.UTF_8
            );

            AtomicInteger effectiveCallCount = new AtomicInteger(0);

            when(responseSpec.bodyToMono(eq(ZenQuote[].class)))
                    .thenAnswer(invocation -> Mono.defer(() -> {
                        int count = effectiveCallCount.incrementAndGet();
                        System.out.println("Mocked bodyToMono (Retry Exhausted on 5xx Test): Attempt #" + count + " -> Returning 5xx ERROR");
                        return Mono.error(serverError);
                    }));

            // Act
            Mono<List<Quote>> resultMono = zenQuotesProvider.fetchQuotes();

            // Assert
            StepVerifier.create(resultMono)
                    .expectErrorSatisfies(throwable -> {
                        assertThat(throwable)
                                .isInstanceOf(QuoteProviderException.class)
                                .hasCauseInstanceOf(WebClientResponseException.class);
                        assertThat(throwable.getCause()).isSameAs(serverError);
                    })
                    .verify(Duration.ofSeconds(10));

            // Verifications
            verify(quoteRepository).countByProvider(PROVIDER_NAME);

            // The WebClient chain setup calls (webClient.get(), .uri(), .retrieve(), and responseSpec.bodyToMono() method itself)
            // happen ONCE.
            verifyWebClientInteractions(times(1));

            // Verify that the deferred logic inside bodyToMono's mock was attempted
            assertThat(effectiveCallCount.get())
                    .as("Number of attempts for bodyToMono's deferred logic")
                    .isEqualTo(1 + ZenQuotesProvider.MAX_RETRY_ATTEMPTS);

            verifyNoInteractions(mapper); // Since all attempts failed, no mapping should occur
            verify(quoteRepository, never()).bulkInsertQuotesIgnoreConflicts(anyList(), anyString()); // No quotes should be persisted
        }
    }

    @SuppressWarnings("unchecked")
    private void arrangeWebClientChainUpToRetrieve() {
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(eq("/quotes"))).thenReturn(requestHeadersSpec); // specific URI
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    }

    private void verifyWebClientInteractions() {
        verifyWebClientInteractions(times(1));
    }

    private void verifyWebClientInteractions(VerificationMode times) {
        verify(webClient, times).get();
        verify(requestHeadersUriSpec, times).uri(eq("/quotes"));
        verify(requestHeadersSpec, times).retrieve();
        verify(responseSpec, times).bodyToMono(eq(ZenQuote[].class));
    }
}