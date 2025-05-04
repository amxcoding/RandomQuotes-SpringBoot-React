// TODO FIX
//package com.amxcoding.randomquotes.application.caching;
//
//import com.amxcoding.randomquotes.application.common.Constants;
//import com.amxcoding.randomquotes.application.interfaces.services.IQuotesCache;
//import com.amxcoding.randomquotes.application.interfaces.providers.IQuoteProvider;
//import com.amxcoding.randomquotes.application.interfaces.repositories.IQuoteRepository;
//import com.amxcoding.randomquotes.application.services.caching.QuotesCache;
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
//    @Bean
//    public IQuotesCache quotesCache(
//            @Qualifier("zenquotes") IQuoteProvider quoteProvider,
//            IQuoteRepository quoteRepository,
//            CacheManager cacheManager) {
//        return new QuotesCache(quoteProvider, quoteRepository, cacheManager);
//    }
//
//    @Bean
//    public CacheManager cacheManager() {
//        return new ConcurrentMapCacheManager(Constants.Cache.QUOTES_CACHE);
//    }
//}
