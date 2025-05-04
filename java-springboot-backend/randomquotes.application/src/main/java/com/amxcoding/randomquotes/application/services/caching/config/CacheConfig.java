package com.amxcoding.randomquotes.application.services.caching.config;

import com.amxcoding.randomquotes.application.common.Constants;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setAsyncCacheMode(true);

        // quotesCache
        cacheManager
                .setCaffeine(Caffeine.newBuilder()
                        .expireAfterWrite(3, TimeUnit.MINUTES)
                        .maximumSize(50)
                        .recordStats()
                );
        cacheManager.setCacheNames(List.of(Constants.Cache.QUOTES_CACHE));

        return cacheManager;
    }
}