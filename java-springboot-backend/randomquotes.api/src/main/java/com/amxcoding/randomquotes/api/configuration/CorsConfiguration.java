package com.amxcoding.randomquotes.api.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class CorsConfiguration {
    private final String[] allowedOrigins;
    private final String[] allowedMethodsApi;
    private final String[] allowedMethodsSse;

    public CorsConfiguration(@Value("${randomquotes.cors.origins}") String[] allowedOrigins,
                             @Value("${randomquotes.cors.methods.api}") String[] allowedMethodsApi,
                             @Value("${randomquotes.cors.methods.sse}")String[] allowedMethodsSse) {
        this.allowedOrigins = allowedOrigins;
        this.allowedMethodsApi = allowedMethodsApi;
        this.allowedMethodsSse = allowedMethodsSse;
    }

    @Bean
    public CorsWebFilter corsWebFilter() {
        org.springframework.web.cors.CorsConfiguration configApi = new org.springframework.web.cors.CorsConfiguration();
        configApi.setAllowedOrigins(List.of(allowedOrigins));
        configApi.setAllowedMethods(List.of(allowedMethodsApi));
        configApi.setAllowedHeaders(List.of("*"));
        configApi.setAllowCredentials(true);
        configApi.setMaxAge(3600L);

        org.springframework.web.cors.CorsConfiguration configSse = new org.springframework.web.cors.CorsConfiguration();
        configSse.setAllowedOrigins(List.of(allowedOrigins));
        configSse.setAllowedMethods(List.of(allowedMethodsSse));
        configSse.setAllowedHeaders(List.of("*"));
        configSse.setAllowCredentials(true);
        configSse.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configApi);
        source.registerCorsConfiguration("/sse/**", configSse);

        return new CorsWebFilter(source);
    }
}
