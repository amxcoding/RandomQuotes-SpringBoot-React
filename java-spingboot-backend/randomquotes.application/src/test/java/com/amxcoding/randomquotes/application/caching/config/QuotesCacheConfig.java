package com.amxcoding.randomquotes.application.caching.config;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Minimal Spring configuration for testing QuoteService and related components.
 * Loads autoconfiguration and scans necessary packages within the application layer.
 */
@Configuration
@EnableAutoConfiguration
@EnableCaching
@ComponentScan(basePackages = {
        "com.amxcoding.randomquotes.application",
        "com.amxcoding.randomquotes.infrastructure",
        "com.amxcoding.randomquotes.domain"
})
public class QuotesCacheConfig {}