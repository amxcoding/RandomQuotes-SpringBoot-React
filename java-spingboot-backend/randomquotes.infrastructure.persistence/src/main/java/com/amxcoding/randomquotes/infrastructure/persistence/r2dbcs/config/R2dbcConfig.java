package com.amxcoding.randomquotes.infrastructure.persistence.r2dbcs.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

@Configuration
@EnableR2dbcRepositories(basePackages = "com.amxcoding.randomquotes.infrastructure.persistence.r2dbcs")
public class R2dbcConfig {
    // Configuration if needed
}
