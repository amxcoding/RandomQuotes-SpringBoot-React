package interfaces.providers;

import entities.Quote;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface IQuoteProvider {
    CompletableFuture<Optional<Quote>> fetchRandomQuote();
}
