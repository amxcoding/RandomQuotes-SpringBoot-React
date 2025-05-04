package com.amxcoding.randomquotes.infrastructure.repositories;


import com.amxcoding.randomquotes.infrastructure.persistence.mappers.QuoteEntityMapper;
import org.mapstruct.factory.Mappers;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;


/**
 * Minimal Spring configuration for testing
 * <p>
 * Loads autoconfiguration and scans necessary packages within the application layer.
 */
@SpringBootConfiguration
@EnableR2dbcRepositories(basePackages = "com.amxcoding.randomquotes.infrastructure.persistence.r2dbcs")
//@ComponentScan(basePackages = "com.amxcoding.randomquotes.infrastructure.persistence.mappers")
public class TestConfig {

    @Bean
    public QuoteEntityMapper quoteEntityMapper() {
        return Mappers.getMapper(QuoteEntityMapper.class);

    }

}