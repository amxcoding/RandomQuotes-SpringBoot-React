package com.amxcoding.randomquotes.application.services;

import com.amxcoding.randomquotes.application.exceptions.repositories.QuotePersistenceException;
import com.amxcoding.randomquotes.application.interfaces.caching.IQuotesCache;
import com.amxcoding.randomquotes.application.interfaces.repositories.IQuoteRepository;
import com.amxcoding.randomquotes.domain.entities.Quote;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("QuoteService Unit Tests")
class QuoteServiceTest {

    @Mock
    private IQuotesCache quotesCache;

    @Mock
    private IQuoteRepository quoteRepository;

    @InjectMocks
    private QuoteService quoteService;

    private Quote quote1;
    private Quote quote2;
    private List<Quote> quoteList;

    @BeforeEach
    void setUp() {
        quote1 = new Quote(1L, "Author One", "Text One", 5);
        quote2 = new Quote(2L, "Author Two", "Text Two", 10);
        quoteList = List.of(quote1, quote2);
    }

    // --- Tests for getRandomQuote ---
    @Nested
    @DisplayName("getRandomQuote()")
    class GetRandomQuoteTests {

        @Test
        @DisplayName("should return quote from repository when cache has quotes and repository finds by hash")
        void getRandomQuote_cacheHit_repoHit() {
            // Arrange: Mock cache to return a list of quotes and repository to find a quote by hash.
            // Since the service picks a random quote, we mock the repo to return a valid quote for *any* hash lookup
            // derived from the cached list. Verification will check if the hash used was valid.
            Optional<List<Quote>> cachedOptionalList = Optional.of(quoteList);
            Optional<Quote> repoResult = Optional.of(quote1); // Assume repo returns quote1 when found by hash

            when(quotesCache.getQuotes()).thenReturn(Mono.just(cachedOptionalList));
            when(quoteRepository.findByTextAuthorHash(anyString())).thenReturn(Mono.just(repoResult));

            // Act & Assert: Verify the service returns the quote found by the repository.
            StepVerifier.create(quoteService.getRandomQuote())
                    .expectNext(repoResult)
                    .verifyComplete();

            // Verify interactions: Ensure cache and repository were called. Capture and validate the hash used for repo lookup.
            verify(quotesCache, times(1)).getQuotes();
            ArgumentCaptor<String> hashCaptor = ArgumentCaptor.forClass(String.class);
            verify(quoteRepository, times(1)).findByTextAuthorHash(hashCaptor.capture());
            assertThat(hashCaptor.getValue()).isIn(quote1.generateTextAuthorHash(), quote2.generateTextAuthorHash());
        }

        @Test
        @DisplayName("should return empty Optional when cache has quotes but repository doesn't find by hash")
        void getRandomQuote_cacheHit_repoMiss() {
            // Arrange: Mock cache returning quotes, but repository returning empty Optional for the hash lookup.
            Optional<List<Quote>> cachedOptionalList = Optional.of(quoteList);
            Optional<Quote> repoResult = Optional.empty(); // Repo returns empty

            when(quotesCache.getQuotes()).thenReturn(Mono.just(cachedOptionalList));
            when(quoteRepository.findByTextAuthorHash(anyString())).thenReturn(Mono.just(repoResult));

            // Act & Assert: Verify the service returns an empty Optional.
            StepVerifier.create(quoteService.getRandomQuote())
                    .expectNext(Optional.empty())
                    .verifyComplete();

            // Verify interactions: Ensure both cache and repository were consulted.
            verify(quotesCache, times(1)).getQuotes();
            verify(quoteRepository, times(1)).findByTextAuthorHash(anyString());
        }

        @Test
        @DisplayName("should return empty Optional when cache returns empty Optional")
        void getRandomQuote_cacheMiss_emptyOptional() {
            // Arrange: Mock cache to return an empty Optional (no quotes available).
            when(quotesCache.getQuotes()).thenReturn(Mono.just(Optional.empty()));

            // Act & Assert: Verify the service returns an empty Optional directly.
            StepVerifier.create(quoteService.getRandomQuote())
                    .expectNext(Optional.empty())
                    .verifyComplete();

            // Verify interactions: Ensure cache was called, but repository was not (no hashes to look up).
            verify(quotesCache, times(1)).getQuotes();
            verifyNoInteractions(quoteRepository);
        }

