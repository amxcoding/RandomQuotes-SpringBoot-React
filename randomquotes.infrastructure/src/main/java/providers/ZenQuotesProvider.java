package providers;

import entities.Quote;
import interfaces.common.ILogger;
import interfaces.providers.IQuoteProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Component
@Qualifier("ZenQuotes")
public class ZenQuotesProvider implements IQuoteProvider {

    private final ILogger logger;
    private final WebClient webClient;
    private final String apiUrl;

    public ZenQuotesProvider(ILogger logger, WebClient.Builder webClientBuilder, @Value("${quoteprovider.zenquotes}") String apiUrl) {
        this.logger = logger;
        this.webClient = webClientBuilder.build();
        this.apiUrl = apiUrl;
    }

    @Override
    public CompletableFuture<Optional<Quote>> fetchRandomQuote() {
        Mono<Optional<Quote>> quoteMono = webClient
                .get()
                .uri(this.apiUrl)
                .retrieve()
                .bodyToMono(Quote[].class)
                .map(quotes -> {
                    if (quotes != null && quotes.length > 0) {
                        return Optional.of(quotes[0]);
                    } else {
                        return Optional.<Quote>empty();
                    }
                })
                // On Http error
                .onErrorResume(WebClientResponseException.class, ex -> {
                    // TODO throw exception and handle on the front
                    logger.error("ZenQuotesProvider", "HTTP Error (status {})  {} - {}", String.valueOf(ex.getStatusCode()), ex.getResponseBodyAsString(), ex);
                    return Mono.just(Optional.empty());
                })
                // Other errors
                .onErrorResume(ex -> {
                    // TODO throw exception and handle on the front
                    logger.error("ZenQuotesProvider","Other errors on getting the quote {}: {}", ex.getMessage(), ex);
                    return Mono.just(Optional.empty());
                })
                .defaultIfEmpty(Optional.empty());

        return quoteMono.toFuture();
    }
}
