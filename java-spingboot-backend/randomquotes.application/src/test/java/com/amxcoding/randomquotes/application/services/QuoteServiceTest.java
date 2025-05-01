package com.amxcoding.randomquotes.application.services;

import com.amxcoding.randomquotes.application.exceptions.providers.QuoteProviderException;
import com.amxcoding.randomquotes.application.interfaces.caching.IQuotesCache;
import com.amxcoding.randomquotes.domain.entities.Quote;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuoteServiceTest {

    @Mock
    private IQuotesCache mockQuotesCache;

    @InjectMocks
    private QuoteService quoteService;

    private Quote quote1;
    private Quote quote2;

    @BeforeEach
    void testSetup() {
        quote1 = new Quote(1L, "Quote text 1", "Author 1");
        quote2 = new Quote(2L, "Quote text 2", "Author 2");
    }

    @Test
    @DisplayName("getRandomQuote should return a random quote from a list of two")
    void getRandomQuote_whenCacheReturnsListOfTwo_shouldReturnRandomQuoteFromList() {
        // Arrange
        List<Quote> quotesList = List.of(quote1, quote2);
        CompletableFuture<Optional<List<Quote>>> successfulFuture =
                CompletableFuture.completedFuture(Optional.of(quotesList));

        when(mockQuotesCache.getQuotes()).thenReturn(successfulFuture);

        // Act
        CompletableFuture<Optional<Quote>> randomQuoteFuture = quoteService.getRandomQuote();

        // Assert
        assertThat(randomQuoteFuture)
                .succeedsWithin(Duration.ofSeconds(1))
                .satisfies(optionalQuote -> {
                    assertThat(optionalQuote).isPresent();
                    assertThat(optionalQuote.get()).isIn(quotesList);
                });

        verify(mockQuotesCache, times(1)).getQuotes();
        verifyNoMoreInteractions(mockQuotesCache);
    }

    @Test
    @DisplayName("getRandomQuote should return the only quote when cache returns a list of one")
    void getRandomQuote_whenCacheReturnsListOfOne_shouldReturnThatQuote() {
        // Arrange
        List<Quote> quotesList = List.of(quote1);
        CompletableFuture<Optional<List<Quote>>> successfulFuture =
                CompletableFuture.completedFuture(Optional.of(quotesList));
        when(mockQuotesCache.getQuotes()).thenReturn(successfulFuture);

        // Act
        CompletableFuture<Optional<Quote>> randomQuoteFuture = quoteService.getRandomQuote();

        // Assert
        assertThat(randomQuoteFuture)
                .succeedsWithin(Duration.ofSeconds(1))
                .isEqualTo(Optional.of(quote1));

        verify(mockQuotesCache, times(1)).getQuotes();
        verifyNoMoreInteractions(mockQuotesCache);
    }

    @Test
    @DisplayName("getRandomQuote should return empty Optional when cache returns an empty list")
    void getRandomQuote_whenCacheReturnsEmptyList_shouldReturnEmptyOptional() {
        // Arrange
        CompletableFuture<Optional<List<Quote>>> successfulEmptyListFuture =
                CompletableFuture.completedFuture(Optional.of(Collections.emptyList()));
        when(mockQuotesCache.getQuotes()).thenReturn(successfulEmptyListFuture);

        // Act
        CompletableFuture<Optional<Quote>> randomQuoteFuture = quoteService.getRandomQuote();

        // Assert
        assertThat(randomQuoteFuture)
                .succeedsWithin(Duration.ofSeconds(1))
                .isEqualTo(Optional.empty());

        verify(mockQuotesCache, times(1)).getQuotes();
        verifyNoMoreInteractions(mockQuotesCache);
    }

    @Test
    @DisplayName("getRandomQuote should propagate failure when cache call fails")
    void getRandomQuote_whenCacheCallFails_shouldPropagateFailure() {
        // Arrange
        QuoteProviderException providerException = new QuoteProviderException("Cache lookup failed");
        CompletableFuture<Optional<List<Quote>>> failedFuture = CompletableFuture.failedFuture(providerException);
        when(mockQuotesCache.getQuotes()).thenReturn(failedFuture);

        // Act
        CompletableFuture<Optional<Quote>> randomQuoteFuture = quoteService.getRandomQuote();

        // Assert
        assertThat(randomQuoteFuture)
                .failsWithin(Duration.ofSeconds(1))
                .withThrowableOfType(ExecutionException.class)
                .withCause(providerException);

        verify(mockQuotesCache, times(1)).getQuotes();
        verifyNoMoreInteractions(mockQuotesCache);
    }
}