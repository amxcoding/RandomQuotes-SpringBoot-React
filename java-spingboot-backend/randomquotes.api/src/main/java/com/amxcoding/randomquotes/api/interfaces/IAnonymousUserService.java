package com.amxcoding.randomquotes.api.interfaces;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;

public interface IAnonymousUserService {
    String getOrCreateAnonymousUserId(ServerHttpRequest request, ServerHttpResponse response);
}
