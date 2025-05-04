package com.amxcoding.randomquotes.application.interfaces.services;

import reactor.core.publisher.Mono;

public interface IQuoteLikeService {
    Mono<Boolean> likeQuote(String userId, Long quoteId);
    Mono<Boolean> unlikeQuote(String userId, Long quoteId);
    Mono<Boolean> checkUserLike(String userId, Long quoteId);
}
