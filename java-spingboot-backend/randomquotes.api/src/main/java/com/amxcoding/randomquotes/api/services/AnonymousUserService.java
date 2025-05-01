package com.amxcoding.randomquotes.api.services;

import com.amxcoding.randomquotes.api.interfaces.IAnonymousUserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
public class AnonymousUserService implements IAnonymousUserService {

    private static final String USER_ID_COOKIE_NAME = "user_id";
    private static final Duration USER_ID_COOKIE_MAX_AGE = Duration.ofDays(365);

    private final boolean secureCookies;

    // Default to false if property is missing
    public AnonymousUserService(@Value("${app.cookie.secure:false}") boolean secureCookies) {
        this.secureCookies = secureCookies;
    }

    @Override
    public String getOrCreateAnonymousUserId(ServerHttpRequest request, ServerHttpResponse response) {
        // Look for the user ID in the cookies
        String userId = null;
        if (request.getCookies().containsKey(USER_ID_COOKIE_NAME)) {
            HttpCookie cookie =  request.getCookies().getFirst(USER_ID_COOKIE_NAME);

            if (cookie != null) {
                userId = cookie.getValue();
            }
        }

        if (userId != null) {
            return userId;
        }

        // If cookie not found, create a new one
        String newUserId = UUID.randomUUID().toString();
        ResponseCookie newCookie = ResponseCookie.from(USER_ID_COOKIE_NAME, newUserId)
                .httpOnly(true)
                .path("/")
                .maxAge(USER_ID_COOKIE_MAX_AGE)
                .secure(secureCookies)
                .build();

        // Add the cookie to the response (using WebFlux's response.addCookie)
        response.addCookie(newCookie);

        return newUserId;
    }
}
