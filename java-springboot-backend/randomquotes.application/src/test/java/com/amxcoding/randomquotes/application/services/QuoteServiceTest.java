package com.amxcoding.randomquotes.application.services;

import com.amxcoding.randomquotes.application.exceptions.repositories.QuotePersistenceException;
import com.amxcoding.randomquotes.application.interfaces.repositories.IQuoteRepository;
import com.amxcoding.randomquotes.application.interfaces.services.IQuoteCache;
import com.amxcoding.randomquotes.domain.entities.Quote;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("QuoteService Unit Tests")
class QuoteServiceTest {

    @Mock
    private IQuoteCache quotesCache;

    @Mock
    private IQuoteRepository quoteRepository;

    @InjectMocks
    private QuoteService quoteService;

    private Quote quoteWithId;
    private Quote quoteWithoutId;
    private List<Quote> quoteListWithId;
    private List<Quote> quoteListWithoutId;
    private List<Quote> mixedQuoteList;

    @BeforeEach
    void setUp() {
        quoteWithId = new Quote(1L, "Author Persisted", "Text Persisted", 5);
        quoteWithoutId = new Quote(null, "Author Fresh", "Text Fresh"); // Use null ID explicitly
        quoteListWithId = List.of(quoteWithId);
        quoteListWithoutId = List.of(quoteWithoutId);
        mixedQuoteList = List.of(quoteWithId, quoteWithoutId); // Example with both types
    }


    @Nested
    @DisplayName("getRandomQuote() Unit Tests")
    class GetRandomQuoteTests {

        @Test
        @DisplayName("1. Cache Hit: Should return quote directly from cache if ID is present")
        void getRandomQuote_whenCacheHit_andQuoteHasId_shouldReturnCachedQuote() {
            // Arrange: Mock cache returns a list containing one quote *with* an ID.
            // FIX: Return Mono<List<Quote>> instead of Mono<Optional<List<Quote>>>
            when(quotesCache.getQuotes()).thenReturn(Mono.just(quoteListWithId));

            // Act: Call the service method.
            Mono<Optional<Quote>> resultMono = quoteService.getRandomQuote();

            // Assert: Verify the exact quote with ID is returned (wrapped in Optional by the service).
            StepVerifier.create(resultMono)
                    .expectNext(Optional.of(quoteWithId)) // Service signature returns Optional
                    .verifyComplete();

            // Assert: Verify cache was checked, repository was not.
            verify(quotesCache).getQuotes();
            verifyNoInteractions(quoteRepository);
        }

        @Test
        @DisplayName("2. Cache Hit & Repo Hit: Should return quote from repository when cached quote has null ID")
        void getRandomQuote_whenCacheHit_andQuoteHasNullId_andRepoFinds_shouldReturnRepoQuote() {
            // Arrange: Mock cache returns a list containing one quote *without* an ID.
            // FIX: Return Mono<List<Quote>>
            when(quotesCache.getQuotes()).thenReturn(Mono.just(quoteListWithoutId));

            // Arrange: Calculate hash for the quote without ID.
            String expectedHash = quoteWithoutId.generateTextAuthorHash();
            // Arrange: Mock repository returning a potentially updated quote (with ID, likes) for that hash.
            Quote quoteFromRepo = new Quote(5L, quoteWithoutId.getAuthor(), quoteWithoutId.getText(), 3);
            when(quoteRepository.findByTextAuthorHash(expectedHash)).thenReturn(Mono.just(Optional.of(quoteFromRepo)));

            // Act: Call the service method.
            Mono<Optional<Quote>> resultMono = quoteService.getRandomQuote();

            // Assert: Verify the quote returned is the one from the repository lookup.
            StepVerifier.create(resultMono)
                    .expectNext(Optional.of(quoteFromRepo)) // Service signature returns Optional
                    .verifyComplete();

            // Assert: Verify cache and repository were called with the correct hash.
            verify(quotesCache).getQuotes();
            verify(quoteRepository).findByTextAuthorHash(expectedHash);
        }

