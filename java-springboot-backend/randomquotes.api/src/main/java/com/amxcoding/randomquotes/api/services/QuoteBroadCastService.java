package com.amxcoding.randomquotes.api.services;

import com.amxcoding.randomquotes.api.interfaces.IQuoteBroadCaster;
import com.amxcoding.randomquotes.api.models.quote.QuoteResponse;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class QuoteBroadCastService implements IQuoteBroadCaster, SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(QuoteBroadCastService.class);

    // Multicast sink that replays last 3 quotes to newly connected users
    private final Sinks.Many<QuoteResponse> sink = Sinks.many().replay().limit(3);
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    @Override
    public Mono<Void> emit(QuoteResponse quoteResponse) {
        if (quoteResponse == null) {
            log.warn("Attempted to emit a null QuoteResponse.");
            return Mono.error(new IllegalArgumentException("QuoteResponse to emit cannot be null"));
        }

        return Mono.defer(() -> {
            if (!isRunning.get()) {
                log.warn("Attempted to emit QuoteResponse while service is not running or shutting down. Quote id: {}", quoteResponse.getId());
                return Mono.error(new IllegalStateException("Service is not running. Cannot emit QuoteResponse."));
            }
            log.debug("Attempting to emit quote with id: {}", quoteResponse.getId());
            Sinks.EmitResult emitResult = this.sink.tryEmitNext(quoteResponse);

            if (emitResult.isFailure()) {
                log.warn("Failed to emit QuoteResponse with id: {}. Reason: {}. Current sink state might be relevant.",
                        quoteResponse.getId(), emitResult.name());
                return Mono.error(new IllegalStateException("Failed to emit QuoteResponse to sink. Reason: " + emitResult.name()));
            }
            log.info("Successfully emitted QuoteResponse with id: {}", quoteResponse.getId());
            return Mono.empty();
        });
    }

    @Override
    public Flux<QuoteResponse> getFlux() {
        return this.sink.asFlux()
                .doOnSubscribe(subscription -> log.info("New subscriber to quote stream."))
                .doOnCancel(() -> log.info("A subscriber cancelled their quote stream subscription."))
                .doOnTerminate(() -> log.info("Quote stream terminating for a subscriber (either complete or error)."));
    }

    @Override
    public void start() {
        log.info("QuoteBroadCastService starting.");
        isRunning.set(true);
        log.info("QuoteBroadCastService started and isRunning set to true.");
    }

    @Override
    public void stop() {}

    @Override
    public void stop(Runnable callback) {
        log.info("QuoteBroadCastService stopping (SmartLifecycle). Attempting to complete the sink.");
        isRunning.set(false); // Signal that the service is stopping
        Sinks.EmitResult completeResult = this.sink.tryEmitComplete();

        // Execute the callback to signal to Spring that this bean's shutdown phase is complete.
        callback.run();
        log.info("QuoteBroadCastService stop callback executed.");
    }

    @Override
    public boolean isRunning() {
        return isRunning.get();
    }

    /**
     * Beans with higher phase values are stopped earlier by DefaultLifecycleProcessor.
     */
    @Override
    public int getPhase() {
        // Ensure this stops before the web server's final steps
        return Integer.MAX_VALUE - 1000;
    }

//    @Override
//    public boolean isAutoStartup() {
//        return true;
//    }
}