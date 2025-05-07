package com.amxcoding.randomquotes.application.services;

import com.amxcoding.randomquotes.application.exceptions.providers.QuoteProviderException;
import com.amxcoding.randomquotes.application.exceptions.repositories.QuotePersistenceException;
import com.amxcoding.randomquotes.application.exceptions.services.QuoteFetchOrchestratorException;
import com.amxcoding.randomquotes.application.interfaces.providers.IQuoteProvider;
import com.amxcoding.randomquotes.application.interfaces.repositories.IQuoteRepository;
import com.amxcoding.randomquotes.domain.entities.Quote;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
@DisplayName("QuoteFetchOrchestrator Unit Tests")
class QuoteFetchOrchestratorTest {

    @Mock
    private IQuoteRepository quoteRepository;
    @Mock
    private IQuoteProvider provider1;
    @Mock
    private IQuoteProvider provider2;

    private QuoteFetchOrchestrator quoteFetchOrchestrator;

    private Quote quote1;
    private Quote quote2;
    private List<Quote> provider1Quotes;
    private List<Quote> provider2Quotes;
    private List<Quote> dbQuotes;

    private static final int FALLBACK_RANDOM_QUOTE_AMOUNT = 50;

    @BeforeEach
    void setUp() {
        // Arrange: Initialize common test data and mock behaviors for each test.
        quote1 = new Quote(1L, "Author One", "Text One", 1);
        quote2 = new Quote(2L, "Author Two", "Text Two", 2);
        provider1Quotes = List.of(quote1);
        provider2Quotes = List.of(quote2);
        dbQuotes = List.of(
                new Quote(101L, "DB Author 1", "DB Text 1", 11),
                new Quote(102L, "DB Author 2", "DB Text 2", 22)
        );

        reset(quoteRepository, provider1, provider2); // Reset mocks before each test.

        // Arrange: Lenient stubbing for provider names, as they might be called for logging.
        lenient().when(provider1.getProviderName()).thenReturn("Provider1");
        lenient().when(provider2.getProviderName()).thenReturn("Provider2");
    }

    private void initializeOrchestrator(List<IQuoteProvider> providers) {
        // Helper for Arrange phase: Creates and initializes the orchestrator instance.
        List<IQuoteProvider> mutableProviders = new ArrayList<>(providers);
        mutableProviders.sort(AnnotationAwareOrderComparator.INSTANCE); // Simulate constructor sorting.
        quoteFetchOrchestrator = new QuoteFetchOrchestrator(mutableProviders, quoteRepository);
    }

    @Nested
    @DisplayName("getQuotes() - Provider Scenarios")
    class ProviderScenarios {

        @Test
        @DisplayName("1. Should return quotes from the first provider if it succeeds")
        void getQuotes_whenFirstProviderSucceeds_shouldReturnItsQuotes() {
            // Arrange: Setup orchestrator and mock provider1 to return quotes.
            initializeOrchestrator(List.of(provider1, provider2));
            when(provider1.fetchQuotes()).thenReturn(Mono.just(provider1Quotes));

            // Act: Execute the method under test.
            Mono<List<Quote>> resultMono = quoteFetchOrchestrator.getQuotes();

            // Assert: Verify that the result contains quotes from provider1 and interactions are correct.
            StepVerifier.create(resultMono)
                    .expectNext(provider1Quotes)
                    .verifyComplete();

            verify(provider1).fetchQuotes();
            verifyNoInteractions(provider2); // Provider2 should not be called.
            verifyNoInteractions(quoteRepository); // Repository should not be called.
        }

        @Test
        @DisplayName("2. Should return quotes from the second provider if the first returns empty list")
        void getQuotes_whenFirstProviderEmpty_andSecondSucceeds_shouldReturnSecondProviderQuotes() {
            // Arrange: Mock provider1 to return an empty list, provider2 to return quotes.
            initializeOrchestrator(List.of(provider1, provider2));
            when(provider1.fetchQuotes()).thenReturn(Mono.just(Collections.emptyList()));
            when(provider2.fetchQuotes()).thenReturn(Mono.just(provider2Quotes));

            // Act: Execute the method under test.
            Mono<List<Quote>> resultMono = quoteFetchOrchestrator.getQuotes();

            // Assert: Verify that the result contains quotes from provider2.
            StepVerifier.create(resultMono)
                    .expectNext(provider2Quotes)
                    .verifyComplete();

            verify(provider1).fetchQuotes(); // Provider1 is called.
            verify(provider2).fetchQuotes(); // Provider2 is called.
            verifyNoInteractions(quoteRepository); // Repository should not be called.
        }

