//package com.amxcoding.randomquotes.application.caching;
//
//import com.amxcoding.randomquotes.application.common.Constants;
//import com.amxcoding.randomquotes.application.exceptions.providers.QuoteProviderException;
//import com.amxcoding.randomquotes.application.interfaces.caching.IQuotesCache;
//import com.amxcoding.randomquotes.application.interfaces.providers.IQuoteProvider;
//import com.amxcoding.randomquotes.application.interfaces.repositories.IQuoteLikeRepository;
//import com.amxcoding.randomquotes.application.interfaces.repositories.IQuoteRepository;
//import com.amxcoding.randomquotes.domain.entities.Quote;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.cache.Cache;
//import org.springframework.cache.CacheManager;
//import org.springframework.test.context.ContextConfiguration;
//import org.springframework.test.context.bean.override.mockito.MockitoBean;
//import org.springframework.test.context.junit.jupiter.SpringExtension;
//import reactor.core.publisher.Mono;
//import reactor.test.StepVerifier;
//
//import java.util.Collections;
//import java.util.List;
//import java.util.Optional;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.mockito.ArgumentMatchers.anyList;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(SpringExtension.class)
////@SpringBootTest(classes = QuotesCacheConfig.class)
//@ContextConfiguration(classes = CacheTestConfig.class)
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
//    @MockitoBean
//    private IQuoteRepository mockQuoteRepository;
//
//    @MockitoBean
//    private IQuoteLikeRepository mockQuoteLikeRepository;
//
//    private final String CACHE_NAME = Constants.Cache.QUOTES_CACHE;
//    private final String CACHE_KEY = Constants.Cache.RANDOM_QUOTES_KEY;
//
//    @BeforeEach
//    void setupAndClearCache() {
//        Cache cache = cacheManager.getCache(CACHE_NAME);
//        if (cache != null) {
//            cache.clear();
//        } else {
//            System.err.println("Warning: Cache '" + CACHE_NAME + "' not found during setup.");
//        }
//        // Reset all mocks
//        reset(mockQuoteProvider, mockQuoteRepository, mockQuoteLikeRepository); // Add new mock here
//    }
//
//    @Test
//    @DisplayName("getQuotes should fetch from provider, save to repo, cache result on first call, then return cached result")
//    void getQuotes_shouldCacheResult() {
//        // Arrange
//        List<Quote> quotesList = List.of(new Quote(1L, "Cached Text", "Cached Author"));
//        Optional<List<Quote>> optionalQuotesList = Optional.of(quotesList);
//        when(mockQuoteProvider.fetchQuotes()).thenReturn(Mono.just(optionalQuotesList));
//        when(mockQuoteRepository.bulkInsertQuotesIgnoreConflicts(anyList())).thenReturn(Mono.empty());
//
//        // --- Act 1 & Assert 1: First call ---
//        StepVerifier.create(quotesCache.getQuotes())
//                .expectNext(optionalQuotesList)
//                .verifyComplete();
//
//        // Verify interactions
//        verify(mockQuoteProvider, times(1)).fetchQuotes();
//        verify(mockQuoteRepository, times(1)).bulkInsertQuotesIgnoreConflicts(quotesList);
//        verifyNoInteractions(mockQuoteLikeRepository); // Verify this mock wasn't used
//
//        // Verify cache state
//        Cache cache = cacheManager.getCache(CACHE_NAME);
//        assertThat(cache).isNotNull();
//        Cache.ValueWrapper cachedValueWrapper = cache.get(CACHE_KEY);
//        assertThat(cachedValueWrapper).isNotNull();
//        assertThat(cachedValueWrapper.get()).isEqualTo(optionalQuotesList);
//
//
//        // --- Act 2 & Assert 2: Second call ---
//        StepVerifier.create(quotesCache.getQuotes())
//                .expectNext(optionalQuotesList)
//                .verifyComplete();
//
//        // Verify NO MORE interactions
//        verifyNoMoreInteractions(mockQuoteProvider);
//        verifyNoMoreInteractions(mockQuoteRepository);
//        verifyNoInteractions(mockQuoteLikeRepository);
//    }
//
//    @Test
//    @DisplayName("getQuotes should not cache result when provider fetch throws exception")
//    void getQuotes_shouldNotCacheOnProviderError() {
//        // Arrange 1
//        QuoteProviderException providerException = new QuoteProviderException("Provider unavailable");
//        when(mockQuoteProvider.fetchQuotes()).thenReturn(Mono.error(providerException));
//
//        // --- Act 1 & Assert 1 ---
//        StepVerifier.create(quotesCache.getQuotes())
//                .expectErrorMatches(throwable -> throwable instanceof QuoteProviderException &&
//                        throwable.getMessage().equals("Provider unavailable"))
//                .verify();
//
//        // Verify interactions
//        verify(mockQuoteProvider, times(1)).fetchQuotes();
//        verifyNoInteractions(mockQuoteRepository);
//        verifyNoInteractions(mockQuoteLikeRepository);
//
//        // Assert Cache is empty
//        Cache cache = cacheManager.getCache(CACHE_NAME);
//        assertThat(cache).isNotNull();
//        assertThat(cache.get(CACHE_KEY)).isNull();
//
//
//        // --- Arrange 2 ---
//        reset(mockQuoteProvider, mockQuoteRepository, mockQuoteLikeRepository); // Reset all mocks
//        List<Quote> quotesList = List.of(new Quote(2L, "Success Text", "Success Author"));
//        Optional<List<Quote>> optionalQuotesList = Optional.of(quotesList);
//        when(mockQuoteProvider.fetchQuotes()).thenReturn(Mono.just(optionalQuotesList));
//        when(mockQuoteRepository.bulkInsertQuotesIgnoreConflicts(anyList())).thenReturn(Mono.empty());
//
//        // --- Act 2 & Assert 2 ---
//        StepVerifier.create(quotesCache.getQuotes())
//                .expectNext(optionalQuotesList)
//                .verifyComplete();
//
//        // Verify interactions
//        verify(mockQuoteProvider, times(1)).fetchQuotes();
//        verify(mockQuoteRepository, times(1)).bulkInsertQuotesIgnoreConflicts(quotesList);
//        verifyNoInteractions(mockQuoteLikeRepository);
//
//        // Assert Cache now contains the value
//        Cache.ValueWrapper cachedValueWrapper = cache.get(CACHE_KEY);
//        assertThat(cachedValueWrapper).isNotNull();
//        assertThat(cachedValueWrapper.get()).isEqualTo(optionalQuotesList);
//    }
//
//    @Test
//    @DisplayName("getQuotes should not cache result when repository save throws exception")
//    void getQuotes_shouldNotCacheOnRepositoryError() {
//        // Arrange 1
//        List<Quote> quotesList = List.of(new Quote(3L, "Repo Fail Text", "Repo Fail Author"));
//        Optional<List<Quote>> optionalQuotesList = Optional.of(quotesList);
//        RuntimeException repoException = new RuntimeException("DB connection failed");
//        when(mockQuoteProvider.fetchQuotes()).thenReturn(Mono.just(optionalQuotesList));
//        when(mockQuoteRepository.bulkInsertQuotesIgnoreConflicts(anyList())).thenReturn(Mono.error(repoException));
//
//        // --- Act 1 & Assert 1 ---
//        StepVerifier.create(quotesCache.getQuotes())
//                .expectErrorMatches(throwable -> throwable == repoException)
//                .verify();
//
//        // Verify interactions
//        verify(mockQuoteProvider, times(1)).fetchQuotes();
//        verify(mockQuoteRepository, times(1)).bulkInsertQuotesIgnoreConflicts(quotesList);
//        verifyNoInteractions(mockQuoteLikeRepository);
//
//        // Assert Cache is empty
//        Cache cache = cacheManager.getCache(CACHE_NAME);
//        assertThat(cache).isNotNull();
//        assertThat(cache.get(CACHE_KEY)).isNull();
//
//
//        // --- Arrange 2 ---
//        reset(mockQuoteProvider, mockQuoteRepository, mockQuoteLikeRepository); // Reset all mocks
//        when(mockQuoteProvider.fetchQuotes()).thenReturn(Mono.just(optionalQuotesList));
//        when(mockQuoteRepository.bulkInsertQuotesIgnoreConflicts(anyList())).thenReturn(Mono.empty());
//
//        // --- Act 2 & Assert 2 ---
//        StepVerifier.create(quotesCache.getQuotes())
//                .expectNext(optionalQuotesList)
//                .verifyComplete();
//
//        // Verify interactions
//        verify(mockQuoteProvider, times(1)).fetchQuotes();
//        verify(mockQuoteRepository, times(1)).bulkInsertQuotesIgnoreConflicts(quotesList);
//        verifyNoInteractions(mockQuoteLikeRepository);
//
//        // Assert Cache now contains the value
//        Cache.ValueWrapper cachedValueWrapper = cache.get(CACHE_KEY);
//        assertThat(cachedValueWrapper).isNotNull();
//        assertThat(cachedValueWrapper.get()).isEqualTo(optionalQuotesList);
//    }
//
//    @Test
//    @DisplayName("getQuotes should cache empty Optional when provider returns empty")
//    void getQuotes_shouldCacheEmptyOptional() {
//        // Arrange
//        Optional<List<Quote>> emptyOptional = Optional.empty();
//        when(mockQuoteProvider.fetchQuotes()).thenReturn(Mono.just(emptyOptional));
//
//        // Act 1 & Assert 1
//        StepVerifier.create(quotesCache.getQuotes())
//                .expectNext(emptyOptional)
//                .verifyComplete();
//
//        // Verify interactions
//        verify(mockQuoteProvider, times(1)).fetchQuotes();
//        verifyNoInteractions(mockQuoteRepository);
//        verifyNoInteractions(mockQuoteLikeRepository);
//
//        // Verify cache state
//        Cache cache = cacheManager.getCache(CACHE_NAME);
//        assertThat(cache).isNotNull();
//        Cache.ValueWrapper cachedValueWrapper = cache.get(CACHE_KEY);
//        assertThat(cachedValueWrapper).isNotNull();
//        assertThat(cachedValueWrapper.get()).isEqualTo(emptyOptional);
//
//        // Act 2 & Assert 2
//        StepVerifier.create(quotesCache.getQuotes())
//                .expectNext(emptyOptional)
//                .verifyComplete();
//
//        // Verify NO MORE interactions
//        verifyNoMoreInteractions(mockQuoteProvider);
//        verifyNoMoreInteractions(mockQuoteRepository);
//        verifyNoInteractions(mockQuoteLikeRepository);
//    }
//
//    @Test
//    @DisplayName("getQuotes should cache Optional with empty list when provider returns empty list")
//    void getQuotes_shouldCacheOptionalWithEmptyList() {
//        // Arrange
//        Optional<List<Quote>> optionalEmptyList = Optional.of(Collections.emptyList());
//        when(mockQuoteProvider.fetchQuotes()).thenReturn(Mono.just(optionalEmptyList));
//
//        // Act 1 & Assert 1
//        StepVerifier.create(quotesCache.getQuotes())
//                .expectNext(optionalEmptyList)
//                .verifyComplete();
//
//        // Verify interactions
//        verify(mockQuoteProvider, times(1)).fetchQuotes();
//        verifyNoInteractions(mockQuoteRepository);
//        verifyNoInteractions(mockQuoteLikeRepository);
//
//        // Verify cache state
//        Cache cache = cacheManager.getCache(CACHE_NAME);
//        assertThat(cache).isNotNull();
//        Cache.ValueWrapper cachedValueWrapper = cache.get(CACHE_KEY);
//        assertThat(cachedValueWrapper).isNotNull();
//        assertThat(cachedValueWrapper.get()).isEqualTo(optionalEmptyList);
//
//        // Act 2 & Assert 2
//        StepVerifier.create(quotesCache.getQuotes())
//                .expectNext(optionalEmptyList)
//                .verifyComplete();
//
//        // Verify NO MORE interactions
//        verifyNoMoreInteractions(mockQuoteProvider);
//        verifyNoMoreInteractions(mockQuoteRepository);
//        verifyNoInteractions(mockQuoteLikeRepository);
//    }
//}
