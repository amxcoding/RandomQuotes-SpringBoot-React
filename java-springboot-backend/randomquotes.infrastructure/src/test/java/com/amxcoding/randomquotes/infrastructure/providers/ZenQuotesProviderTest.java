// TODO FIX
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
//import org.junit.jupiter.api.*;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.slf4j.LoggerFactory;
//import org.springframework.web.reactive.function.client.WebClient;
//import org.springframework.web.reactive.function.client.WebClientResponseException;
//import reactor.core.publisher.Mono;
//import reactor.test.StepVerifier;
//
//import java.io.IOException;
//import java.util.List;
//import java.util.Optional;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//@ExtendWith(MockitoExtension.class)
//class ZenQuotesProviderTest {
//
//    private MockWebServer mockWebServer;
//    private ZenQuotesProvider zenQuotesProvider;
//    private ListAppender<ILoggingEvent> listAppender;
//
//    @BeforeEach
//    void setUp() throws IOException {
//        // Start mock HTTP server and configure WebClient
//        mockWebServer = new MockWebServer();
//        mockWebServer.start();
//
//        String baseUrl = mockWebServer.url("/").toString();
//        WebClient.Builder webClientBuilder = WebClient.builder();
//        zenQuotesProvider = new ZenQuotesProvider(webClientBuilder, baseUrl);
//
//        // Capture ERROR-level logs from provider
//        Logger providerLogger = (Logger) LoggerFactory.getLogger(ZenQuotesProvider.class);
//        listAppender = new ListAppender<>();
//        listAppender.start();
//        providerLogger.addAppender(listAppender);
//        providerLogger.setLevel(Level.ERROR);
//    }
//
//    @AfterEach
//    void tearDown() throws IOException {
//        // Shut down mock server and logging
//        if (mockWebServer != null) {
//            mockWebServer.shutdown();
//            mockWebServer = null;
//        }
//        if (listAppender != null) {
//            Logger providerLogger = (Logger) LoggerFactory.getLogger(ZenQuotesProvider.class);
//            providerLogger.detachAppender(listAppender);
//            listAppender.stop();
//            listAppender = null;
//        }
//    }
//
//    @Test
//    @DisplayName("returns list of quotes when API is successful")
//    void fetchQuotes_returnsQuotes_whenApiSucceeds() throws InterruptedException {
//        // Arrange: enqueue a valid JSON array of quotes
//        String jsonResponse = """
//                [
//                  { \"a\": \"Author One\", \"q\": \"Quote one.\", \"h\": \"\" },
//                  { \"a\": \"Author Two\", \"q\": \"Quote two.\", \"h\": \"\" }
//                ]
//                """;
//        mockWebServer.enqueue(new MockResponse()
//                .setBody(jsonResponse)
//                .addHeader("Content-Type", "application/json"));
//
//        // Act: call fetchQuotes()
//        Mono<Optional<List<Quote>>> resultMono = zenQuotesProvider.fetchQuotes();
//
//        // Assert: verify emitted quotes and absence of errors
//        StepVerifier.create(resultMono)
//                .assertNext(opt -> {
//                    assertThat(opt).isPresent();
//                    List<Quote> quotes = opt.get();
//                    assertThat(quotes).hasSize(2);
//                    assertThat(quotes.get(0).getAuthor()).isEqualTo("Author One");
//                    assertThat(quotes.get(0).getText()).isEqualTo("Quote one.");
//                })
//                .verifyComplete();
//
//        // Assert: no logs and correct request
//        assertThat(listAppender.list).isEmpty();
//        RecordedRequest request = mockWebServer.takeRequest();
//        assertThat(request.getMethod()).isEqualTo("GET");
//        assertThat(request.getPath()).isEqualTo("/quotes");
//    }
//
//
//    @Test
//    @DisplayName("returns empty Optional when API returns empty array")
//    void fetchQuotes_returnsEmpty_whenApiReturnsEmptyArray() throws InterruptedException {
//        // Arrange: enqueue an empty JSON array
//        mockWebServer.enqueue(new MockResponse()
//                .setBody("[]")
//                .addHeader("Content-Type", "application/json"));
//
//        // Act: call fetchQuotes()
//        Mono<Optional<List<Quote>>> resultMono = zenQuotesProvider.fetchQuotes();
//
//        // Assert: verify empty Optional and no errors
//        StepVerifier.create(resultMono)
//                .assertNext(opt -> assertThat(opt).isNotPresent())
//                .verifyComplete();
//
//        // Assert: no logs and correct request path
//        assertThat(listAppender.list).isEmpty();
//        RecordedRequest request = mockWebServer.takeRequest();
//        assertThat(request.getPath()).isEqualTo("/quotes");
//    }
//
//
//    @Test
//    @DisplayName("returns empty Optional when API responds no content")
//    void fetchQuotes_returnsEmpty_whenNoContent() throws InterruptedException {
//        // Arrange: enqueue a 204 No Content
//        mockWebServer.enqueue(new MockResponse().setResponseCode(204));
//
//        // Act: call fetchQuotes()
//        Mono<Optional<List<Quote>>> resultMono = zenQuotesProvider.fetchQuotes();
//
//        // Assert: expect an empty Optional from defaultIfEmpty
//        StepVerifier.create(resultMono)
//                .expectNext(Optional.empty())
//                .verifyComplete();
//
//        // Assert: no logs and correct request path
//        assertThat(listAppender.list).isEmpty();
//        RecordedRequest request = mockWebServer.takeRequest();
//        assertThat(request.getPath()).isEqualTo("/quotes");
//    }
//
//
//    @Test
//    @DisplayName("throws QuoteProviderException on HTTP error and logs it")
//    void fetchQuotes_throwsException_andLogs_onHttpError() throws InterruptedException {
//        // Arrange: enqueue a 500 server error
//        String errorBody = "{\"error\":\"Server Error\"}";
//        mockWebServer.enqueue(new MockResponse()
//                .setResponseCode(500)
//                .setBody(errorBody)
//                .addHeader("Content-Type", "application/json"));
//
//        // Act: call fetchQuotes()
//        Mono<Optional<List<Quote>>> resultMono = zenQuotesProvider.fetchQuotes();
//
//        // Assert: expect QuoteProviderException with HTTP cause
//        StepVerifier.create(resultMono)
//                .expectErrorSatisfies(ex -> {
//                    assertThat(ex).isInstanceOf(QuoteProviderException.class)
//                            .hasMessageContaining("Error fetching all quotes");
//                    assertThat(ex.getCause())
//                            .isInstanceOf(WebClientResponseException.InternalServerError.class);
//                })
//                .verify();
//
//        // Assert: error logged and correct request path
//        Thread.sleep(100);
//        assertThat(listAppender.list).hasSize(1);
//        assertThat(listAppender.list.getFirst().getLevel()).isEqualTo(Level.ERROR);
//        RecordedRequest request = mockWebServer.takeRequest();
//        assertThat(request.getPath()).isEqualTo("/quotes");
//    }
//
//
//    @Test
//    @DisplayName("throws QuoteProviderException on invalid JSON response and logs it")
//    void fetchQuotes_throwsException_andLogs_onInvalidJson() throws InterruptedException {
//        // Arrange: enqueue invalid JSON
//        mockWebServer.enqueue(new MockResponse()
//                .setBody("{\"a\":")
//                .addHeader("Content-Type", "application/json"));
//
//        // Act: call fetchQuotes()
//        Mono<Optional<List<Quote>>> resultMono = zenQuotesProvider.fetchQuotes();
//
//        // Assert: expect QuoteProviderException with parsing cause
//        StepVerifier.create(resultMono)
//                .expectErrorSatisfies(ex -> {
//                    assertThat(ex).isInstanceOf(QuoteProviderException.class)
//                            .hasMessageContaining("Error fetching all quotes");
//                    assertThat(ex.getCause()).isNotNull();
//                })
//                .verify();
//
//        // Assert: error logged and correct request path
//        Thread.sleep(100);
//        assertThat(listAppender.list).hasSize(1);
//        assertThat(listAppender.list.getFirst().getLevel()).isEqualTo(Level.ERROR);
//        RecordedRequest request = mockWebServer.takeRequest();
//        assertThat(request.getPath()).isEqualTo("/quotes");
//    }
//}