        @Test
        @DisplayName("3. Should return quotes from second provider if first fails recoverably (QuoteProviderException)")
        void getQuotes_whenFirstProviderFailsRecoverably_andSecondSucceeds_shouldReturnSecondProviderQuotes() {
            // Arrange: Mock provider1 to fail with a recoverable error, provider2 to return quotes.
            initializeOrchestrator(List.of(provider1, provider2));
            QuoteProviderException provider1Error = new QuoteProviderException("Provider1 API timeout", null);
            when(provider1.fetchQuotes()).thenReturn(Mono.error(provider1Error));
            when(provider2.fetchQuotes()).thenReturn(Mono.just(provider2Quotes));

            // Act: Execute the method under test.
            Mono<List<Quote>> resultMono = quoteFetchOrchestrator.getQuotes();

            // Assert: Verify that the result contains quotes from provider2.
            StepVerifier.create(resultMono)
                    .expectNext(provider2Quotes)
                    .verifyComplete();

            verify(provider1).fetchQuotes();
            verify(provider1).getProviderName(); // Called for logging the warning.
            verify(provider2).fetchQuotes();
            verifyNoInteractions(quoteRepository);
        }

        @Test
        @DisplayName("4. Should propagate critical error if first provider fails with QuotePersistenceException")
        void getQuotes_whenFirstProviderFailsCritically_shouldPropagateError() {
            // Arrange: Mock provider1 to fail with a critical persistence error.
            initializeOrchestrator(List.of(provider1, provider2));
            QuotePersistenceException criticalError = new QuotePersistenceException("DB connection lost during provider1 persistence", null);
            when(provider1.fetchQuotes()).thenReturn(Mono.error(criticalError));

            // Act: Execute the method under test.
            Mono<List<Quote>> resultMono = quoteFetchOrchestrator.getQuotes();

            // Assert: Verify that the specific critical error is propagated, wrapped by the orchestrator's exception.
            StepVerifier.create(resultMono)
                    .expectErrorSatisfies(error -> {
                        assertThat(error)
                                .isInstanceOf(QuoteFetchOrchestratorException.class)
                                .hasMessageContaining("CRITICAL error failed to get quotes: " + criticalError.getMessage())
                                .hasCause(criticalError); // The original critical error should be the cause.
                    })
                    .verify();

            verify(provider1).fetchQuotes();
            verify(provider1).getProviderName(); // Called for logging the critical error.
            verifyNoInteractions(provider2); // Provider2 should not be called.
            verifyNoInteractions(quoteRepository); // Repository should not be called.
        }

        @Test
        @DisplayName("5. Should wrap and propagate unexpected error if first provider fails unexpectedly")
        void getQuotes_whenFirstProviderFailsUnexpectedly_shouldWrapAndPropagateError() {
            // Arrange: Mock provider1 to fail with an unexpected runtime error.
            initializeOrchestrator(List.of(provider1, provider2));
            RuntimeException unexpectedError = new RuntimeException("Something unexpected broke in Provider1");
            when(provider1.fetchQuotes()).thenReturn(Mono.error(unexpectedError));

            // Act: Execute the method under test.
            Mono<List<Quote>> resultMono = quoteFetchOrchestrator.getQuotes();

            // Assert: Verify that the error is wrapped twice, first by the inner handler, then by the final orchestrator handler.
            StepVerifier.create(resultMono)
                    .expectErrorSatisfies(error -> {
                        assertThat(error) // Outermost wrapper (final onErrorMap).
                                .isInstanceOf(QuoteFetchOrchestratorException.class)
                                .hasMessageContaining("CRITICAL error failed to get quotes:");

                        assertThat(error.getCause()) // Inner wrapper (from concatMap's onErrorResume).
                                .isInstanceOf(QuoteFetchOrchestratorException.class)
                                .hasMessageContaining("Unexpected error: " + unexpectedError.getMessage());

                        assertThat(error.getCause().getCause()) // Original unexpected error.
                                .isSameAs(unexpectedError);
                    })
                    .verify();

            verify(provider1).fetchQuotes();
            verify(provider1).getProviderName(); // Called for logging the unexpected error.
            verifyNoInteractions(provider2);
            verifyNoInteractions(quoteRepository);
        }
    }