        @Test
        @DisplayName("should return empty Optional when cache returns Optional with empty list")
        void getRandomQuote_cacheMiss_emptyList() {
            // Arrange: Mock cache to return an Optional containing an empty list.
            when(quotesCache.getQuotes()).thenReturn(Mono.just(Optional.of(Collections.emptyList())));

            // Act & Assert: Verify the service returns an empty Optional.
            StepVerifier.create(quoteService.getRandomQuote())
                    .expectNext(Optional.empty())
                    .verifyComplete();

            // Verify interactions: Ensure cache was called, but repository was not (no hashes to look up).
            verify(quotesCache, times(1)).getQuotes();
            verifyNoInteractions(quoteRepository);
        }

        @Test
        @DisplayName("should propagate error when cache returns error")
        void getRandomQuote_cacheError() {
            // Arrange: Mock cache to return an error during quote retrieval.
            RuntimeException cacheError = new RuntimeException("Cache failure");
            when(quotesCache.getQuotes()).thenReturn(Mono.error(cacheError));

            // Act & Assert: Verify the exact error from the cache is propagated.
            StepVerifier.create(quoteService.getRandomQuote())
                    .expectErrorMatches(error -> error == cacheError)
                    .verify();

            // Verify interactions: Ensure cache was called, but repository was not.
            verify(quotesCache, times(1)).getQuotes();
            verifyNoInteractions(quoteRepository);
        }

        @Test
        @DisplayName("should propagate error when repository returns error during hash lookup")
        void getRandomQuote_repoError() {
            // Arrange: Mock cache success, but repository to return an error during hash lookup.
            Optional<List<Quote>> cachedOptionalList = Optional.of(quoteList);
            RuntimeException repoError = new RuntimeException("Repository failure");

            when(quotesCache.getQuotes()).thenReturn(Mono.just(cachedOptionalList));
            when(quoteRepository.findByTextAuthorHash(anyString())).thenReturn(Mono.error(repoError));

            // Act & Assert: Verify the exact error from the repository is propagated.
            StepVerifier.create(quoteService.getRandomQuote())
                    .expectErrorMatches(error -> error == repoError)
                    .verify();

            // Verify interactions: Ensure both cache and repository were called.
            verify(quotesCache, times(1)).getQuotes();
            verify(quoteRepository, times(1)).findByTextAuthorHash(anyString());
        }

        @Test
        @DisplayName("should throw IllegalStateException when a randomly selected cached quote generates an empty hash")
        void getRandomQuote_emptyHash() {
            // Arrange: Mock cache to return a list containing a quote that generates an empty hash.
            // This simulates invalid quote data being present in the cache.
            Quote quoteWithEmptyHash = mock(Quote.class); // Use mock to precisely control hash generation
            when(quoteWithEmptyHash.generateTextAuthorHash()).thenReturn(""); // Force empty hash generation
            Optional<List<Quote>> cachedOptionalList = Optional.of(List.of(quoteWithEmptyHash));

            when(quotesCache.getQuotes()).thenReturn(Mono.just(cachedOptionalList));

            // Act & Assert: Verify that an IllegalStateException is thrown due to the missing hash.
            StepVerifier.create(quoteService.getRandomQuote())
                    .expectErrorSatisfies(error -> {
                        assertThat(error).isInstanceOf(IllegalStateException.class)
                                .hasMessage("Quotes hash is missing");
                    })
                    .verify();

            // Verify interactions: Cache was called, hash generation was attempted, but repository lookup was skipped.
            verify(quotesCache, times(1)).getQuotes();
            verify(quoteWithEmptyHash, times(1)).generateTextAuthorHash();
            verifyNoInteractions(quoteRepository);
        }
    }

    // --- Tests for getQuoteById ---
    @Nested
    @DisplayName("getQuoteById()")
    class GetQuoteByIdTests {