        @Test
        @DisplayName("3. Cache Hit & Repo Miss: Should return empty Optional when cached quote has null ID and repo doesn't find hash")
        void getRandomQuote_whenCacheHit_andQuoteHasNullId_andRepoMisses_shouldReturnEmpty() {
            // Arrange: Mock cache returns a list containing one quote *without* an ID.
            // FIX: Return Mono<List<Quote>>
            when(quotesCache.getQuotes()).thenReturn(Mono.just(quoteListWithoutId));

            // Arrange: Calculate hash.
            String expectedHash = quoteWithoutId.generateTextAuthorHash();
            // Arrange: Mock repository returning empty for the hash lookup.
            when(quoteRepository.findByTextAuthorHash(expectedHash)).thenReturn(Mono.just(Optional.empty()));

            // Act: Call the service method.
            Mono<Optional<Quote>> resultMono = quoteService.getRandomQuote();

            // Assert: Verify the final result is an empty Optional.
            StepVerifier.create(resultMono)
                    .expectNext(Optional.empty()) // Service signature returns Optional
                    .verifyComplete();

            // Assert: Verify cache and repository were called.
            verify(quotesCache).getQuotes();
            verify(quoteRepository).findByTextAuthorHash(expectedHash);
        }

        @Test
        @DisplayName("4. Cache Miss (Empty List): Should return empty Optional")
            // Test name adjusted: Cache miss means empty list according to new contract
        void getRandomQuote_whenCacheMiss_emptyList_shouldReturnEmpty() {
            // Arrange: Mock cache to return an empty list.
            // FIX: Return Mono<List<Quote>> (empty list) instead of Mono<Optional.empty()>
            when(quotesCache.getQuotes()).thenReturn(Mono.just(Collections.emptyList()));

            // Act: Call the service method.
            Mono<Optional<Quote>> resultMono = quoteService.getRandomQuote();

            // Assert: Verify the service returns an empty Optional.
            StepVerifier.create(resultMono)
                    .expectNext(Optional.empty()) // Service signature returns Optional
                    .verifyComplete();

            // Assert: Verify cache called, repository not called.
            verify(quotesCache).getQuotes();
            verifyNoInteractions(quoteRepository);
        }

        // Test 5 becomes redundant as it's the same case as Test 4 now.
        // @Test
        // @DisplayName("5. Cache Miss (Optional<Empty List>): Should return empty Optional")
        // void getRandomQuote_whenCacheMiss_optionalEmptyList_shouldReturnEmpty() { ... }


        @Test
        @DisplayName("6. Cache Error: Should propagate error when cache fails")
        void getRandomQuote_whenCacheError_shouldPropagateError() {
            // Arrange: Mock cache getQuotes() to return an error.
            RuntimeException cacheError = new RuntimeException("Cache unavailable");
            when(quotesCache.getQuotes()).thenReturn(Mono.error(cacheError));

            // Act: Call the service method.
            Mono<Optional<Quote>> resultMono = quoteService.getRandomQuote();

            // Assert: Verify the exact error from the cache is propagated.
            StepVerifier.create(resultMono)
                    .expectErrorMatches(error -> error == cacheError) // Check specific error instance
                    .verify();

            // Assert: Verify cache called, repository not called.
            verify(quotesCache).getQuotes();
            verifyNoInteractions(quoteRepository);
        }

        @Test
        @DisplayName("7. Repository Error: Should propagate error when repository fails during hash lookup")
        void getRandomQuote_whenRepoErrorOnHashLookup_shouldPropagateError() {
            // Arrange: Mock cache returns quote without ID.
            // FIX: Return Mono<List<Quote>>
            when(quotesCache.getQuotes()).thenReturn(Mono.just(quoteListWithoutId));
            String expectedHash = quoteWithoutId.generateTextAuthorHash();
            // Arrange: Mock repository findByHash to return an error.
            QuotePersistenceException repoError = new QuotePersistenceException("DB error on findByHash", null);
            when(quoteRepository.findByTextAuthorHash(expectedHash)).thenReturn(Mono.error(repoError));

            // Act: Call the service method.
            Mono<Optional<Quote>> resultMono = quoteService.getRandomQuote();

            // Assert: Verify the exact error from the repository is propagated.
            StepVerifier.create(resultMono)
                    .expectErrorMatches(error -> error == repoError) // Check specific error instance
                    .verify();

            // Assert: Verify cache and repository were called.
            verify(quotesCache).getQuotes();
            verify(quoteRepository).findByTextAuthorHash(expectedHash);
        }