    @Nested
    @DisplayName("getQuotes() - Database Fallback Scenarios")
    class DbFallbackScenarios {

        @BeforeEach
        void setupProvidersToFailOrReturnEmpty() {
            // Arrange: Common setup for DB fallback tests: initialize orchestrator and make all providers fail or return empty.
            initializeOrchestrator(List.of(provider1, provider2));
            when(provider1.fetchQuotes()).thenReturn(Mono.just(Collections.emptyList())); // Provider 1 returns empty.
            when(provider2.fetchQuotes()).thenReturn(Mono.error(new QuoteProviderException("Provider 2 unavailable", null))); // Provider 2 fails.
        }

        @Test
        @DisplayName("6. Fallback: Should call findRandomQuotes from DB when all providers exhausted")
        void getQuotes_whenAllProvidersExhausted_shouldCallFindRandomQuotes() {
            // Arrange: Mock the repository's findRandomQuotes to return DB quotes.
            when(quoteRepository.findRandomQuotes(FALLBACK_RANDOM_QUOTE_AMOUNT))
                    .thenReturn(Mono.just(dbQuotes));

            // Act: Execute the method under test.
            Mono<List<Quote>> resultMono = quoteFetchOrchestrator.getQuotes();

            // Assert: Verify that the result contains quotes from the database.
            StepVerifier.create(resultMono)
                    .expectNext(dbQuotes)
                    .verifyComplete();

            verify(provider1).fetchQuotes(); // Provider1 is tried.
            verify(provider2).fetchQuotes(); // Provider2 is tried.
            verify(provider2).getProviderName(); // Called for logging provider2's error.
            verify(quoteRepository).findRandomQuotes(FALLBACK_RANDOM_QUOTE_AMOUNT); // DB fallback is invoked.
        }

        @Test
        @DisplayName("7. Fallback: Should return empty list when findRandomQuotes from DB returns empty")
        void getQuotes_whenAllProvidersExhausted_andFindRandomQuotesEmpty_shouldReturnEmptyList() {
            // Arrange: Mock the repository's findRandomQuotes to return an empty list.
            when(quoteRepository.findRandomQuotes(FALLBACK_RANDOM_QUOTE_AMOUNT))
                    .thenReturn(Mono.just(Collections.emptyList()));

            // Act: Execute the method under test.
            Mono<List<Quote>> resultMono = quoteFetchOrchestrator.getQuotes();

            // Assert: Verify that the result is an empty list.
            StepVerifier.create(resultMono)
                    .expectNext(Collections.emptyList())
                    .verifyComplete();

            verify(provider1).fetchQuotes();
            verify(provider2).fetchQuotes();
            verify(provider2).getProviderName();
            verify(quoteRepository).findRandomQuotes(FALLBACK_RANDOM_QUOTE_AMOUNT); // DB fallback is invoked.
        }


        @Test
        @DisplayName("8. Fallback Error: Should propagate error when findRandomQuotes from DB fails")
        void getQuotes_whenAllProvidersExhausted_andFindRandomQuotesFails_shouldPropagateError() {
            // Arrange: Mock the repository's findRandomQuotes to return an error.
            QuotePersistenceException dbFetchError = new QuotePersistenceException("DB random fetch failed", null);
            when(quoteRepository.findRandomQuotes(FALLBACK_RANDOM_QUOTE_AMOUNT)).thenReturn(Mono.error(dbFetchError));

            // Act: Execute the method under test.
            Mono<List<Quote>> resultMono = quoteFetchOrchestrator.getQuotes();

            // Assert: Verify that the DB error is propagated, wrapped by the orchestrator's final exception handler.
            StepVerifier.create(resultMono)
                    .expectErrorSatisfies(error -> {
                        assertThat(error) // Outermost wrapper (final onErrorMap).
                                .isInstanceOf(QuoteFetchOrchestratorException.class)
                                .hasMessageContaining("CRITICAL error failed to get quotes: " + dbFetchError.getMessage());

                        assertThat(error.getCause()) // The original DB error.
                                .isSameAs(dbFetchError);
                    })
                    .verify();

            verify(provider1).fetchQuotes();
            verify(provider2).fetchQuotes();
            verify(provider2).getProviderName();
            verify(quoteRepository).findRandomQuotes(FALLBACK_RANDOM_QUOTE_AMOUNT); // DB fallback is attempted.
        }
    }

