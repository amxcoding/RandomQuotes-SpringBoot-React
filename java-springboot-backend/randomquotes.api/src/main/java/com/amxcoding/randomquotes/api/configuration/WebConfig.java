package com.amxcoding.randomquotes.api.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class WebConfig {
    private final String[] allowedOrigins;
    private final String[] allowedMethods;

    public WebConfig(@Value("${randomquotes.cors.origins}") String[] allowedOrigins,
                     @Value("${randomquotes.cors.methods}") String[] allowedMethods) {
        this.allowedOrigins = allowedOrigins;
        this.allowedMethods = allowedMethods;
    }

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(allowedOrigins));
        config.setAllowedMethods(List.of(allowedMethods));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);

        return new CorsWebFilter(source);
    }
}
