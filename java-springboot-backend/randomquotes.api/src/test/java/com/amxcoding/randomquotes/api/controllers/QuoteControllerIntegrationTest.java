// TODO: fix scope issues


//package com.amxcoding.randomquotes.api.controllers;
//
//import com.amxcoding.randomquotes.api.Application;
//import com.amxcoding.randomquotes.api.interfaces.IAnonymousUserService;
//import com.amxcoding.randomquotes.api.mappers.QuoteMapper;
//import com.amxcoding.randomquotes.api.models.quote.QuoteResponse;
//import com.amxcoding.randomquotes.application.caching.QuotesCache;
//import com.amxcoding.randomquotes.application.interfaces.services.IQuoteLikeService;
//import com.amxcoding.randomquotes.application.interfaces.services.IQuoteService;
//import com.amxcoding.randomquotes.application.services.QuoteLikeService;
//import com.amxcoding.randomquotes.domain.entities.Quote;
//import com.amxcoding.randomquotes.infrastructure.providers.ZenQuotesProvider;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Nested;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
//import org.springframework.boot.autoconfigure.data.r2dbc.R2dbcDataAutoConfiguration;
//import org.springframework.boot.autoconfigure.data.r2dbc.R2dbcRepositoriesAutoConfiguration;
//import org.springframework.boot.autoconfigure.r2dbc.R2dbcAutoConfiguration;
//import org.springframework.boot.autoconfigure.r2dbc.R2dbcTransactionManagerAutoConfiguration;
//import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
//import org.springframework.context.annotation.ComponentScan;
//import org.springframework.context.annotation.FilterType;
//import org.springframework.http.MediaType;
//import org.springframework.test.context.bean.override.mockito.MockitoBean;
//import org.springframework.test.web.reactive.server.WebTestClient;
//import reactor.core.publisher.Mono;
//
//import java.util.Optional;
//
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.*;
//
//// Configure WebFluxTest targeting only QuoteController.
//// Exclude specific components and auto-configurations to prevent loading unnecessary beans
//// (like database, cache, external providers) for this focused web layer test.
//@WebFluxTest(controllers = QuoteController.class,
//        excludeFilters = {
//                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
//                        Application.class,
//                        QuotesCache.class,
//                        ZenQuotesProvider.class,
//                        QuoteLikeService.class
//                })
//        },
//        excludeAutoConfiguration = {
//                R2dbcAutoConfiguration.class,
//                R2dbcDataAutoConfiguration.class,
//                R2dbcRepositoriesAutoConfiguration.class,
//                R2dbcTransactionManagerAutoConfiguration.class,
//                CacheAutoConfiguration.class
//        })
//@DisplayName("QuoteController Web Layer Tests")
//class QuoteControllerIntegrationTest {
//
//    @Autowired
//    private WebTestClient webTestClient;
//
//    @MockitoBean
//    private IQuoteService mockQuoteService;
//    @MockitoBean
//    private IQuoteLikeService mockQuoteLikeService;
//    @MockitoBean
//    private IAnonymousUserService mockAnonymousUserService;
//    @MockitoBean
//    private QuoteMapper quoteMapper; // Mock the mapper if its logic is complex or tested separately
//
//    private final String ANONYMOUS_USER_ID = "test-user-123";
//    private Quote testQuote;
//    private QuoteResponse testQuoteResponse;
//
//    @BeforeEach
//    void setUp() {
//        testQuote = new Quote(1L, "Test Author", "Test Text", 5);
//        testQuoteResponse = quoteMapper.toQuoteResponse(testQuote); // Use the mock mapper
//
//        // Common mocking for anonymous user service needed in most tests
//        when(mockAnonymousUserService.getOrCreateAnonymousUserId(any(), any())).thenReturn(ANONYMOUS_USER_ID);
//    }
//
//    // --- Tests for GET /random ---
//    @Nested
//    @DisplayName("GET /api/v1/quotes/random")
//    class GetRandomQuoteTests {
//
//        @Test
//        @DisplayName("should return 200 OK with random quote and liked status when quote found")
//        void getRandomQuote_found_liked() {
//            // Arrange: Mock services to return a quote and indicate it's liked by the user.
//            // Prepare the expected response DTO.
//            testQuoteResponse.setIsLiked(true);
//            when(mockQuoteService.getRandomQuote()).thenReturn(Mono.just(Optional.of(testQuote)));
//            when(mockQuoteLikeService.checkUserLike(ANONYMOUS_USER_ID, testQuote.getId())).thenReturn(Mono.just(true));
//            // Mock the mapper behavior for this specific test case
//            when(quoteMapper.toQuoteResponse(testQuote)).thenReturn(testQuoteResponse);
//
//
//            // Act & Assert: Perform GET request and verify the response status, headers, and body.
//            webTestClient.get().uri("/api/v1/quotes/random")
//                    .accept(MediaType.APPLICATION_JSON)
//                    .exchange()
//                    .expectStatus().isOk()
//                    .expectHeader().contentType(MediaType.APPLICATION_JSON)
//                    .expectBody(QuoteResponse.class)
//                    .isEqualTo(testQuoteResponse);
//
//            // Verify service calls: Ensure expected service methods were invoked correctly.
//            verify(mockAnonymousUserService, times(1)).getOrCreateAnonymousUserId(any(), any());
//            verify(mockQuoteService, times(1)).getRandomQuote();
//            verify(mockQuoteLikeService, times(1)).checkUserLike(ANONYMOUS_USER_ID, testQuote.getId());
//            verify(quoteMapper, times(1)).toQuoteResponse(testQuote); // Verify mapper interaction
//        }
//
//        @Test
//        @DisplayName("should return 200 OK with random quote and not liked status when quote found")
//        void getRandomQuote_found_notLiked() {
//            // Arrange: Mock services to return a quote and indicate it's not liked by the user.
//            // Prepare the expected response DTO.
//            testQuoteResponse.setIsLiked(false);
//            when(mockQuoteService.getRandomQuote()).thenReturn(Mono.just(Optional.of(testQuote)));
//            when(mockQuoteLikeService.checkUserLike(ANONYMOUS_USER_ID, testQuote.getId())).thenReturn(Mono.just(false));
//            // Mock the mapper behavior for this specific test case
//            when(quoteMapper.toQuoteResponse(testQuote)).thenReturn(testQuoteResponse);
//
//            // Act & Assert: Perform GET request and verify the response.
//            webTestClient.get().uri("/api/v1/quotes/random")
//                    .accept(MediaType.APPLICATION_JSON)
//                    .exchange()
//                    .expectStatus().isOk()
//                    .expectHeader().contentType(MediaType.APPLICATION_JSON)
//                    .expectBody(QuoteResponse.class)
//                    .isEqualTo(testQuoteResponse);
//
//            // Verify service calls: Ensure expected service methods were invoked correctly.
//            verify(mockAnonymousUserService, times(1)).getOrCreateAnonymousUserId(any(), any());
//            verify(mockQuoteService, times(1)).getRandomQuote();
//            verify(mockQuoteLikeService, times(1)).checkUserLike(ANONYMOUS_USER_ID, testQuote.getId());
//            verify(quoteMapper, times(1)).toQuoteResponse(testQuote); // Verify mapper interaction
//        }
//
//
//        @Test
//        @DisplayName("should return 404 Not Found when quote service returns empty optional")
//        void getRandomQuote_notFound() {
//            // Arrange: Mock quote service to indicate no random quote is available.
//            when(mockQuoteService.getRandomQuote()).thenReturn(Mono.just(Optional.empty()));
//
//            // Act & Assert: Perform GET request and verify 404 status and empty body.
//            webTestClient.get().uri("/api/v1/quotes/random")
//                    .accept(MediaType.APPLICATION_JSON)
//                    .exchange()
//                    .expectStatus().isNotFound()
//                    .expectBody().isEmpty();
//
//            // Verify service calls: Ensure only necessary services were called.
//            verify(mockAnonymousUserService, times(1)).getOrCreateAnonymousUserId(any(), any());
//            verify(mockQuoteService, times(1)).getRandomQuote();
//            verifyNoInteractions(mockQuoteLikeService); // Like service shouldn't be called if no quote found
//            verifyNoInteractions(quoteMapper);        // Mapper shouldn't be called if no quote found
//        }
//
//        @Test
//        @DisplayName("should return 500 Internal Server Error when quote service fails")
//        void getRandomQuote_serviceError() {
//            // Arrange: Mock quote service to throw an error.
//            when(mockQuoteService.getRandomQuote()).thenReturn(Mono.error(new RuntimeException("Service unavailable")));
//
//            // Act & Assert: Perform GET request and verify 5xx server error status.
//            webTestClient.get().uri("/api/v1/quotes/random")
//                    .accept(MediaType.APPLICATION_JSON)
//                    .exchange()
//                    .expectStatus().is5xxServerError();
//
//            // Verify service calls: Ensure only necessary services were called before the error.
//            verify(mockAnonymousUserService, times(1)).getOrCreateAnonymousUserId(any(), any());
//            verify(mockQuoteService, times(1)).getRandomQuote();
//            verifyNoInteractions(mockQuoteLikeService);
//            verifyNoInteractions(quoteMapper);
//        }
//    }
//
//    // --- Tests for GET /{id}/like ---
//    @Nested
//    @DisplayName("GET /api/v1/quotes/{id}/like")
//    class LikeQuoteTests {
//
//        @Test
//        @DisplayName("should return 200 OK with updated quote (liked) after successful like operation")
//        void likeQuote_success() {
//            // Arrange: Mock successful liking, subsequent quote retrieval, and checking the new like status.
//            // Prepare the expected response DTO reflecting the 'liked' state.
//            Long quoteId = testQuote.getId();
//            testQuoteResponse.setIsLiked(true);
//
//            when(mockQuoteLikeService.likeQuote(ANONYMOUS_USER_ID, quoteId)).thenReturn(Mono.empty());
//            when(mockQuoteService.getQuoteById(quoteId)).thenReturn(Mono.just(Optional.of(testQuote)));
//            when(mockQuoteLikeService.checkUserLike(ANONYMOUS_USER_ID, quoteId)).thenReturn(Mono.just(true));
//            // Mock the mapper behavior for this specific test case
//            when(quoteMapper.toQuoteResponse(testQuote)).thenReturn(testQuoteResponse);
//
//            // Act & Assert: Perform GET request to like the quote and verify the successful response.
//            webTestClient.get().uri("/api/v1/quotes/{id}/like", quoteId)
//                    .accept(MediaType.APPLICATION_JSON)
//                    .exchange()
//                    .expectStatus().isOk()
//                    .expectBody(QuoteResponse.class)
//                    .isEqualTo(testQuoteResponse);
//
//            // Verify service calls: Ensure expected service methods were invoked in the correct sequence.
//            verify(mockAnonymousUserService, times(1)).getOrCreateAnonymousUserId(any(), any());
//            verify(mockQuoteLikeService, times(1)).likeQuote(ANONYMOUS_USER_ID, quoteId);
//            verify(mockQuoteService, times(1)).getQuoteById(quoteId);
//            verify(mockQuoteLikeService, times(1)).checkUserLike(ANONYMOUS_USER_ID, quoteId);
//            verify(quoteMapper, times(1)).toQuoteResponse(testQuote); // Verify mapper interaction
//        }
//
//        @Test
//        @DisplayName("should return 404 Not Found if quote doesn't exist when trying to fetch it after like attempt")
//        void likeQuote_quoteNotFoundAfterLike() {
//            // Arrange: Mock successful liking but mock quote retrieval to return empty optional (quote disappeared?).
//            Long quoteId = 999L; // Use an ID assumed not to exist
//            when(mockQuoteLikeService.likeQuote(ANONYMOUS_USER_ID, quoteId)).thenReturn(Mono.empty());
//            when(mockQuoteService.getQuoteById(quoteId)).thenReturn(Mono.just(Optional.empty()));
//
//            // Act & Assert: Perform GET request to like, expect 404 because the quote isn't found afterwards.
//            webTestClient.get().uri("/api/v1/quotes/{id}/like", quoteId)
//                    .accept(MediaType.APPLICATION_JSON)
//                    .exchange()
//                    .expectStatus().isNotFound()
//                    .expectBody().isEmpty();
//
//            // Verify service calls: Check which services were called before the 404 occurred.
//            verify(mockAnonymousUserService, times(1)).getOrCreateAnonymousUserId(any(), any());
//            verify(mockQuoteLikeService, times(1)).likeQuote(ANONYMOUS_USER_ID, quoteId);
//            verify(mockQuoteService, times(1)).getQuoteById(quoteId);
//            verifyNoMoreInteractions(mockQuoteLikeService); // checkUserLike should not be called
//            verifyNoInteractions(quoteMapper);            // Mapper shouldn't be called
//        }
//    }
//
//    // --- Tests for DELETE /{id}/like ---
//    @Nested
//    @DisplayName("DELETE /api/v1/quotes/{id}/like")
//    class UnlikeQuoteTests {
//
//        @Test
//        @DisplayName("should return 200 OK with updated quote (not liked) after successful unlike operation")
//        void unlikeQuote_success() {
//            // Arrange: Mock successful unliking, subsequent quote retrieval, and checking the new (not liked) status.
//            // Prepare the expected response DTO reflecting the 'not liked' state.
//            Long quoteId = testQuote.getId();
//            testQuoteResponse.setIsLiked(false);
//
//            when(mockQuoteLikeService.unlikeQuote(ANONYMOUS_USER_ID, quoteId)).thenReturn(Mono.empty());
//            when(mockQuoteService.getQuoteById(quoteId)).thenReturn(Mono.just(Optional.of(testQuote)));
//            when(mockQuoteLikeService.checkUserLike(ANONYMOUS_USER_ID, quoteId)).thenReturn(Mono.just(false));
//            // Mock the mapper behavior for this specific test case
//            when(quoteMapper.toQuoteResponse(testQuote)).thenReturn(testQuoteResponse);
//
//
//            // Act & Assert: Perform DELETE request to unlike the quote and verify the successful response.
//            webTestClient.delete().uri("/api/v1/quotes/{id}/like", quoteId)
//                    .accept(MediaType.APPLICATION_JSON)
//                    .exchange()
//                    .expectStatus().isOk()
//                    .expectBody(QuoteResponse.class)
//                    .isEqualTo(testQuoteResponse);
//
//            // Verify service calls: Ensure expected service methods were invoked in the correct sequence.
//            verify(mockAnonymousUserService, times(1)).getOrCreateAnonymousUserId(any(), any());
//            verify(mockQuoteLikeService, times(1)).unlikeQuote(ANONYMOUS_USER_ID, quoteId);
//            verify(mockQuoteService, times(1)).getQuoteById(quoteId);
//            verify(mockQuoteLikeService, times(1)).checkUserLike(ANONYMOUS_USER_ID, quoteId);
//            verify(quoteMapper, times(1)).toQuoteResponse(testQuote); // Verify mapper interaction
//        }
//
//        @Test
//        @DisplayName("should return 404 Not Found if quote doesn't exist when trying to fetch it after unlike attempt")
//        void unlikeQuote_quoteNotFoundAfterUnlike() {
//            // Arrange: Mock successful unliking but mock quote retrieval to return empty optional.
//            Long quoteId = 999L; // Use an ID assumed not to exist
//            when(mockQuoteLikeService.unlikeQuote(ANONYMOUS_USER_ID, quoteId)).thenReturn(Mono.empty());
//            when(mockQuoteService.getQuoteById(quoteId)).thenReturn(Mono.just(Optional.empty()));
//
//            // Act & Assert: Perform DELETE request to unlike, expect 404 because the quote isn't found afterwards.
//            webTestClient.delete().uri("/api/v1/quotes/{id}/like", quoteId)
//                    .accept(MediaType.APPLICATION_JSON)
//                    .exchange()
//                    .expectStatus().isNotFound()
//                    .expectBody().isEmpty();
//
//            // Verify service calls: Check which services were called before the 404 occurred.
//            verify(mockAnonymousUserService, times(1)).getOrCreateAnonymousUserId(any(), any());
//            verify(mockQuoteLikeService, times(1)).unlikeQuote(ANONYMOUS_USER_ID, quoteId);
//            verify(mockQuoteService, times(1)).getQuoteById(quoteId);
//            verifyNoMoreInteractions(mockQuoteLikeService); // checkUserLike should not be called
//            verifyNoInteractions(quoteMapper);            // Mapper shouldn't be called
//        }
//    }
//}