    @Nested
    @DisplayName("getQuotes() - Edge Cases")
    class EdgeCaseScenarios {

        @Test
        @DisplayName("9. No Providers: Should fallback to DB immediately and use findRandomQuotes")
        void getQuotes_whenNoProvidersConfigured_shouldFallbackToDbUsingFindRandomQuotes() {
            // Arrange: Initialize orchestrator with an empty list of providers. Mock DB to return quotes.
            initializeOrchestrator(Collections.emptyList());
            when(quoteRepository.findRandomQuotes(FALLBACK_RANDOM_QUOTE_AMOUNT))
                    .thenReturn(Mono.just(dbQuotes));

            // Act: Execute the method under test.
            Mono<List<Quote>> resultMono = quoteFetchOrchestrator.getQuotes();

            // Assert: Verify that the result contains quotes from the database.
            StepVerifier.create(resultMono)
                    .expectNext(dbQuotes)
                    .verifyComplete();

            verifyNoInteractions(provider1, provider2); // No providers should be called.
            verify(quoteRepository).findRandomQuotes(FALLBACK_RANDOM_QUOTE_AMOUNT); // DB fallback is invoked directly.
        }

        @Test
        @DisplayName("10. Providers one returns empty list, second succeeds")
        void getQuotes_whenFirstProviderReturnsEmptyList_shouldProceedToNext() {
            // Arrange: Initialize orchestrator. Mock provider1 to return empty, provider2 to succeed.
            initializeOrchestrator(List.of(provider1, provider2));
            when(provider1.fetchQuotes()).thenReturn(Mono.just(Collections.emptyList()));
            when(provider2.fetchQuotes()).thenReturn(Mono.just(provider2Quotes));

            // Act: Execute the method under test.
            Mono<List<Quote>> resultMono = quoteFetchOrchestrator.getQuotes();

            // Assert: Verify that the result contains quotes from provider2.
            StepVerifier.create(resultMono)
                    .expectNext(provider2Quotes)
                    .verifyComplete();

            verify(provider1).fetchQuotes();
            verify(provider2).fetchQuotes();
            verifyNoInteractions(quoteRepository); // Repository should not be called.
        }

        @Test
        @DisplayName("11. All providers return empty list, fallback to DB returns empty list, should result in empty list")
        void getQuotes_whenAllProvidersAndDbFallbackReturnEmpty_shouldReturnEmptyList() {
            // Arrange: Initialize orchestrator. Mock all providers and DB fallback to return empty lists.
            initializeOrchestrator(List.of(provider1, provider2));
            when(provider1.fetchQuotes()).thenReturn(Mono.just(Collections.emptyList()));
            when(provider2.fetchQuotes()).thenReturn(Mono.just(Collections.emptyList()));
            when(quoteRepository.findRandomQuotes(FALLBACK_RANDOM_QUOTE_AMOUNT))
                    .thenReturn(Mono.just(Collections.emptyList()));

            // Act: Execute the method under test.
            Mono<List<Quote>> resultMono = quoteFetchOrchestrator.getQuotes();

            // Assert: Verify that the final result is an empty list due to the .defaultIfEmpty(List.of()) safeguard.
            StepVerifier.create(resultMono)
                    .expectNext(Collections.emptyList())
                    .verifyComplete();

            verify(provider1).fetchQuotes(); // Provider1 is tried.
            verify(provider2).fetchQuotes(); // Provider2 is tried.
            verify(quoteRepository).findRandomQuotes(FALLBACK_RANDOM_QUOTE_AMOUNT); // DB fallback is invoked.
        }
    }
}