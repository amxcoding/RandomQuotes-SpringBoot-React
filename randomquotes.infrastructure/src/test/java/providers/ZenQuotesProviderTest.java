package providers;

import entities.Quote;
import interfaces.common.ILogger;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ZenQuotesProviderTest {

    private MockWebServer mockWebServer;
    private ZenQuotesProvider zenQuotesProvider;

    @Mock
    private ILogger mockLogger;

    // Start the mock server before each test
    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        // Generate URL like http://localhost:xxxxx/api/random
        String baseUrl = mockWebServer.url("/api/random").toString();

        WebClient.Builder webClientBuilder = WebClient.builder();

        // Instantiate the provider with the mock logger, real builder, and mock server URL
        zenQuotesProvider = new ZenQuotesProvider(mockLogger, webClientBuilder, baseUrl);
    }

    // Shut down the mock server after each test
    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void fetchRandomQuote_OnSuccess() throws ExecutionException, InterruptedException {
        // Arrange
        String jsonResponse = """
                [
                  {
                    "a": "Test Author",
                    "q": "This is a test quote."
                  }
                ]
                """;
        mockWebServer.enqueue(new MockResponse()
                .setBody(jsonResponse)
                .addHeader("Content-Type", "application/json"));

        // Act
        CompletableFuture<Optional<Quote>> future = zenQuotesProvider.fetchRandomQuote();
        Optional<Quote> result = future.get();

        // Assert
        assertTrue(result.isPresent(), "Quote should be present");
        assertEquals("Test Author", result.get().getAuthor());
        assertEquals("This is a test quote.", result.get().getText());
        assertEquals(0, result.get().getId());

        // Verify no error logging occurred
        verify(mockLogger, never()).error(anyString(), anyString(), any());
        verify(mockLogger, never()).error(anyString(), anyString(), anyString(), any());

        // Verify one request was made to the mock server
        assertEquals(1, mockWebServer.getRequestCount());
    }

    @Test
    void fetchRandomQuote_OnEmptyArray() throws ExecutionException, InterruptedException {
        // Arrange
        mockWebServer.enqueue(new MockResponse()
                .setBody("[]") // Empty JSON array
                .addHeader("Content-Type", "application/json"));

        // Act
        CompletableFuture<Optional<Quote>> future = zenQuotesProvider.fetchRandomQuote();
        Optional<Quote> result = future.get();

        // Assert
        assertFalse(result.isPresent(), "Quote should not be present for empty array response");
        verify(mockLogger, never()).error(anyString(), anyString(), any());
        verify(mockLogger, never()).error(anyString(), anyString(), anyString(), any());
        assertEquals(1, mockWebServer.getRequestCount());
    }


    @Test
    void fetchRandomQuote_onHttpError() throws ExecutionException, InterruptedException {
        // Arrange: Simulate a 404 Not Found error
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(404)
                .setBody("{\"error\":\"Not Found\"}") // Example error body
                .addHeader("Content-Type", "application/json"));

        // Act
        CompletableFuture<Optional<Quote>> future = zenQuotesProvider.fetchRandomQuote();
        Optional<Quote> result = future.get(); // Error should be handled internally

        // Assert
        assertFalse(result.isPresent(), "Quote should not be present on HTTP error");
        assertEquals(1, mockWebServer.getRequestCount());

        // Verify that the specific HTTP error was logged using the mockLogger
        verify(mockLogger, times(1)).error(
                eq("ZenQuotesProvider"),
                eq("HTTP Error (status {})  {} - {}"),
                eq("404 NOT_FOUND"),
                eq("{\"error\":\"Not Found\"}"),
                any(WebClientResponseException.class)
        );

        // Verify no "other" errors were logged
        verify(mockLogger, never())
                .error(eq("ZenQuotesProvider"),
                        eq("Other errors on getting the quote {}: {}"), anyString(), any());
    }

    @Test
    void fetchRandomQuote_onInvalidResponse() throws ExecutionException, InterruptedException {
        // Arrange: Invalid JSON response
        mockWebServer.enqueue(new MockResponse()
                .setBody("this is not valid json")
                .addHeader("Content-Type", "application/json"));

        // Act
        CompletableFuture<Optional<Quote>> future = zenQuotesProvider.fetchRandomQuote();
        Optional<Quote> result = future.get(); // Error should be handled internally

        // Assert
        assertFalse(result.isPresent(), "Quote should not be present for invalid JSON");
        assertEquals(1, mockWebServer.getRequestCount());

        // Verify that the "other" error was logged
        verify(mockLogger, times(1)).error(
                eq("ZenQuotesProvider"),
                eq("Other errors on getting the quote {}: {}"),
                anyString(),
                any(Exception.class)
        );
        // Verify no HTTP specific errors were logged
        verify(mockLogger, never())
                .error(eq("ZenQuotesProvider"),
                        eq("HTTP Error (status {})  {} - {}"), anyString(), anyString(), any());
    }
}