        @Test
        @DisplayName("should return quote Optional from repository when found by ID")
        void getQuoteById_found() {
            // Arrange: Mock repository to return a specific quote wrapped in Optional for the given ID.
            Long quoteId = 1L;
            Optional<Quote> repoResult = Optional.of(quote1);
            when(quoteRepository.findById(quoteId)).thenReturn(Mono.just(repoResult));

            // Act & Assert: Verify the service returns the Optional containing the found quote.
            StepVerifier.create(quoteService.getQuoteById(quoteId))
                    .expectNext(repoResult)
                    .verifyComplete();

            // Verify interaction: Ensure repository's findById was called exactly once with the correct ID.
            verify(quoteRepository, times(1)).findById(quoteId);
        }

        @Test
        @DisplayName("should return empty Optional when repository does not find quote by ID")
        void getQuoteById_notFound() {
            // Arrange: Mock repository to return an empty Optional for the given ID.
            Long quoteId = 999L;
            when(quoteRepository.findById(quoteId)).thenReturn(Mono.just(Optional.empty()));

            // Act & Assert: Verify the service returns an empty Optional.
            StepVerifier.create(quoteService.getQuoteById(quoteId))
                    .expectNext(Optional.empty())
                    .verifyComplete();

            // Verify interaction: Ensure repository's findById was called.
            verify(quoteRepository, times(1)).findById(quoteId);
        }

        @Test
        @DisplayName("should propagate error when repository findById fails")
        void getQuoteById_repoError() {
            // Arrange: Mock repository findById operation to return an error.
            Long quoteId = 1L;
            RuntimeException repoError = new RuntimeException("DB error on findById");
            when(quoteRepository.findById(quoteId)).thenReturn(Mono.error(repoError));

            // Act & Assert: Verify the exact error from the repository is propagated.
            StepVerifier.create(quoteService.getQuoteById(quoteId))
                    .expectErrorMatches(error -> error == repoError)
                    .verify();

            // Verify interaction: Ensure repository's findById was called.
            verify(quoteRepository, times(1)).findById(quoteId);
        }
    }

    // --- Tests for updateQuote ---
    @Nested
    @DisplayName("updateQuote()")
    class UpdateQuoteTests {

        @Test
        @DisplayName("should save quote via repository and return the saved quote when ID is present")
        void updateQuote_success() {
            // Arrange: Mock repository saveQuote operation to successfully return the (potentially updated) quote object.
            Quote quoteToUpdate = new Quote(1L, "Updated Author", "Updated Text", 15);
            when(quoteRepository.saveQuote(quoteToUpdate)).thenReturn(Mono.just(quoteToUpdate));

            // Act & Assert: Verify the service returns the quote provided by the repository's save method.
            StepVerifier.create(quoteService.updateQuote(quoteToUpdate))
                    .expectNext(quoteToUpdate)
                    .verifyComplete();

            // Verify interaction: Ensure repository's saveQuote was called with the correct quote object.
            verify(quoteRepository, times(1)).saveQuote(quoteToUpdate);
        }

        @Test
        @DisplayName("should return QuotePersistenceException when attempting to update a quote with a null ID")
        void updateQuote_nullId() {
            // Arrange: Create a quote object with a null ID.
            Quote quoteWithNullId = new Quote(null, "New Author", "New Text", 0);

            // Act & Assert: Verify that a QuotePersistenceException is thrown with a specific message.
            StepVerifier.create(quoteService.updateQuote(quoteWithNullId))
                    .expectErrorSatisfies(error -> {
                        assertThat(error).isInstanceOf(QuotePersistenceException.class)
                                .hasMessage("Quote ID must not be null for update");
                    })
                    .verify();

            // Verify interaction: Ensure the repository's save method was never called.
            verifyNoInteractions(quoteRepository);
        }

        @Test
        @DisplayName("should propagate error when repository saveQuote fails")
        void updateQuote_repoError() {
            // Arrange: Mock repository saveQuote operation to return an error.
            Quote quoteToUpdate = new Quote(1L, "Author", "Text", 5);
            RuntimeException repoError = new RuntimeException("DB error on save");
            when(quoteRepository.saveQuote(quoteToUpdate)).thenReturn(Mono.error(repoError));

            // Act & Assert: Verify the exact error from the repository is propagated.
            StepVerifier.create(quoteService.updateQuote(quoteToUpdate))
                    .expectErrorMatches(error -> error == repoError)
                    .verify();

            // Verify interaction: Ensure repository's saveQuote was called.
            verify(quoteRepository, times(1)).saveQuote(quoteToUpdate);
        }
    }
}