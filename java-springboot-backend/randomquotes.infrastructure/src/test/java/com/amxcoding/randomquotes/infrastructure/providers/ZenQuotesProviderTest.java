package com.amxcoding.randomquotes.infrastructure.providers;

import com.amxcoding.randomquotes.application.exceptions.providers.QuoteProviderException;
import com.amxcoding.randomquotes.application.exceptions.repositories.QuotePersistenceException;
import com.amxcoding.randomquotes.application.interfaces.repositories.IQuoteRepository;
import com.amxcoding.randomquotes.domain.entities.Quote;
import com.amxcoding.randomquotes.infrastructure.common.Constants;
import com.amxcoding.randomquotes.infrastructure.mappers.ZenQuoteMapper;
import com.amxcoding.randomquotes.infrastructure.models.ZenQuote;
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
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

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

    private final String BASE_URL = "http://zenquotes.test"; // Dummy base URL for testing
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
        @DisplayName("1. Should return empty Optional when quote count threshold is met")
        void fetchQuotes_whenThresholdMet_shouldReturnEmptyOptional() {
            // Arrange: Mock repository count to be >= threshold
            when(quoteRepository.countByProvider(PROVIDER_NAME)).thenReturn(Mono.just(PROVIDER_THRESHOLD));

            // Act: Call the method under test
            Mono<Optional<List<Quote>>> resultMono = zenQuotesProvider.fetchQuotes();

            // Assert: Verify empty Optional is returned and no external/persistence calls made beyond count
            StepVerifier.create(resultMono)
                    .expectNext(Optional.empty())
                    .verifyComplete();

            // Verify only countByProvider was called
            verify(quoteRepository).countByProvider(PROVIDER_NAME);
            verifyNoInteractions(webClient, mapper); // No WebClient or Mapper interaction
            verify(quoteRepository, never()).bulkInsertQuotesIgnoreConflicts(anyList(), anyString()); // No bulk insert
        }

        @Test
        @DisplayName("2. Should fetch, map, persist, and return quotes when threshold not met")
        void fetchQuotes_whenThresholdNotMetAndFetchSuccess_shouldFetchMapPersistAndReturn() {
            // Arrange: Mock repository count below threshold
            when(quoteRepository.countByProvider(PROVIDER_NAME)).thenReturn(Mono.just(PROVIDER_THRESHOLD - 10));

            // Arrange: Mock the successful WebClient call chain
            arrangeWebClientChainUpToRetrieve();
            ZenQuote[] apiResponse = {zenQuote1, zenQuote2};
            when(responseSpec.bodyToMono(eq(ZenQuote[].class))).thenReturn(Mono.just(apiResponse));

            // Arrange: Mock the mapper
            when(mapper.toQuote(zenQuote1)).thenReturn(domainQuote1);
            when(mapper.toQuote(zenQuote2)).thenReturn(domainQuote2);

            // Arrange: Mock successful persistence
            when(quoteRepository.bulkInsertQuotesIgnoreConflicts(anyList(), eq(PROVIDER_NAME)))
                    .thenReturn(Mono.empty()); // Simulate successful void persistence

            // Act: Call the method under test
            Mono<Optional<List<Quote>>> resultMono = zenQuotesProvider.fetchQuotes();

            // Assert: Verify the Optional contains the mapped quotes and interactions are correct
            StepVerifier.create(resultMono)
                    .assertNext(optionalResult -> {
                        assertThat(optionalResult).isPresent();
                        assertThat(optionalResult.get()).containsExactlyInAnyOrder(domainQuote1, domainQuote2);
                    })
                    .verifyComplete();

            // Verify interactions
            verify(quoteRepository).countByProvider(PROVIDER_NAME);
            verifyWebClientInteractions(); // Check WebClient calls
            verify(mapper).toQuote(zenQuote1);
            verify(mapper).toQuote(zenQuote2);
            ArgumentCaptor<List<Quote>> listCaptor = ArgumentCaptor.forClass(List.class);
            verify(quoteRepository).bulkInsertQuotesIgnoreConflicts(listCaptor.capture(), eq(PROVIDER_NAME));
            assertThat(listCaptor.getValue()).containsExactlyInAnyOrder(domainQuote1, domainQuote2); // Verify correct list passed to repo
        }

        @Test
        @DisplayName("3. Should return empty Optional when fetch succeeds but returns empty array")
        void fetchQuotes_whenFetchReturnsEmptyArray_shouldReturnEmptyOptional() {
            // Arrange: Mock repository count below threshold
            when(quoteRepository.countByProvider(PROVIDER_NAME)).thenReturn(Mono.just(0L));

            // Arrange: Mock WebClient to return an empty array
            arrangeWebClientChainUpToRetrieve();
            ZenQuote[] emptyApiResponse = {};
            when(responseSpec.bodyToMono(eq(ZenQuote[].class))).thenReturn(Mono.just(emptyApiResponse));

            // Act: Call the method under test
            Mono<Optional<List<Quote>>> resultMono = zenQuotesProvider.fetchQuotes();

            // Assert: Verify empty Optional is returned, mapper and bulk insert not called
            StepVerifier.create(resultMono)
                    .expectNext(Optional.empty())
                    .verifyComplete();

            verify(quoteRepository).countByProvider(PROVIDER_NAME);
            verifyWebClientInteractions(); // WebClient was called
            verifyNoInteractions(mapper); // Mapper should not be called for empty array
            verify(quoteRepository, never()).bulkInsertQuotesIgnoreConflicts(anyList(), anyString()); // Bulk insert not called
        }

        @Test
        @DisplayName("4. Should propagate repository error during count check")
        void fetchQuotes_whenCountCheckFails_shouldPropagateError() {
            // Arrange: Mock repository count to return an error
            RuntimeException dbError = new RuntimeException("Database connection failed");
            when(quoteRepository.countByProvider(PROVIDER_NAME)).thenReturn(Mono.error(dbError));

            // Act: Call the method under test
            Mono<Optional<List<Quote>>> resultMono = zenQuotesProvider.fetchQuotes();

            // Assert: Verify the original database error is propagated
            StepVerifier.create(resultMono)
                    .expectErrorMatches(throwable -> throwable == dbError) // Expect the exact error instance
                    .verify();

            verify(quoteRepository).countByProvider(PROVIDER_NAME);
            verifyNoInteractions(webClient, mapper);
            verify(quoteRepository, never()).bulkInsertQuotesIgnoreConflicts(anyList(), anyString());
        }

        @Test
        @DisplayName("5. Should propagate repository error during bulk insert")
        void fetchQuotes_whenBulkInsertFails_shouldPropagateError() {
            // Arrange: Mock repository count below threshold
            when(quoteRepository.countByProvider(PROVIDER_NAME)).thenReturn(Mono.just(0L));

            // Arrange: Mock WebClient chain to succeed initially
            arrangeWebClientChainUpToRetrieve(); // Mock get->uri->retrieve
            ZenQuote[] apiResponse = {zenQuote1};
            when(responseSpec.bodyToMono(eq(ZenQuote[].class))).thenReturn(Mono.just(apiResponse)); // Mock successful body

            // Arrange: Mock mapper
            when(mapper.toQuote(zenQuote1)).thenReturn(domainQuote1);

            // Arrange: Mock bulk insert call to return a specific persistence error
            QuotePersistenceException persistError = new QuotePersistenceException(
                    "Database write during bulk insert failed", new RuntimeException("Simulated DB Error")
            );
            when(quoteRepository.bulkInsertQuotesIgnoreConflicts(anyList(), eq(PROVIDER_NAME)))
                    .thenReturn(Mono.error(persistError));

            // Act: Call the method under test
            Mono<Optional<List<Quote>>> resultMono = zenQuotesProvider.fetchQuotes();

            // Assert: Verify that the error is mapped to QuoteProviderException
            // and that the original persistence error is the cause.
            StepVerifier.create(resultMono)
                    .expectErrorSatisfies(throwable -> {
                        // Check that the error is the expected wrapper type
                        assertThat(throwable)
                                .isInstanceOf(QuoteProviderException.class);
                        assertThat(throwable.getCause())
                                .isSameAs(persistError);
                    })
                    .verify();

            // Assert: Verify interactions happened up to the point of the mocked error
            verify(quoteRepository).countByProvider(PROVIDER_NAME);
            verifyWebClientChainUpToRetrieve(times(1)); // Verify base chain called once
            verify(responseSpec, times(1)).bodyToMono(eq(ZenQuote[].class)); // Verify bodyToMono called once
            verify(mapper).toQuote(zenQuote1); // Mapper was called
            // Verify bulk insert was called with the correct arguments before it returned the mocked error
            verify(quoteRepository).bulkInsertQuotesIgnoreConflicts(List.of(domainQuote1), PROVIDER_NAME);
        }

        @Test
        @DisplayName("6. Should map WebClient 4xx error to QuoteProviderException without retry")
        void fetchQuotes_whenWebClient4xxError_shouldMapToProviderExceptionNoRetry() {
            // Arrange 1: Mock repository count below threshold to allow the fetch process to start.
            when(quoteRepository.countByProvider(PROVIDER_NAME)).thenReturn(Mono.just(0L));
            arrangeWebClientChainUpToRetrieve();

            // Arrange 3: Mock the final step (.bodyToMono) to return a 404 Not Found error.
            // This error is non-retryable according to the provider's logic.
            WebClientResponseException clientError = WebClientResponseException.create(
                    HttpStatus.NOT_FOUND.value(), "Not Found", HttpHeaders.EMPTY, "".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
            when(responseSpec.bodyToMono(eq(ZenQuote[].class))).thenReturn(Mono.error(clientError));

            // Act: Execute the method under test.
            Mono<Optional<List<Quote>>> resultMono = zenQuotesProvider.fetchQuotes();

            // Assert 1: Check that the final result is the expected QuoteProviderException,
            // wrapping the original clientError, and that the retry logic was not triggered.
            StepVerifier.create(resultMono)
                    .expectErrorSatisfies(throwable -> {
                        assertThat(throwable)
                                .as("Check error type is QuoteProviderException")
                                .isInstanceOf(QuoteProviderException.class);
                        assertThat(throwable.getCause())
                                .as("Check cause is the original client error instance")
                                .isSameAs(clientError);
                    })
                    .verify(); // Verify the error signal terminates the Mono

            // Assert 2: Verify the interactions with mocks.
            // Check count was called once.
            verify(quoteRepository).countByProvider(PROVIDER_NAME);
            // Check WebClient chain was called exactly once (no retries).
            verifyWebClientChainUpToRetrieve(times(1));
            verify(responseSpec, times(1)).bodyToMono(eq(ZenQuote[].class));
            // Check that mapping and persistence were *not* called due to the error.
            verifyNoInteractions(mapper);
            verify(quoteRepository, never()).bulkInsertQuotesIgnoreConflicts(anyList(), anyString());
        }


        // TODO FIX
//        @Test
//        @DisplayName("7. Should retry on 5xx error then succeed")
//        void fetchQuotes_whenWebClient5xxErrorThenSuccess_shouldRetryAndSucceed() {
//            // Arrange 1: Mock repository count to be below threshold, allowing the fetch process.
//            when(quoteRepository.countByProvider(PROVIDER_NAME)).thenReturn(Mono.just(0L));
//            arrangeWebClientChainUpToRetrieve();
//
//            // Arrange 3: Define the error (503) and success responses.
//            WebClientResponseException serverError = WebClientResponseException.create(
//                    HttpStatus.SERVICE_UNAVAILABLE.value(), "Service Unavailable", HttpHeaders.EMPTY, "".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
//            ZenQuote[] successResponse = {zenQuote1};
//
//            // Arrange 4: Mock the final .bodyToMono() step using thenAnswer to control response across invocations.
//            AtomicInteger invocationCounter = new AtomicInteger(0); // Counter for bodyToMono calls scoped to this test run
//            when(responseSpec.bodyToMono(eq(ZenQuote[].class)))
//                    .thenAnswer(invocation -> {
//                        int currentInvocation = invocationCounter.incrementAndGet();
//                        if (currentInvocation == 1) { // First actual call fails
//                            return Mono.error(serverError);
//                        } else { // Subsequent calls (i.e., the first retry) succeed
//                            return Mono.just(successResponse);
//                        }
//                    });
//
//            // Arrange 5: Mock downstream mapper and repository calls for the eventual successful path.
//            when(mapper.toQuote(zenQuote1)).thenReturn(domainQuote1);
//            when(quoteRepository.bulkInsertQuotesIgnoreConflicts(anyList(), eq(PROVIDER_NAME))).thenReturn(Mono.empty());
//
//            // Act Phase: Execute the method under test.
//            Mono<Optional<List<Quote>>> resultMono = zenQuotesProvider.fetchQuotes();
//
//            // Assert 1: Use StepVerifier to confirm the stream emits the expected successful result
//            // after the retry logic handles the initial error.
//            StepVerifier.create(resultMono)
//                    .assertNext(optionalResult -> {
//                        assertThat(optionalResult).as("Check Optional is present after retry").isPresent();
//                        assertThat(optionalResult.get()).as("Check list content after retry").containsExactly(domainQuote1);
//                    })
//                    .verifyComplete(); // Verify successful completion
//
//            // Assert 2: Verify the interactions with mocks occurred as expected.
//            // Check count was called once.
//            verify(quoteRepository).countByProvider(PROVIDER_NAME);
//            // Verify the *full* WebClient chain (including bodyToMono) was invoked exactly twice (initial call + 1 retry).
//            verifyWebClientInteractions(times(2)); // Verifies get, uri, retrieve, bodyToMono all called twice
//            // Verify the mapper was called once (only on the successful attempt).
//            verify(mapper).toQuote(zenQuote1);
//            // Verify persistence was called once (only on the successful attempt).
//            ArgumentCaptor<List<Quote>> listCaptor = ArgumentCaptor.forClass(List.class);
//            verify(quoteRepository).bulkInsertQuotesIgnoreConflicts(listCaptor.capture(), eq(PROVIDER_NAME));
//            assertThat(listCaptor.getValue())
//                    .as("Check quotes passed to bulkInsert after successful retry")
//                    .containsExactly(domainQuote1);
//        }

        // TODO FIX
//        @Test
//        @DisplayName("8. Should retry on TimeoutException then succeed")
//        void fetchQuotes_whenTimeoutErrorThenSuccess_shouldRetryAndSucceed() {
//            // Arrange: Mock repository count below threshold
//            when(quoteRepository.countByProvider(PROVIDER_NAME)).thenReturn(Mono.just(0L));
//            arrangeWebClientChainUpToRetrieve();
//
//            // Arrange: Mock WebClient to fail first with Timeout, then succeed
//            TimeoutException timeoutError = mock(TimeoutException.class);
//            ZenQuote[] successResponse = {zenQuote1};
//            when(responseSpec.bodyToMono(eq(ZenQuote[].class)))
//                    .thenReturn(Mono.error(timeoutError), Mono.just(successResponse));
//
//            // Arrange: Mock mapper and persistence for the success case
//            when(mapper.toQuote(zenQuote1)).thenReturn(domainQuote1);
//            when(quoteRepository.bulkInsertQuotesIgnoreConflicts(anyList(), eq(PROVIDER_NAME))).thenReturn(Mono.empty());
//
//
//            // Act: Call the method under test
//            Mono<Optional<List<Quote>>> resultMono = zenQuotesProvider.fetchQuotes();
//
//            // Assert: Verify successful result after retry
//            StepVerifier.create(resultMono)
//                    .assertNext(optionalResult -> {
//                        assertThat(optionalResult).isPresent();
//                        assertThat(optionalResult.get()).containsExactly(domainQuote1);
//                    })
//                    .verifyComplete();
//
//            // Verify WebClient was called twice (initial + 1 retry)
//            verifyWebClientInteractions(times(2));
//            verify(mapper).toQuote(zenQuote1);
//            verify(quoteRepository).bulkInsertQuotesIgnoreConflicts(List.of(domainQuote1), PROVIDER_NAME);
//        }

        // TODO FIX
//        @Test
//        @DisplayName("9. Should fail with QuoteProviderException after exhausting retries on 5xx error")
//        void fetchQuotes_whenRetryExhaustedOn5xx_shouldFailWithProviderException() {
//            // Arrange: Mock repository count below threshold
//            when(quoteRepository.countByProvider(PROVIDER_NAME)).thenReturn(Mono.just(0L));
//
//            // Arrange: Mock WebClient to consistently fail with 500 error
//            WebClientResponseException serverError = WebClientResponseException.create(
//                    HttpStatus.INTERNAL_SERVER_ERROR.value(), "Server Error", HttpHeaders.EMPTY, "".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
//            // Mock the chain to always fail
//            when(responseSpec.bodyToMono(eq(ZenQuote[].class))).thenReturn(Mono.error(serverError));
//
//
//            // Act: Call the method under test
//            Mono<Optional<List<Quote>>> resultMono = zenQuotesProvider.fetchQuotes();
//
//            // Assert: Verify failure with QuoteProviderException wrapping the *last* 500 error
//            StepVerifier.create(resultMono)
//                    .expectErrorSatisfies(throwable -> {
//                        assertThat(throwable).isInstanceOf(QuoteProviderException.class)
//                                .hasMessageContaining("Error fetching ZenQuotes (status: 500)")
//                                .hasCause(serverError); // Should wrap the last error
//                    })
//                    .verify(Duration.ofSeconds(10)); // Allow time for retries
//
//            // Verify WebClient was called 1 (initial) + 3 (retries) = 4 times
//            verifyWebClientInteractions(times(1 + 3)); // MAX_RETRY_ATTEMPTS is 3
//            verifyNoInteractions(mapper);
//            verify(quoteRepository, never()).bulkInsertQuotesIgnoreConflicts(anyList(), anyString());
//        }
    }

    /**
     * Mocks the fluent WebClient chain up to the .retrieve() call,
     * returning the mock ResponseSpec.
     * Call this in the Arrange block, then mock responseSpec.bodyToMono() as needed.
     */
    @SuppressWarnings("unchecked")
    private void arrangeWebClientChainUpToRetrieve() {
        // Mock: webClient.get()
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        // Mock: .uri("/quotes") - Use anyString() or specific eq("/quotes")
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        // Mock: .retrieve() -> return mock ResponseSpec
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    }

    /** Verifies the standard WebClient interaction chain up to retrieve() was called (default: once) */
    private void verifyWebClientChainUpToRetrieve() {
        verifyWebClientChainUpToRetrieve(times(1));
    }

    /** Verifies the standard WebClient interaction chain up to retrieve() was called a specific number of times */
    private void verifyWebClientChainUpToRetrieve(VerificationMode times) {
        verify(webClient, times).get();
        verify(requestHeadersUriSpec, times).uri(eq("/quotes")); // Make URI specific
        verify(requestHeadersSpec, times).retrieve();
    }

    /** Verifies the standard WebClient interaction chain was called (default: once) */
    private void verifyWebClientInteractions() {
        verifyWebClientInteractions(times(1));
    }

    /** Verifies the standard WebClient interaction chain was called a specific number of times */
    private void verifyWebClientInteractions(VerificationMode times) {
        verify(webClient, times).get();
        verify(requestHeadersUriSpec, times).uri(eq("/quotes")); // Verify specific URI
        verify(requestHeadersSpec, times).retrieve();
        verify(responseSpec, times).bodyToMono(eq(ZenQuote[].class));
    }

}