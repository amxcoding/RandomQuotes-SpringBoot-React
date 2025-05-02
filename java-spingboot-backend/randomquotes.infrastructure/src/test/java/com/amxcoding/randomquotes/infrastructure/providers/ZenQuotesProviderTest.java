//package com.amxcoding.randomquotes.infrastructure.providers;
//
//import ch.qos.logback.classic.Level;
//import ch.qos.logback.classic.Logger;
//import ch.qos.logback.classic.spi.ILoggingEvent;
//import ch.qos.logback.core.read.ListAppender;
//import com.amxcoding.randomquotes.application.exceptions.providers.QuoteProviderException;
//import com.amxcoding.randomquotes.domain.entities.Quote;
//import okhttp3.mockwebserver.MockResponse;
//import okhttp3.mockwebserver.MockWebServer;
//import okhttp3.mockwebserver.RecordedRequest;
//import org.junit.jupiter.api.AfterEach;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import org.slf4j.LoggerFactory;
//import org.springframework.web.reactive.function.client.WebClient;
//
//import java.io.IOException;
//import java.time.Duration;
//import java.util.List;
//import java.util.Optional;
//import java.util.concurrent.CompletableFuture;
//import java.util.concurrent.ExecutionException;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//@ExtendWith(MockitoExtension.class)
//class ZenQuotesProviderTest {
//
//    private MockWebServer mockWebServer;
//    private ZenQuotesProvider zenQuotesProvider;
//    private ListAppender<ILoggingEvent> listAppender; // Logback appender
//
//    // Start the mock server before each test
//    @BeforeEach
//    void setUp() throws IOException {
//        mockWebServer = new MockWebServer();
//        mockWebServer.start();
//
//        String baseUrl = mockWebServer.url("/").toString();
//        WebClient.Builder webClientBuilder = WebClient.builder();
//        zenQuotesProvider = new ZenQuotesProvider(webClientBuilder, baseUrl);
//
//        // Setup Logback ListAppender
//        Logger providerLogger = (Logger) LoggerFactory.getLogger(ZenQuotesProvider.class);
//        listAppender = new ListAppender<>();
//        listAppender.start();
//        providerLogger.addAppender(listAppender);
//        providerLogger.setLevel(Level.DEBUG);
//    }
//
//    // Shut down the mock server and stop logging capture after each test
//    @AfterEach
//    void tearDown() throws IOException {
//        if (mockWebServer != null) {
//            mockWebServer.shutdown();
//            mockWebServer = null;
//        }
//
//        if (listAppender != null) {
//            Logger providerLogger = (Logger) LoggerFactory.getLogger(ZenQuotesProvider.class);
//            providerLogger.detachAppender(listAppender);
//            listAppender.stop();
//            listAppender = null;
//        }
//    }
//
//    @Test
//    void fetchRandomQuote_OnSuccess() throws ExecutionException, InterruptedException {
//        // Arrange
//        String jsonResponse = """
//                [
//                  {
//                    "a": "Test Author",
//                    "q": "This is a test quote."
//                  }
//                ]
//                """;
//        mockWebServer.enqueue(new MockResponse()
//                .setBody(jsonResponse)
//                .addHeader("Content-Type", "application/json"));
//
//        // Act
//        CompletableFuture<Optional<Quote>> future = zenQuotesProvider.fetchRandomQuote();
//        Optional<Quote> result = future.get();
//
//        // Assert
//        assertThat(result).isPresent();
//        assertThat(result.get().getAuthor()).isEqualTo("Test Author");
//        assertThat(result.get().getText()).isEqualTo("This is a test quote.");
//
//        // Assert Logging
//        assertThat(listAppender.list).isEmpty(); // No errors should be logged
//
//        // Assert Request
//        RecordedRequest recordedRequest = mockWebServer.takeRequest();
//        assertThat(recordedRequest.getMethod()).isEqualTo("GET");
//        assertThat(recordedRequest.getPath()).isEqualTo("/random");
//    }
//
//    @Test
//    void fetchRandomQuote_OnEmptyArray() throws ExecutionException, InterruptedException {
//        // Arrange
//        mockWebServer.enqueue(new MockResponse()
//                .setBody("[]") // Empty JSON array
//                .addHeader("Content-Type", "application/json"));
//
//        // Act
//        CompletableFuture<Optional<Quote>> future = zenQuotesProvider.fetchRandomQuote();
//        Optional<Quote> result = future.get();
//
//        // Assert Result
//        assertThat(result).isNotPresent();
//
//        // Assert Logging
//        assertThat(listAppender.list).isEmpty();
//
//        // Assert Request
//        RecordedRequest recordedRequest = mockWebServer.takeRequest();
//        assertThat(recordedRequest.getMethod()).isEqualTo("GET");
//        assertThat(recordedRequest.getPath()).isEqualTo("/random");
//    }
//
//
//    @Test
//    void fetchRandomQuote_onHttpError() throws ExecutionException, InterruptedException {
//        // Arrange: Simulate a 404 Not Found error
//        String errorBody = "{\"error\":\"Not Found\"}";
//        mockWebServer.enqueue(new MockResponse()
//                .setResponseCode(404)
//                .setBody(errorBody)
//                .addHeader("Content-Type", "application/json"));
//
//        // Act
//        CompletableFuture<Optional<Quote>> future = zenQuotesProvider.fetchRandomQuote();
//
//        // Assert Exception
//        assertThat(future)
//                .failsWithin(Duration.ofSeconds(1))
//                .withThrowableOfType(ExecutionException.class)
//                .withCauseInstanceOf(QuoteProviderException.class);
//
//        // Assert Logging
//        assertThat(listAppender.list).hasSize(1);
//        ILoggingEvent loggingEvent = listAppender.list.getFirst();
//        assertThat(loggingEvent.getLevel()).isEqualTo(Level.ERROR);
//
//        // Assert Request
//        RecordedRequest recordedRequest = mockWebServer.takeRequest();
//        assertThat(recordedRequest.getMethod()).isEqualTo("GET");
//        assertThat(recordedRequest.getPath()).isEqualTo("/random");
//    }
//
//    @Test
//    void fetchRandomQuote_onInvalidResponse() throws ExecutionException, InterruptedException {
//        // Arrange: Invalid JSON response
//        String invalidJson = "this is not valid json";
//        mockWebServer.enqueue(new MockResponse()
//                .setBody(invalidJson)
//                .addHeader("Content-Type", "application/json"));
//
//        // Act
//        CompletableFuture<Optional<Quote>> future = zenQuotesProvider.fetchRandomQuote();
//
//        // Assert Exception
//        assertThat(future)
//                .failsWithin(Duration.ofSeconds(1))
//                .withThrowableOfType(ExecutionException.class)
//                .withCauseInstanceOf(QuoteProviderException.class);
//
//        // Assert Logging
//        assertThat(listAppender.list).hasSize(1);
//        ILoggingEvent loggingEvent = listAppender.list.getFirst();
//        assertThat(loggingEvent.getLevel()).isEqualTo(Level.ERROR);
//
//        // Assert Request
//        RecordedRequest recordedRequest = mockWebServer.takeRequest();
//        assertThat(recordedRequest.getMethod()).isEqualTo("GET");
//        assertThat(recordedRequest.getPath()).isEqualTo("/random");
//    }
//
//    // --- *** Tests for fetchQuotes *** ---
//
//    @Test
//    void fetchQuotes_OnSuccess() throws ExecutionException, InterruptedException {
//        // Arrange
//        String jsonResponse = """
//                [
//                  { "a": "Author One", "q": "Quote one.", "h": "" },
//                  { "a": "Author Two", "q": "Quote two.", "h": "" }
//                ]
//                """;
//        mockWebServer.enqueue(new MockResponse()
//                .setBody(jsonResponse)
//                .addHeader("Content-Type", "application/json"));
//
//        // Act
//        CompletableFuture<Optional<List<Quote>>> future = zenQuotesProvider.fetchQuotes();
//        Optional<List<Quote>> result = future.get();
//
//        // Assert Result
//        assertThat(result).isPresent();
//        assertThat(result.get()).hasSize(2);
//        assertThat(result.get().get(0).getAuthor()).isEqualTo("Author One");
//        assertThat(result.get().get(0).getText()).isEqualTo("Quote one.");
//        assertThat(result.get().get(1).getAuthor()).isEqualTo("Author Two");
//        assertThat(result.get().get(1).getText()).isEqualTo("Quote two.");
//
//        // Assert Logging
//        assertThat(listAppender.list).isEmpty(); // No errors should be logged
//
//        // Assert Request
//        RecordedRequest recordedRequest = mockWebServer.takeRequest();
//        assertThat(recordedRequest.getMethod()).isEqualTo("GET");
//        assertThat(recordedRequest.getPath()).isEqualTo("/quotes");
//    }
//
//    @Test
//    void fetchQuotes_OnEmptyArray() throws ExecutionException, InterruptedException {
//        // Arrange
//        mockWebServer.enqueue(new MockResponse()
//                .setBody("[]") // Empty JSON array
//                .addHeader("Content-Type", "application/json"));
//
//        // Act
//        CompletableFuture<Optional<List<Quote>>> future = zenQuotesProvider.fetchQuotes();
//        Optional<List<Quote>> result = future.get();
//
//        // Assert Result
//        assertThat(result).isNotPresent();
//
//        // Assert Logging
//        assertThat(listAppender.list).isEmpty();
//
//        // Assert Request
//        RecordedRequest recordedRequest = mockWebServer.takeRequest();
//        assertThat(recordedRequest.getMethod()).isEqualTo("GET");
//        assertThat(recordedRequest.getPath()).isEqualTo("/quotes");
//    }
//
//    @Test
//    void fetchQuotes_onHttpError() throws ExecutionException, InterruptedException {
//        // Arrange: Simulate a 500 Internal Server Error
//        String errorBody = "{\"error\":\"Server Error\"}";
//        mockWebServer.enqueue(new MockResponse()
//                .setResponseCode(500)
//                .setBody(errorBody)
//                .addHeader("Content-Type", "application/json"));
//
//        // Act
//        CompletableFuture<Optional<List<Quote>>> future = zenQuotesProvider.fetchQuotes();
//
//        // Assert Exception
//        assertThat(future)
//                .failsWithin(Duration.ofSeconds(1))
//                .withThrowableOfType(ExecutionException.class)
//                .withCauseInstanceOf(QuoteProviderException.class);
//
//        // Assert Logging
//        assertThat(listAppender.list).hasSize(1);
//        ILoggingEvent loggingEvent = listAppender.list.getFirst();
//        assertThat(loggingEvent.getLevel()).isEqualTo(Level.ERROR);
//
//        // Assert Request
//        RecordedRequest recordedRequest = mockWebServer.takeRequest();
//        assertThat(recordedRequest.getMethod()).isEqualTo("GET");
//        assertThat(recordedRequest.getPath()).isEqualTo("/quotes");
//    }
//
//    @Test
//    void fetchQuotes_onInvalidResponse() throws ExecutionException, InterruptedException {
//        // Arrange: Invalid JSON response
//        String invalidJson = "{\"a\":"; // Incomplete JSON
//        mockWebServer.enqueue(new MockResponse()
//                .setBody(invalidJson)
//                .addHeader("Content-Type", "application/json"));
//
//        // Act
//        CompletableFuture<Optional<List<Quote>>> future = zenQuotesProvider.fetchQuotes();
//
//        // Assert Exception
//        assertThat(future)
//                .failsWithin(Duration.ofSeconds(1))
//                .withThrowableOfType(ExecutionException.class)
//                .withCauseInstanceOf(QuoteProviderException.class);
//
//        // Assert Logging
//        assertThat(listAppender.list).hasSize(1);
//        ILoggingEvent loggingEvent = listAppender.list.getFirst();
//        assertThat(loggingEvent.getLevel()).isEqualTo(Level.ERROR);
//
//        // Assert Request
//        RecordedRequest recordedRequest = mockWebServer.takeRequest();
//        assertThat(recordedRequest.getMethod()).isEqualTo("GET");
//        assertThat(recordedRequest.getPath()).isEqualTo("/quotes");
//    }
//}
