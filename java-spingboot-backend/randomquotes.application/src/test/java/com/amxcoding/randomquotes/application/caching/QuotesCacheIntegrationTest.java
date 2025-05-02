//package com.amxcoding.randomquotes.application.caching;
//
//import com.amxcoding.randomquotes.application.caching.config.QuotesCacheConfig;
//import com.amxcoding.randomquotes.application.common.Constants;
//import com.amxcoding.randomquotes.application.exceptions.providers.QuoteProviderException;
//import com.amxcoding.randomquotes.application.interfaces.caching.IQuotesCache;
//import com.amxcoding.randomquotes.application.interfaces.providers.IQuoteProvider;
//import com.amxcoding.randomquotes.domain.entities.Quote;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.cache.Cache;
//import org.springframework.cache.CacheManager;
//import org.springframework.test.context.bean.override.mockito.MockitoBean;
//import org.springframework.test.context.junit.jupiter.SpringExtension;
//
//import java.time.Duration;
//import java.util.List;
//import java.util.Optional;
//import java.util.concurrent.CompletableFuture;
//import java.util.concurrent.ExecutionException;
//import java.util.concurrent.TimeoutException;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.mockito.Mockito.*;
//
//
//
//@ExtendWith(SpringExtension.class)
//@SpringBootTest(classes = QuotesCacheConfig.class)
//public class QuotesCacheIntegrationTest {
//
//    @Autowired
//    private IQuotesCache quotesCache;
//
//    @Autowired
//    private CacheManager cacheManager;
//
//    @MockitoBean
//    @Qualifier("zenquotes")
//    private IQuoteProvider mockQuoteProvider;
//
//    private final String CACHE_NAME = Constants.Cache.QUOTES_CACHE;
//    private final String CACHE_KEY = Constants.Cache.RANDOM_QUOTES_KEY;
//
//    @BeforeEach
//    void clearCache() {
//        Cache cache = cacheManager.getCache(CACHE_NAME);
//        if (cache != null) {
//            cache.clear();
//        } else {
//            System.err.println("Warning: Cache '" + CACHE_NAME + "' not found during setup.");
//        }
//
//        reset(mockQuoteProvider);
//    }
//
//    @Test
//    @DisplayName("getQuotes should fetch from provider and cache result on first call, then return cached result on second call")
//    void getQuotes_shouldCacheResult() throws ExecutionException, InterruptedException {
//        // Arrange
//        List<Quote> quotesList = List.of(new Quote(1L, "Cached Text", "Cached Author"));
//        CompletableFuture<Optional<List<Quote>>> successfulFuture =
//                CompletableFuture.completedFuture(Optional.of(quotesList));
//        when(mockQuoteProvider.fetchQuotes()).thenReturn(successfulFuture);
//
//        // Act 1: First call -> result will be cached and returned
//        CompletableFuture<Optional<List<Quote>>> future1 = quotesCache.getQuotes();
//        Optional<List<Quote>> result1 = future1.get();
//
//        // Assert 1
//        assertThat(result1).isPresent().contains(quotesList);
//        verify(mockQuoteProvider, times(1)).fetchQuotes(); // Verify provider was called once
//
//        // Act 2: Second call -> cached items will be returned
//        CompletableFuture<Optional<List<Quote>>> future2 = quotesCache.getQuotes();
//        Optional<List<Quote>> result2 = future2.get();
//
//        // Assert 2: Provider should have not been called
//        assertThat(result2).isPresent().contains(quotesList);
//        verifyNoMoreInteractions(mockQuoteProvider);
//
//        // Assert 3: Verify the cache contains the completed future's result
//        Cache cache = cacheManager.getCache(CACHE_NAME);
//        assertThat(cache).isNotNull();
//        Cache.ValueWrapper cachedValueWrapper = cache.get(CACHE_KEY);
//        assertThat(cachedValueWrapper).isNotNull();
//        assertThat(cachedValueWrapper.get()).isEqualTo(Optional.of(quotesList));
//    }
//
//    @Test
//    @DisplayName("getQuotes should not cache result when provider throws exception")
//    void getQuotes_shouldNotCacheOnError() throws InterruptedException, ExecutionException {
//        // Arrange 1: Provider fails on the first call
//        QuoteProviderException providerException = new QuoteProviderException("Provider unavailable");
//        CompletableFuture<Optional<List<Quote>>> failedFuture = CompletableFuture.failedFuture(providerException);
//        when(mockQuoteProvider.fetchQuotes()).thenReturn(failedFuture);
//
//        // Act 1: First call -> fails, should not cache
//        CompletableFuture<Optional<List<Quote>>> future1 = quotesCache.getQuotes();
//
//        // Assert 1: Verify the future completes exceptionally
//        assertThat(future1)
//                .failsWithin(Duration.ofSeconds(1))
//                .withThrowableOfType(ExecutionException.class)
//                .withCause(providerException);
//        verify(mockQuoteProvider, times(1)).fetchQuotes();
//
//        // Assert 1 Cache: Ensure nothing was cached
//        Cache cache = cacheManager.getCache(CACHE_NAME);
//        assertThat(cache).isNotNull();
//        assertThat(cache.get(CACHE_KEY)).isNull();
//
//        // Arrange 2: Reset the mock and arrange for the provider to succeed on the next attempt
//        reset(mockQuoteProvider);
//        List<Quote> quotesList = List.of(new Quote(2L, "Success Text", "Success Author"));
//        CompletableFuture<Optional<List<Quote>>> successfulFuture =
//                CompletableFuture.completedFuture(Optional.of(quotesList));
//        when(mockQuoteProvider.fetchQuotes()).thenReturn(successfulFuture);
//
//        // Act 2: Second call -> should now call the provider and cache the successful result
//        CompletableFuture<Optional<List<Quote>>> future2 = quotesCache.getQuotes();
//        Optional<List<Quote>> result2;
//        try {
//            result2 = future2.get(5, java.util.concurrent.TimeUnit.SECONDS);
//        } catch (TimeoutException e) {
//            throw new RuntimeException("Second call to getQuotes failed unexpectedly", e);
//        }
//
//        // Assert 2
//        assertThat(result2).isPresent();
//        assertThat(result2.get()).isEqualTo(quotesList);
//        verify(mockQuoteProvider, times(1)).fetchQuotes();
//
//        // Assert 2 Cache: Ensure result is now cached
//        Cache.ValueWrapper cachedValueWrapper = cache.get(CACHE_KEY);
//        assertThat(cachedValueWrapper).isNotNull();
//        assertThat(cachedValueWrapper.get()).isEqualTo(Optional.of(quotesList));
//    }
//}