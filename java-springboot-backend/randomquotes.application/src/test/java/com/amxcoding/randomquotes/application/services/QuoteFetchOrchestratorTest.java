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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
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
        quote1 = new Quote(1L, "Author One", "Text One", 1);
        quote2 = new Quote(2L, "Author Two", "Text Two", 2);
        provider1Quotes = List.of(quote1);
        provider2Quotes = List.of(quote2);
        dbQuotes = List.of(
                new Quote(101L, "DB Author 1", "DB Text 1", 11),
                new Quote(102L, "DB Author 2", "DB Text 2", 22)
        );

        reset(quoteRepository, provider1, provider2);

        lenient().when(provider1.getProviderName()).thenReturn("Provider1");
        lenient().when(provider2.getProviderName()).thenReturn("Provider2");
    }

    // Helper to create the orchestrator instance, simulating constructor logic
    private void initializeOrchestrator(List<IQuoteProvider> providers) {
        // Create a mutable list to allow sorting, as the constructor does
        List<IQuoteProvider> mutableProviders = new ArrayList<>(providers);
        // Simulate the sorting based on @Order (or natural order if no annotation)
        mutableProviders.sort(AnnotationAwareOrderComparator.INSTANCE);
        quoteFetchOrchestrator = new QuoteFetchOrchestrator(mutableProviders, quoteRepository);
    }

    @Nested
    @DisplayName("getQuotes() - Provider Scenarios")
    class ProviderScenarios {

        @Test
        @DisplayName("1. Should return quotes from the first provider if it succeeds")
        void getQuotes_whenFirstProviderSucceeds_shouldReturnItsQuotes() {
            // Arrange: Initialize with two providers. Provider1 will succeed.
            // Note: Order matters. If provider2 had @Order(1) and provider1 @Order(2),
            // the sorting in initializeOrchestrator would place provider2 first.
            // Assuming natural order or provider1 is explicitly ordered first.
            initializeOrchestrator(List.of(provider1, provider2));
            when(provider1.fetchQuotes()).thenReturn(Mono.just(Optional.of(provider1Quotes)));

            // Act: Call the method under test.
            Mono<Optional<List<Quote>>> resultMono = quoteFetchOrchestrator.getQuotes();

            // Assert: Verify the result contains quotes from provider1.
            StepVerifier.create(resultMono)
                    .expectNext(Optional.of(provider1Quotes))
                    .verifyComplete();

            // Assert: Verify interactions. Only provider1 should be called.
            verify(provider1).fetchQuotes();
            verifyNoInteractions(provider2);
            verifyNoInteractions(quoteRepository);
        }

        @Test
        @DisplayName("2. Should return quotes from the second provider if the first returns empty")
        void getQuotes_whenFirstProviderEmpty_andSecondSucceeds_shouldReturnSecondProviderQuotes() {
            // Arrange: Provider1 returns empty, Provider2 returns quotes.
            initializeOrchestrator(List.of(provider1, provider2));
            when(provider1.fetchQuotes()).thenReturn(Mono.just(Optional.empty()));
            when(provider2.fetchQuotes()).thenReturn(Mono.just(Optional.of(provider2Quotes)));

            // Act
            Mono<Optional<List<Quote>>> resultMono = quoteFetchOrchestrator.getQuotes();

            // Assert: Expect quotes from provider2.
            StepVerifier.create(resultMono)
                    .expectNext(Optional.of(provider2Quotes))
                    .verifyComplete();

            // Assert: Both providers were called. Repository was not.
            verify(provider1).fetchQuotes();
            verify(provider2).fetchQuotes();
            verifyNoInteractions(quoteRepository);
        }

        @Test
        @DisplayName("3. Should return quotes from second provider if first fails recoverably (QuoteProviderException)")
        void getQuotes_whenFirstProviderFailsRecoverably_andSecondSucceeds_shouldReturnSecondProviderQuotes() {
            // Arrange: Provider1 fails with a 'recoverable' exception, Provider2 succeeds.
            initializeOrchestrator(List.of(provider1, provider2));
            QuoteProviderException provider1Error = new QuoteProviderException("Provider1 API timeout", null);
            // Mock provider1 to return the specific error type handled by onErrorResume to continue
            when(provider1.fetchQuotes()).thenReturn(Mono.error(provider1Error));
            when(provider2.fetchQuotes()).thenReturn(Mono.just(Optional.of(provider2Quotes)));

            // Act
            Mono<Optional<List<Quote>>> resultMono = quoteFetchOrchestrator.getQuotes();

            // Assert: Expect quotes from provider2 as the orchestrator recovered from provider1's error.
            StepVerifier.create(resultMono)
                    .expectNext(Optional.of(provider2Quotes))
                    .verifyComplete();

            // Assert: Both providers were called. Repository was not.
            // Verify getProviderName was likely called due to logging in onErrorResume
            verify(provider1).fetchQuotes();
            verify(provider1).getProviderName(); // Called for logging the warning
            verify(provider2).fetchQuotes();
            verifyNoInteractions(quoteRepository);
        }

        @Test
        @DisplayName("4. Should propagate critical error immediately if first provider fails with QuotePersistenceException")
        void getQuotes_whenFirstProviderFailsCritically_shouldPropagateError() {
            // Arrange: Provider1 fails with a 'critical' persistence error.
            initializeOrchestrator(List.of(provider1, provider2));
            QuotePersistenceException criticalError = new QuotePersistenceException("DB connection lost during provider1 persistence", null);
            // Mock provider1 to return the critical error
            when(provider1.fetchQuotes()).thenReturn(Mono.error(criticalError));

            // Act
            Mono<Optional<List<Quote>>> resultMono = quoteFetchOrchestrator.getQuotes();

            // Assert: Expect the specific critical error, wrapped by the orchestrator's final onErrorMap.
            StepVerifier.create(resultMono)
                    .expectErrorSatisfies(error -> {
                        assertThat(error)
                                .isInstanceOf(QuoteFetchOrchestratorException.class)
                                .hasMessageContaining("CRITICAL error failed to get quotes: " + criticalError.getMessage()) // Check outer message
                                .hasCause(criticalError); // Check that the original critical error is the direct cause
                    })
                    .verify();

            // Assert: Only provider1 was called.
            verify(provider1).fetchQuotes();
            verify(provider1).getProviderName(); // Called for logging the critical error
            verifyNoInteractions(provider2);
            verifyNoInteractions(quoteRepository);
        }

        @Test
        @DisplayName("5. Should wrap and propagate unexpected error if first provider fails unexpectedly")
        void getQuotes_whenFirstProviderFailsUnexpectedly_shouldWrapAndPropagateError() {
            // Arrange: Provider1 fails with an unexpected RuntimeException.
            initializeOrchestrator(List.of(provider1, provider2));
            RuntimeException unexpectedError = new RuntimeException("Something unexpected broke in Provider1");
            when(provider1.fetchQuotes()).thenReturn(Mono.error(unexpectedError));

            // Act
            Mono<Optional<List<Quote>>> resultMono = quoteFetchOrchestrator.getQuotes();

            // Assert: Expect the error to be wrapped twice.
            // 1. Inner wrap: In onErrorResume (else block) -> QuoteFetchOrchestratorException("Unexpected error...")
            // 2. Outer wrap: Final onErrorMap -> QuoteFetchOrchestratorException("CRITICAL error...")
            StepVerifier.create(resultMono)
                    .expectErrorSatisfies(error -> {
                        assertThat(error)
                                .isInstanceOf(QuoteFetchOrchestratorException.class)
                                .hasMessageContaining("CRITICAL error failed to get quotes: Unexpected error:"); // Outer wrapper message check

                        assertThat(error.getCause())
                                .isInstanceOf(QuoteFetchOrchestratorException.class) // Inner wrapper type check
                                .hasMessage("Unexpected error: " + unexpectedError.getMessage()); // Inner wrapper message check

                        assertThat(error.getCause().getCause())
                                .isSameAs(unexpectedError); // Original cause check
                    })
                    .verify();

            // Assert: Only provider1 was called.
            verify(provider1).fetchQuotes();
            verify(provider1).getProviderName(); // Called for logging the unexpected error
            verifyNoInteractions(provider2);
            verifyNoInteractions(quoteRepository);
        }
    }

    @Nested
    @DisplayName("getQuotes() - Database Fallback Scenarios")
    class DbFallbackScenarios {

        @BeforeEach
        void setupProvidersToFailOrReturnEmpty() {
            // Arrange common setup for fallback tests: Ensure providers don't succeed.
            initializeOrchestrator(List.of(provider1, provider2));
            // Provider 1 returns empty optional
            when(provider1.fetchQuotes()).thenReturn(Mono.just(Optional.empty()));
            // Provider 2 fails recoverably
            when(provider2.fetchQuotes()).thenReturn(Mono.error(new QuoteProviderException("Provider 2 unavailable", null)));
        }

        @Test
        @DisplayName("6. Fallback: Should call findRandomQuotes when DB count >= threshold")
        void getQuotes_whenAllProvidersFail_andDbCountHigh_shouldCallFindRandomQuotes() {
            // Arrange: Mock DB count high, triggering findRandomQuotes. Mock findRandomQuotes success.
            when(quoteRepository.count()).thenReturn(Mono.just((long) FALLBACK_RANDOM_QUOTE_AMOUNT)); // Exact threshold or more
            when(quoteRepository.findRandomQuotes(FALLBACK_RANDOM_QUOTE_AMOUNT))
                    .thenReturn(Mono.just(Optional.of(dbQuotes)));

            // Act
            Mono<Optional<List<Quote>>> resultMono = quoteFetchOrchestrator.getQuotes();

            // Assert: Expect quotes from the DB (findRandomQuotes path).
            StepVerifier.create(resultMono)
                    .expectNext(Optional.of(dbQuotes))
                    .verifyComplete();

            // Assert: Verify interactions. Providers tried, then DB count and findRandomQuotes called.
            verify(provider1).fetchQuotes();
            verify(provider2).fetchQuotes();
            verify(provider2).getProviderName(); // For logging recoverable error
            verify(quoteRepository).count();
            verify(quoteRepository).findRandomQuotes(FALLBACK_RANDOM_QUOTE_AMOUNT);
            verify(quoteRepository, never()).findAllQuotes(anyInt()); // Ensure findAllQuotes was NOT called
        }

        @Test
        @DisplayName("7. Fallback: Should call findAllQuotes when DB count < threshold")
        void getQuotes_whenAllProvidersFail_andDbCountLow_shouldCallFindAllQuotes() {
            // Arrange: Mock DB count low, triggering findAllQuotes. Mock findAllQuotes success.
            when(quoteRepository.count()).thenReturn(Mono.just((long) FALLBACK_RANDOM_QUOTE_AMOUNT - 1)); // Less than threshold
            // findAllQuotes returns a Flux in the repository interface/impl
            when(quoteRepository.findAllQuotes(FALLBACK_RANDOM_QUOTE_AMOUNT))
                    .thenReturn(Flux.fromIterable(dbQuotes));

            // Act
            Mono<Optional<List<Quote>>> resultMono = quoteFetchOrchestrator.getQuotes();

            // Assert: Expect quotes from the DB (findAllQuotes path).
            StepVerifier.create(resultMono)
                    .expectNext(Optional.of(dbQuotes)) // Service collects the Flux to a List and wraps in Optional
                    .verifyComplete();

            // Assert: Verify interactions. Providers tried, then DB count and findAllQuotes called.
            verify(provider1).fetchQuotes();
            verify(provider2).fetchQuotes();
            verify(provider2).getProviderName(); // For logging recoverable error
            verify(quoteRepository).count();
            verify(quoteRepository).findAllQuotes(FALLBACK_RANDOM_QUOTE_AMOUNT);
            verify(quoteRepository, never()).findRandomQuotes(anyInt()); // Ensure findRandomQuotes was NOT called
        }

        @Test
        @DisplayName("8. Fallback: Should return empty Optional when findAllQuotes returns empty")
        void getQuotes_whenAllProvidersFail_andFindAllEmpty_shouldReturnEmpty() {
            // Arrange: Mock DB count low. Mock findAllQuotes returns empty Flux.
            when(quoteRepository.count()).thenReturn(Mono.just(10L)); // Count < Threshold
            when(quoteRepository.findAllQuotes(FALLBACK_RANDOM_QUOTE_AMOUNT)).thenReturn(Flux.empty());

            // Act
            Mono<Optional<List<Quote>>> resultMono = quoteFetchOrchestrator.getQuotes();

            // Assert: Expect empty Optional as DB fallback yielded no quotes.
            StepVerifier.create(resultMono)
                    .expectNext(Optional.empty()) // The service maps empty list from collectList to Optional.empty()
                    .verifyComplete();

            // Assert: Verify interactions.
            verify(provider1).fetchQuotes();
            verify(provider2).fetchQuotes();
            verify(provider2).getProviderName(); // For logging recoverable error
            verify(quoteRepository).count();
            verify(quoteRepository).findAllQuotes(FALLBACK_RANDOM_QUOTE_AMOUNT);
            verify(quoteRepository, never()).findRandomQuotes(anyInt());
        }

        @Test
        @DisplayName("9. Fallback Error: Should propagate error when DB count fails")
        void getQuotes_whenAllProvidersFail_andDbCountFails_shouldPropagateError() {
            // Arrange: Mock DB count to fail.
            QuotePersistenceException dbCountError = new QuotePersistenceException("DB count failed", null);
            when(quoteRepository.count()).thenReturn(Mono.error(dbCountError));

            // Act
            Mono<Optional<List<Quote>>> resultMono = quoteFetchOrchestrator.getQuotes();

            // Assert: Expect error wrapped twice.
            // 1. Inner wrap: fetchRandomFromDb's onErrorMap -> QuotePersistenceException("Database fallback failed...")
            // 2. Outer wrap: Final onErrorMap -> QuoteFetchOrchestratorException("CRITICAL error...")
            StepVerifier.create(resultMono)
                    .expectErrorSatisfies(error -> {
                        assertThat(error)
                                .isInstanceOf(QuoteFetchOrchestratorException.class)
                                .hasMessageContaining("CRITICAL error failed to get quotes: Database fallback failed"); // Outer wrapper

                        assertThat(error.getCause())
                                .isInstanceOf(QuotePersistenceException.class) // Inner wrapper
                                .hasMessageContaining("Database fallback failed during count or fetch: " + dbCountError.getMessage());

                        assertThat(error.getCause().getCause()) // Original cause
                                .isSameAs(dbCountError);
                    })
                    .verify();

            // Assert: Verify interactions. Providers tried, DB count called.
            verify(provider1).fetchQuotes();
            verify(provider2).fetchQuotes();
            verify(provider2).getProviderName(); // For logging recoverable error
            verify(quoteRepository).count();
            verify(quoteRepository, never()).findAllQuotes(anyInt());
            verify(quoteRepository, never()).findRandomQuotes(anyInt());
        }

        @Test
        @DisplayName("10. Fallback Error: Should propagate error when findRandomQuotes fails")
        void getQuotes_whenAllProvidersFail_andFindRandomFails_shouldPropagateError() {
            // Arrange: Mock DB count high. Mock findRandomQuotes to fail.
            when(quoteRepository.count()).thenReturn(Mono.just(100L)); // Count >= Threshold
            QuotePersistenceException dbFetchError = new QuotePersistenceException("DB random fetch failed", null);
            when(quoteRepository.findRandomQuotes(FALLBACK_RANDOM_QUOTE_AMOUNT)).thenReturn(Mono.error(dbFetchError));

            // Act
            Mono<Optional<List<Quote>>> resultMono = quoteFetchOrchestrator.getQuotes();

            // Assert: Expect error wrapped twice (similar to count failure).
            StepVerifier.create(resultMono)
                    .expectErrorSatisfies(error -> {
                        assertThat(error)
                                .isInstanceOf(QuoteFetchOrchestratorException.class)
                                .hasMessageContaining("CRITICAL error failed to get quotes: Database fallback failed"); // Outer wrapper

                        assertThat(error.getCause())
                                .isInstanceOf(QuotePersistenceException.class) // Inner wrapper
                                .hasMessageContaining("Database fallback failed during count or fetch: " + dbFetchError.getMessage());

                        assertThat(error.getCause().getCause()) // Original cause
                                .isSameAs(dbFetchError);
                    })
                    .verify();

            // Assert: Verify interactions. Providers tried, count called, findRandom called.
            verify(provider1).fetchQuotes();
            verify(provider2).fetchQuotes();
            verify(provider2).getProviderName(); // For logging recoverable error
            verify(quoteRepository).count();
            verify(quoteRepository).findRandomQuotes(FALLBACK_RANDOM_QUOTE_AMOUNT);
            verify(quoteRepository, never()).findAllQuotes(anyInt());
        }

        @Test
        @DisplayName("11. Fallback Error: Should propagate error when findAllQuotes fails")
        void getQuotes_whenAllProvidersFail_andFindAllFails_shouldPropagateError() {
            // Arrange: Mock DB count low. Mock findAllQuotes to fail with Flux.error.
            when(quoteRepository.count()).thenReturn(Mono.just(10L)); // Count < Threshold
            QuotePersistenceException dbFetchError = new QuotePersistenceException("DB find all failed", null);
            when(quoteRepository.findAllQuotes(FALLBACK_RANDOM_QUOTE_AMOUNT)).thenReturn(Flux.error(dbFetchError));

            // Act
            Mono<Optional<List<Quote>>> resultMono = quoteFetchOrchestrator.getQuotes();

            // Assert: Expect error wrapped twice (similar to count/findRandom failure).
            StepVerifier.create(resultMono)
                    .expectErrorSatisfies(error -> {
                        assertThat(error)
                                .isInstanceOf(QuoteFetchOrchestratorException.class)
                                .hasMessageContaining("CRITICAL error failed to get quotes: Database fallback failed"); // Outer wrapper

                        assertThat(error.getCause())
                                .isInstanceOf(QuotePersistenceException.class) // Inner wrapper
                                .hasMessageContaining("Database fallback failed during count or fetch: " + dbFetchError.getMessage());

                        assertThat(error.getCause().getCause()) // Original cause
                                .isSameAs(dbFetchError);
                    })
                    .verify();

            // Assert: Verify interactions. Providers tried, count called, findAll called.
            verify(provider1).fetchQuotes();
            verify(provider2).fetchQuotes();
            verify(provider2).getProviderName(); // For logging recoverable error
            verify(quoteRepository).count();
            verify(quoteRepository).findAllQuotes(FALLBACK_RANDOM_QUOTE_AMOUNT);
            verify(quoteRepository, never()).findRandomQuotes(anyInt());
        }
    } // End of DbFallbackScenarios Nested Class

    @Nested
    @DisplayName("getQuotes() - Edge Cases")
    class EdgeCaseScenarios {

        @Test
        @DisplayName("12. No Providers: Should fallback to DB immediately")
        void getQuotes_whenNoProvidersConfigured_shouldFallbackToDb() {
            // Arrange: Initialize orchestrator with an empty provider list.
            initializeOrchestrator(Collections.emptyList());
            // Arrange: Mock DB fallback (using findRandomQuotes path as an example)
            when(quoteRepository.count()).thenReturn(Mono.just(100L)); // Count >= Threshold
            when(quoteRepository.findRandomQuotes(FALLBACK_RANDOM_QUOTE_AMOUNT))
                    .thenReturn(Mono.just(Optional.of(dbQuotes)));

            // Act
            Mono<Optional<List<Quote>>> resultMono = quoteFetchOrchestrator.getQuotes();

            // Assert: Verify the result comes from the DB fallback.
            StepVerifier.create(resultMono)
                    .expectNext(Optional.of(dbQuotes))
                    .verifyComplete();

            // Assert: Verify interactions. No providers called, only DB fallback methods.
            verifyNoInteractions(provider1, provider2); // Ensure no provider mocks were touched
            verify(quoteRepository).count();
            verify(quoteRepository).findRandomQuotes(FALLBACK_RANDOM_QUOTE_AMOUNT);
            verify(quoteRepository, never()).findAllQuotes(anyInt());
        }

        @Test
        @DisplayName("13. Providers succeed but return Optional<null> (Bad Practice but test resilience)")
        void getQuotes_whenProviderReturnsOptionalNull_shouldTreatAsEmptyAndContinue() {
            // Arrange: Provider1 returns Optional containing null (should be avoided in real code)
            // Provider2 returns valid data.
            // Note: Mono.just(Optional.ofNullable(null)) is equivalent to Mono.just(Optional.empty())
            initializeOrchestrator(List.of(provider1, provider2));
            
            // Simulate provider returning Mono containing an Optional containing null
            when(provider1.fetchQuotes()).thenReturn(Mono.just(Optional.ofNullable(null))); // This is just Optional.empty()
            when(provider2.fetchQuotes()).thenReturn(Mono.just(Optional.of(provider2Quotes)));


            // Act
            Mono<Optional<List<Quote>>> resultMono = quoteFetchOrchestrator.getQuotes();

            // Assert: filter(Optional::isPresent) correctly handles Optional.empty(), proceeds to provider2
            StepVerifier.create(resultMono)
                    .expectNext(Optional.of(provider2Quotes))
                    .verifyComplete();

            // Assert: Both providers were called
            verify(provider1).fetchQuotes();
            verify(provider2).fetchQuotes();
            verifyNoInteractions(quoteRepository);
        }
    }
}