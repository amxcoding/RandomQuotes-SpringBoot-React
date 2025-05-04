//package com.amxcoding.randomquotes.application.caching;
//
//import com.amxcoding.randomquotes.application.common.Constants;
//import com.amxcoding.randomquotes.application.interfaces.caching.IQuotesCache;
//import com.amxcoding.randomquotes.application.interfaces.providers.IQuoteProvider;
//import com.amxcoding.randomquotes.application.interfaces.repositories.IQuoteRepository;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.boot.test.context.TestConfiguration;
//import org.springframework.cache.CacheManager;
//import org.springframework.cache.annotation.EnableCaching;
//import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
//import org.springframework.context.annotation.Bean;
//
//@TestConfiguration
//@EnableCaching
//public class CacheTestConfig {
//
//    // Define the bean under test explicitly
//    @Bean
//    public IQuotesCache quotesCache(
//            @Qualifier("zenquotes") IQuoteProvider quoteProvider, // Inject mocks
//            IQuoteRepository quoteRepository) {
//        return new QuotesCache(quoteProvider, quoteRepository);
//    }
//
//    // Define a simple CacheManager for the test context
//    @Bean
//    public CacheManager cacheManager() {
//        // Use a simple in-memory cache manager for testing
//        // Make sure it knows about the cache name used in @Cacheable
//        return new ConcurrentMapCacheManager(Constants.Cache.QUOTES_CACHE);
//    }
//}
