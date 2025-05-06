package com.amxcoding.randomquotes.api.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.accept.RequestedContentTypeResolverBuilder;
import org.springframework.web.reactive.config.WebFluxConfigurer;

@Configuration
public class WebFluxConfig implements WebFluxConfigurer {

    @Override
    public void configureContentTypeResolver(RequestedContentTypeResolverBuilder builder) {
        // Conflicts with stream
//        builder.fixedResolver(MediaType.APPLICATION_JSON);
    }
}
