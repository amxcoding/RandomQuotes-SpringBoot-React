package com.amxcoding.randomquotes.api.interfaces;

import com.amxcoding.randomquotes.api.models.quote.QuoteResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface IQuoteBroadCaster {
    Mono<Void> emit(QuoteResponse quoteResponse);
    Flux<QuoteResponse> getFlux();
}
