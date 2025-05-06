package com.amxcoding.randomquotes.api.services;

import com.amxcoding.randomquotes.api.interfaces.IQuoteBroadCaster;
import com.amxcoding.randomquotes.api.models.quote.QuoteResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

@Service
public class QuoteBroadCastService implements IQuoteBroadCaster {

    private static final Logger log = LoggerFactory.getLogger(QuoteBroadCastService.class);

    // Multicast sink that replays last 3 quotes to newly connected users
    private final Sinks.Many<QuoteResponse> sink = Sinks.many().replay().limit(3);

    @Override
    public Mono<Void> emit(QuoteResponse quoteResponse) {
        if (quoteResponse == null) {
            return Mono.error(new IllegalArgumentException("QuoteResponse to emit cannot be null"));
        }

        return Mono.fromRunnable(() -> {
            Sinks.EmitResult emitResult = this.sink.tryEmitNext(quoteResponse);

            if (emitResult.isFailure()) {
                log.warn("Failed to emit QuoteResponse with id: {}. Reason: {}. Current sink state might be relevant.",
                        quoteResponse.getId(), emitResult.name());
                
                throw new IllegalStateException("Failed to emit QuoteResponse to sink. Reason: " + emitResult.name());
            }
        });
    }

    @Override
    public Flux<QuoteResponse> getFlux() {
        return this.sink.asFlux();
    }
}