        @Test
        @DisplayName("8. Empty Hash: Should throw IllegalStateException when generated hash is empty")
        void getRandomQuote_whenEmptyHashGenerated_shouldThrowIllegalState() {
            // Arrange: Mock a quote that generates an empty hash.
            Quote quoteWithEmptyHash = mock(Quote.class);
            when(quoteWithEmptyHash.getId()).thenReturn(null); // Ensure ID is null
            when(quoteWithEmptyHash.generateTextAuthorHash()).thenReturn(""); // Force empty hash
            // Arrange: Mock cache to return this quote.
            // FIX: Return Mono<List<Quote>>
            when(quotesCache.getQuotes()).thenReturn(Mono.just(List.of(quoteWithEmptyHash)));

            // Act: Call the service method.
            Mono<Optional<Quote>> resultMono = quoteService.getRandomQuote();

            // Assert: Verify an IllegalStateException is thrown.
            StepVerifier.create(resultMono)
                    .expectErrorSatisfies(error -> {
                        assertThat(error)
                                .isInstanceOf(IllegalStateException.class)
                                .hasMessage("Quotes hash is missing");
                    })
                    .verify();

            // Assert: Verify interactions.
            verify(quotesCache).getQuotes();
            verify(quoteWithEmptyHash).generateTextAuthorHash();
            verifyNoInteractions(quoteRepository); // Repository not called because of empty hash
        }
    }


    @Nested
    @DisplayName("getQuoteById() Unit Tests")
    class GetQuoteByIdTests {

        @Test
        @DisplayName("1. Should return quote Optional from repository when found by ID")
        void getQuoteById_whenFound_shouldReturnOptionalWithQuote() {
            // Arrange: Define an ID and mock the repository to return a specific quote.
            Long quoteId = 1L;
            Optional<Quote> repoResult = Optional.of(quoteWithId); // Use pre-defined quote with ID=1L
            when(quoteRepository.findById(quoteId)).thenReturn(Mono.just(repoResult));

            // Act: Call the service method.
            Mono<Optional<Quote>> resultMono = quoteService.getQuoteById(quoteId);

            // Assert: Verify the service returns the expected Optional<Quote>.
            StepVerifier.create(resultMono)
                    .expectNext(repoResult)
                    .verifyComplete();

            // Assert: Verify repository interaction.
            verify(quoteRepository).findById(quoteId);
            verifyNoInteractions(quotesCache);
        }

        @Test
        @DisplayName("2. Should return empty Optional when repository does not find quote by ID")
        void getQuoteById_whenNotFound_shouldReturnEmptyOptional() {
            // Arrange: Define an ID and mock the repository to return empty Optional.
            Long nonExistentId = 999L;
            when(quoteRepository.findById(nonExistentId)).thenReturn(Mono.just(Optional.empty()));

            // Act: Call the service method.
            Mono<Optional<Quote>> resultMono = quoteService.getQuoteById(nonExistentId);

            // Assert: Verify the service returns empty Optional.
            StepVerifier.create(resultMono)
                    .expectNext(Optional.empty())
                    .verifyComplete();

            // Assert: Verify repository interaction.
            verify(quoteRepository).findById(nonExistentId);
            verifyNoInteractions(quotesCache);
        }

        @Test
        @DisplayName("3. Should propagate error when repository findById fails")
        void getQuoteById_whenRepoError_shouldPropagateError() {
            // Arrange: Define an ID and mock the repository findById to return an error.
            Long quoteId = 1L;
            QuotePersistenceException repoError = new QuotePersistenceException("DB error on findById", null);
            when(quoteRepository.findById(quoteId)).thenReturn(Mono.error(repoError));

            // Act: Call the service method.
            Mono<Optional<Quote>> resultMono = quoteService.getQuoteById(quoteId);

            // Assert: Verify the exact error from the repository is propagated.
            StepVerifier.create(resultMono)
                    .expectErrorMatches(error -> error == repoError) // Check specific error instance
                    .verify();

            // Assert: Verify repository interaction.
            verify(quoteRepository).findById(quoteId);
            verifyNoInteractions(quotesCache);
        }
    }
}