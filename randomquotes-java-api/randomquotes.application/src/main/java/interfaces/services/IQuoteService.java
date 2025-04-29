package interfaces.services;

import entities.Quote;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface IQuoteService {
    CompletableFuture<Optional<Quote>> fetchRandomQuote();
}
