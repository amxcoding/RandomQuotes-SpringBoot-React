//package com.amxcoding.randomquotes.api.controllers;
//
//import com.amxcoding.randomquotes.api.interfaces.IAnonymousUserService;
//import com.amxcoding.randomquotes.api.mappers.quote.QuoteMapper;
//import com.amxcoding.randomquotes.api.models.quote.QuoteResponse;
//import com.amxcoding.randomquotes.application.interfaces.services.IQuoteLikeService;
//import com.amxcoding.randomquotes.application.interfaces.services.IQuoteService;
//import com.amxcoding.randomquotes.domain.entities.Quote;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Nested;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.http.server.reactive.ServerHttpRequest;
//import org.springframework.http.server.reactive.ServerHttpResponse;
//import reactor.core.publisher.Mono;
//
//import java.time.LocalDateTime;
//import java.util.Optional;
//
//import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//@DisplayName("QuoteController Tests")
//class QuoteControllerTest {
//
//    @Mock
//    private IQuoteService quoteService;
//    @Mock
//    private IQuoteLikeService quoteLikeService;
//    @Mock
//    private QuoteMapper quoteMapper;
//    @Mock
//    private IAnonymousUserService anonymousUserService;
//
//    @Mock
//    private ServerHttpRequest request; // Mock request
//    @Mock
//    private ServerHttpResponse response; // Mock response
//
//    @InjectMocks
//    private QuoteController quoteController;
//
//    private static final String TEST_USER_ID = "anon-user-123";
//    private static final Long TEST_QUOTE_ID = 1L;
//    private Quote testQuote;
//    private QuoteResponse testQuoteResponse;
//
//    @BeforeEach
//    void setUp() {
//        testQuote = new Quote();
//        testQuote.setId(TEST_QUOTE_ID);
//        testQuote.setText("This is a test quote.");
//        testQuote.setAuthor("Test Author");
//
//        testQuoteResponse = new QuoteResponse();
//        testQuoteResponse.setId(TEST_QUOTE_ID);
//        testQuoteResponse.setText("This is a test quote.");
//        testQuoteResponse.setAuthor("Test Author");
//        // isLiked will be set per test case
//    }
//
//    // --- Helper method for verifying response ---
//    private void verifyOkResponse(Mono<ResponseEntity<QuoteResponse>> resultMono, QuoteResponse expectedBody, Boolean expectedLikedStatus) {
//        StepVerifier.create(resultMono)
//                .assertNext(responseEntity -> {
//                    assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
//                    assertThat(responseEntity.getBody()).isNotNull();
//                    assertThat(responseEntity.getBody().getId()).isEqualTo(expectedBody.getId());
//                    assertThat(responseEntity.getBody().getText()).isEqualTo(expectedBody.getText());
//                    assertThat(responseEntity.getBody().getAuthor()).isEqualTo(expectedBody.getAuthor());
//                    assertThat(responseEntity.getBody().getIsLiked()).isEqualTo(expectedLikedStatus);
//                })
//                .verifyComplete();
//    }
//
//    // --- Helper method for verifying not found response ---
//    private void verifyNotFoundResponse(Mono<ResponseEntity<QuoteResponse>> resultMono) {
//        StepVerifier.create(resultMono)
//                .assertNext(responseEntity -> {
//                    assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
//                    assertThat(responseEntity.getBody()).isNull();
//                })
//                .verifyComplete();
//    }
//
//    @Nested
//    @DisplayName("GET /random")
//    class GetRandomQuoteTests {
//
//        @Test
//        @DisplayName("Should return OK with quote and isLiked=false when quote found and user has not liked it")
//        void getRandomQuote_whenQuoteFoundAndNotLiked_shouldReturnOkAndLikedFalse() {
//            // Arrange
//            when(anonymousUserService.getOrCreateAnonymousUserId(request, response)).thenReturn(TEST_USER_ID);
//            when(quoteService.getRandomQuote()).thenReturn(Mono.just(Optional.of(testQuote)));
//            when(quoteLikeService.checkUserLike(TEST_USER_ID, TEST_QUOTE_ID)).thenReturn(Mono.just(false));
//            when(quoteMapper.toQuoteResponse(testQuote)).thenReturn(testQuoteResponse); // isLiked is set later
//
//            // Act
//            Mono<ResponseEntity<QuoteResponse>> result = quoteController.getRandomQuote(request, response);
//
//            // Assert
//            verifyOkResponse(result, testQuoteResponse, false);
//            verify(anonymousUserService).getOrCreateAnonymousUserId(request, response);
//            verify(quoteService).getRandomQuote();
//            verify(quoteLikeService).checkUserLike(TEST_USER_ID, TEST_QUOTE_ID);
//            verify(quoteMapper).toQuoteResponse(testQuote);
//        }
//
//        @Test
//        @DisplayName("Should return OK with quote and isLiked=true when quote found and user has liked it")
//        void getRandomQuote_whenQuoteFoundAndLiked_shouldReturnOkAndLikedTrue() {
//            // Arrange
//            when(anonymousUserService.getOrCreateAnonymousUserId(request, response)).thenReturn(TEST_USER_ID);
//            when(quoteService.getRandomQuote()).thenReturn(Mono.just(Optional.of(testQuote)));
//            when(quoteLikeService.checkUserLike(TEST_USER_ID, TEST_QUOTE_ID)).thenReturn(Mono.just(true));
//            when(quoteMapper.toQuoteResponse(testQuote)).thenReturn(testQuoteResponse); // isLiked is set later
//
//            // Act
//            Mono<ResponseEntity<QuoteResponse>> result = quoteController.getRandomQuote(request, response);
//
//            // Assert
//            verifyOkResponse(result, testQuoteResponse, true);
//            verify(anonymousUserService).getOrCreateAnonymousUserId(request, response);
//            verify(quoteService).getRandomQuote();
//            verify(quoteLikeService).checkUserLike(TEST_USER_ID, TEST_QUOTE_ID);
//            verify(quoteMapper).toQuoteResponse(testQuote);
//        }
//
//        @Test
//        @DisplayName("Should return Not Found when quote service returns empty Optional")
//        void getRandomQuote_whenQuoteNotFound_shouldReturnNotFound() {
//            // Arrange
//            when(anonymousUserService.getOrCreateAnonymousUserId(request, response)).thenReturn(TEST_USER_ID);
//            when(quoteService.getRandomQuote()).thenReturn(Mono.just(Optional.empty()));
//
//            // Act
//            Mono<ResponseEntity<QuoteResponse>> result = quoteController.getRandomQuote(request, response);
//
//            // Assert
//            verifyNotFoundResponse(result);
//            verify(anonymousUserService).getOrCreateAnonymousUserId(request, response);
//            verify(quoteService).getRandomQuote();
//            verifyNoInteractions(quoteLikeService); // Should not be called if quote not found
//            verifyNoInteractions(quoteMapper);     // Should not be called if quote not found
//        }
//
//        @Test
//        @DisplayName("Should return Not Found when quote service returns empty Mono")
//        void getRandomQuote_whenQuoteServiceReturnsEmptyMono_shouldReturnNotFound() {
//            // Arrange
//            when(anonymousUserService.getOrCreateAnonymousUserId(request, response)).thenReturn(TEST_USER_ID);
//            when(quoteService.getRandomQuote()).thenReturn(Mono.empty());
//
//            // Act
//            Mono<ResponseEntity<QuoteResponse>> result = quoteController.getRandomQuote(request, response);
//
//            // Assert
//            verifyNotFoundResponse(result); // defaultIfEmpty in fetchQuoteAndSetUserLike handles this
//            verify(anonymousUserService).getOrCreateAnonymousUserId(request, response);
//            verify(quoteService).getRandomQuote();
//            verifyNoInteractions(quoteLikeService);
//            verifyNoInteractions(quoteMapper);
//        }
//
//        @Test
//        @DisplayName("Should propagate error when quote service fails")
//        void getRandomQuote_whenQuoteServiceFails_shouldPropagateError() {
//            // Arrange
//            RuntimeException testException = new RuntimeException("Quote service failed");
//            when(anonymousUserService.getOrCreateAnonymousUserId(request, response)).thenReturn(TEST_USER_ID);
//            when(quoteService.getRandomQuote()).thenReturn(Mono.error(testException));
//
//            // Act
//            Mono<ResponseEntity<QuoteResponse>> result = quoteController.getRandomQuote(request, response);
//
//            // Assert
//            StepVerifier.create(result)
//                    .expectErrorMatches(throwable -> throwable == testException)
//                    .verify();
//            verify(anonymousUserService).getOrCreateAnonymousUserId(request, response);
//            verify(quoteService).getRandomQuote();
//            verifyNoInteractions(quoteLikeService);
//            verifyNoInteractions(quoteMapper);
//        }
//
//        @Test
//        @DisplayName("Should propagate error when like check service fails")
//        void getRandomQuote_whenLikeCheckServiceFails_shouldPropagateError() {
//            // Arrange
//            RuntimeException testException = new RuntimeException("Like service failed");
//            when(anonymousUserService.getOrCreateAnonymousUserId(request, response)).thenReturn(TEST_USER_ID);
//            when(quoteService.getRandomQuote()).thenReturn(Mono.just(Optional.of(testQuote)));
//            when(quoteLikeService.checkUserLike(TEST_USER_ID, TEST_QUOTE_ID)).thenReturn(Mono.error(testException));
//            // Mapper might not be called depending on exact error point, but mock it just in case
//            // when(quoteMapper.toQuoteResponse(testQuote)).thenReturn(testQuoteResponse);
//
//
//            // Act
//            Mono<ResponseEntity<QuoteResponse>> result = quoteController.getRandomQuote(request, response);
//
//            // Assert
//            StepVerifier.create(result)
//                    .expectErrorMatches(throwable -> throwable == testException)
//                    .verify();
//            verify(anonymousUserService).getOrCreateAnonymousUserId(request, response);
//            verify(quoteService).getRandomQuote();
//            verify(quoteLikeService).checkUserLike(TEST_USER_ID, TEST_QUOTE_ID);
//            verifyNoInteractions(quoteMapper); // Error happens before mapping
//        }
//    }
//
//    @Nested
//    @DisplayName("GET /{id}/like")
//    class LikeQuoteTests {
//
//        @Test
//        @DisplayName("Should return OK with updated quote (isLiked=true) after successful like")
//        void likeQuote_whenSuccessful_shouldReturnOkAndLikedTrue() {
//            // Arrange
//            when(anonymousUserService.getOrCreateAnonymousUserId(request, response)).thenReturn(TEST_USER_ID);
//            when(quoteLikeService.likeQuote(TEST_USER_ID, TEST_QUOTE_ID)).thenReturn(Mono.empty()); // Successful completion
//            when(quoteService.getQuoteById(TEST_QUOTE_ID)).thenReturn(Mono.just(Optional.of(testQuote)));
//            when(quoteLikeService.checkUserLike(TEST_USER_ID, TEST_QUOTE_ID)).thenReturn(Mono.just(true)); // Assume like succeeded
//            when(quoteMapper.toQuoteResponse(testQuote)).thenReturn(testQuoteResponse);
//
//            // Act
//            Mono<ResponseEntity<QuoteResponse>> result = quoteController.likeQuoteByHash(TEST_QUOTE_ID, request, response);
//
//            // Assert
//            verifyOkResponse(result, testQuoteResponse, true);
//            verify(anonymousUserService).getOrCreateAnonymousUserId(request, response);
//            verify(quoteLikeService).likeQuote(TEST_USER_ID, TEST_QUOTE_ID);
//            verify(quoteService).getQuoteById(TEST_QUOTE_ID);
//            verify(quoteLikeService).checkUserLike(TEST_USER_ID, TEST_QUOTE_ID);
//            verify(quoteMapper).toQuoteResponse(testQuote);
//        }
//
//        @Test
//        @DisplayName("Should return Not Found when quote to like is not found after like attempt")
//        void likeQuote_whenQuoteNotFoundAfterLike_shouldReturnNotFound() {
//            // Arrange
//            when(anonymousUserService.getOrCreateAnonymousUserId(request, response)).thenReturn(TEST_USER_ID);
//            when(quoteLikeService.likeQuote(TEST_USER_ID, TEST_QUOTE_ID)).thenReturn(Mono.empty()); // Like attempt completes
//            when(quoteService.getQuoteById(TEST_QUOTE_ID)).thenReturn(Mono.just(Optional.empty())); // Quote not found
//
//            // Act
//            Mono<ResponseEntity<QuoteResponse>> result = quoteController.likeQuoteByHash(TEST_QUOTE_ID, request, response);
//
//            // Assert
//            verifyNotFoundResponse(result);
//            verify(anonymousUserService).getOrCreateAnonymousUserId(request, response);
//            verify(quoteLikeService).likeQuote(TEST_USER_ID, TEST_QUOTE_ID);
//            verify(quoteService).getQuoteById(TEST_QUOTE_ID);
//            verify(quoteLikeService, never()).checkUserLike(any(), any()); // CheckUserLike shouldn't be called
//            verifyNoInteractions(quoteMapper);
//        }
//
//        @Test
//        @DisplayName("Should propagate error when like service fails")
//        void likeQuote_whenLikeServiceFails_shouldPropagateError() {
//            // Arrange
//            RuntimeException testException = new RuntimeException("Like service failed");
//            when(anonymousUserService.getOrCreateAnonymousUserId(request, response)).thenReturn(TEST_USER_ID);
//            when(quoteLikeService.likeQuote(TEST_USER_ID, TEST_QUOTE_ID)).thenReturn(Mono.error(testException));
//
//            // Act
//            Mono<ResponseEntity<QuoteResponse>> result = quoteController.likeQuoteByHash(TEST_QUOTE_ID, request, response);
//
//            // Assert
//            StepVerifier.create(result)
//                    .expectErrorMatches(throwable -> throwable == testException)
//                    .verify();
//            verify(anonymousUserService).getOrCreateAnonymousUserId(request, response);
//            verify(quoteLikeService).likeQuote(TEST_USER_ID, TEST_QUOTE_ID);
//            verifyNoInteractions(quoteService); // Should not proceed after error
//            verify(quoteLikeService, never()).checkUserLike(any(), any());
//            verifyNoInteractions(quoteMapper);
//        }
//
//        @Test
//        @DisplayName("Should propagate error when quote service fails after successful like")
//        void likeQuote_whenQuoteServiceFailsAfterLike_shouldPropagateError() {
//            // Arrange
//            RuntimeException testException = new RuntimeException("Quote service failed");
//            when(anonymousUserService.getOrCreateAnonymousUserId(request, response)).thenReturn(TEST_USER_ID);
//            when(quoteLikeService.likeQuote(TEST_USER_ID, TEST_QUOTE_ID)).thenReturn(Mono.empty()); // Like completes
//            when(quoteService.getQuoteById(TEST_QUOTE_ID)).thenReturn(Mono.error(testException)); // Then quote fetch fails
//
//            // Act
//            Mono<ResponseEntity<QuoteResponse>> result = quoteController.likeQuoteByHash(TEST_QUOTE_ID, request, response);
//
//            // Assert
//            StepVerifier.create(result)
//                    .expectErrorMatches(throwable -> throwable == testException)
//                    .verify();
//            verify(anonymousUserService).getOrCreateAnonymousUserId(request, response);
//            verify(quoteLikeService).likeQuote(TEST_USER_ID, TEST_QUOTE_ID);
//            verify(quoteService).getQuoteById(TEST_QUOTE_ID);
//            verify(quoteLikeService, never()).checkUserLike(any(), any()); // CheckUserLike shouldn't be called
//            verifyNoInteractions(quoteMapper);
//        }
//    }
//
//    @Nested
//    @DisplayName("DELETE /{id}/like")
//    class UnlikeQuoteTests {
//
//        @Test
//        @DisplayName("Should return OK with updated quote (isLiked=false) after successful unlike")
//        void unlikeQuote_whenSuccessful_shouldReturnOkAndLikedFalse() {
//            // Arrange
//            when(anonymousUserService.getOrCreateAnonymousUserId(request, response)).thenReturn(TEST_USER_ID);
//            when(quoteLikeService.unlikeQuote(TEST_USER_ID, TEST_QUOTE_ID)).thenReturn(Mono.empty()); // Successful completion
//            when(quoteService.getQuoteById(TEST_QUOTE_ID)).thenReturn(Mono.just(Optional.of(testQuote)));
//            when(quoteLikeService.checkUserLike(TEST_USER_ID, TEST_QUOTE_ID)).thenReturn(Mono.just(false)); // Assume unlike succeeded
//            when(quoteMapper.toQuoteResponse(testQuote)).thenReturn(testQuoteResponse);
//
//            // Act
//            Mono<ResponseEntity<QuoteResponse>> result = quoteController.unlikeQuoteByHash(TEST_QUOTE_ID, request, response);
//
//            // Assert
//            verifyOkResponse(result, testQuoteResponse, false);
//            verify(anonymousUserService).getOrCreateAnonymousUserId(request, response);
//            verify(quoteLikeService).unlikeQuote(TEST_USER_ID, TEST_QUOTE_ID);
//            verify(quoteService).getQuoteById(TEST_QUOTE_ID);
//            verify(quoteLikeService).checkUserLike(TEST_USER_ID, TEST_QUOTE_ID);
//            verify(quoteMapper).toQuoteResponse(testQuote);
//        }
//
//        @Test
//        @DisplayName("Should return Not Found when quote to unlike is not found after unlike attempt")
//        void unlikeQuote_whenQuoteNotFoundAfterUnlike_shouldReturnNotFound() {
//            // Arrange
//            when(anonymousUserService.getOrCreateAnonymousUserId(request, response)).thenReturn(TEST_USER_ID);
//            when(quoteLikeService.unlikeQuote(TEST_USER_ID, TEST_QUOTE_ID)).thenReturn(Mono.empty()); // Unlike attempt completes
//            when(quoteService.getQuoteById(TEST_QUOTE_ID)).thenReturn(Mono.just(Optional.empty())); // Quote not found
//
//            // Act
//            Mono<ResponseEntity<QuoteResponse>> result = quoteController.unlikeQuoteByHash(TEST_QUOTE_ID, request, response);
//
//            // Assert
//            verifyNotFoundResponse(result);
//            verify(anonymousUserService).getOrCreateAnonymousUserId(request, response);
//            verify(quoteLikeService).unlikeQuote(TEST_USER_ID, TEST_QUOTE_ID);
//            verify(quoteService).getQuoteById(TEST_QUOTE_ID);
//            verify(quoteLikeService, never()).checkUserLike(any(), any());
//            verifyNoInteractions(quoteMapper);
//        }
//
//        @Test
//        @DisplayName("Should propagate error when unlike service fails")
//        void unlikeQuote_whenUnlikeServiceFails_shouldPropagateError() {
//            // Arrange
//            RuntimeException testException = new RuntimeException("Unlike service failed");
//            when(anonymousUserService.getOrCreateAnonymousUserId(request, response)).thenReturn(TEST_USER_ID);
//            when(quoteLikeService.unlikeQuote(TEST_USER_ID, TEST_QUOTE_ID)).thenReturn(Mono.error(testException));
//
//            // Act
//            Mono<ResponseEntity<QuoteResponse>> result = quoteController.unlikeQuoteByHash(TEST_QUOTE_ID, request, response);
//
//            // Assert
//            StepVerifier.create(result)
//                    .expectErrorMatches(throwable -> throwable == testException)
//                    .verify();
//            verify(anonymousUserService).getOrCreateAnonymousUserId(request, response);
//            verify(quoteLikeService).unlikeQuote(TEST_USER_ID, TEST_QUOTE_ID);
//            verifyNoInteractions(quoteService); // Should not proceed after error
//            verify(quoteLikeService, never()).checkUserLike(any(), any());
//            verifyNoInteractions(quoteMapper);
//        }
//
//        @Test
//        @DisplayName("Should propagate error when quote service fails after successful unlike")
//        void unlikeQuote_whenQuoteServiceFailsAfterUnlike_shouldPropagateError() {
//            // Arrange
//            RuntimeException testException = new RuntimeException("Quote service failed");
//            when(anonymousUserService.getOrCreateAnonymousUserId(request, response)).thenReturn(TEST_USER_ID);
//            when(quoteLikeService.unlikeQuote(TEST_USER_ID, TEST_QUOTE_ID)).thenReturn(Mono.empty()); // Unlike completes
//            when(quoteService.getQuoteById(TEST_QUOTE_ID)).thenReturn(Mono.error(testException)); // Then quote fetch fails
//
//            // Act
//            Mono<ResponseEntity<QuoteResponse>> result = quoteController.unlikeQuoteByHash(TEST_QUOTE_ID, request, response);
//
//            // Assert
//            StepVerifier.create(result)
//                    .expectErrorMatches(throwable -> throwable == testException)
//                    .verify();
//            verify(anonymousUserService).getOrCreateAnonymousUserId(request, response);
//            verify(quoteLikeService).unlikeQuote(TEST_USER_ID, TEST_QUOTE_ID);
//            verify(quoteService).getQuoteById(TEST_QUOTE_ID);
//            verify(quoteLikeService, never()).checkUserLike(any(), any());
//            verifyNoInteractions(quoteMapper);
//        }
//    }
//}