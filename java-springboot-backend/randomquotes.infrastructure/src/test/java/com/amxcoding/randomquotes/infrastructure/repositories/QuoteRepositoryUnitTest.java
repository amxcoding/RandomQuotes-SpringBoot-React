package com.amxcoding.randomquotes.infrastructure.repositories;

import com.amxcoding.randomquotes.application.exceptions.repositories.QuotePersistenceException;
import com.amxcoding.randomquotes.domain.entities.Quote;
import com.amxcoding.randomquotes.infrastructure.persistence.mappers.QuoteEntityMapper;
import com.amxcoding.randomquotes.infrastructure.persistence.models.QuoteEntity;
import com.amxcoding.randomquotes.infrastructure.persistence.r2dbcs.IQuoteR2dbcRepository;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.r2dbc.core.FetchSpec;
import org.springframework.r2dbc.core.RowsFetchSpec;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("QuoteRepositoryUnitTests")
class QuoteRepositoryUnitTest {

    @Mock
    DatabaseClient databaseClient;
    @Mock
    IQuoteR2dbcRepository quoteR2dbcRepository;
    @Mock
    QuoteEntityMapper quoteEntityMapper;

    @Mock
    DatabaseClient.GenericExecuteSpec mockGenericExecuteSpec;
    @Mock
    FetchSpec<Map<String, Object>> mockFetchSpecMap; // For rowsUpdated() etc.
    @Mock
    RowsFetchSpec<QuoteEntity> mockRowsFetchSpec; // Mock for the .map(mappingFunction).all() chain result

    @InjectMocks
    QuoteRepository quoteRepository;

    private Quote domainQuote1;
    private Quote domainQuote2;
    private QuoteEntity entityQuote1;
    private QuoteEntity entityQuote2;
    private String hash1;
    private String hash2;
    private final String PROVIDER_NAME = "testProvider";

    @BeforeEach
    void setUp() {
        domainQuote1 = new Quote(1L, "Author One", " Text One ", 5);
        domainQuote2 = new Quote(2L, "author two", "text two", 0);

        hash1 = domainQuote1.generateTextAuthorHash();
        hash2 = domainQuote2.generateTextAuthorHash();

        entityQuote1 = new QuoteEntity(1L, "Author One", " Text One ", 5, hash1, PROVIDER_NAME);
        entityQuote2 = new QuoteEntity(2L, "author two", "text two", 0, hash2, PROVIDER_NAME);
    }

    @Nested
    @DisplayName("bulkInsertQuotesIgnoreConflicts Tests")
    class BulkInsertTests {

        @Test
        @DisplayName("1. Should return empty Mono when input list is empty")
        void bulkInsert_emptyList() {
            // Arrange: Prepare the empty list input
            List<Quote> emptyList = Collections.emptyList();

            // Act: Execute the method under test
            Mono<Void> resultMono = quoteRepository.bulkInsertQuotesIgnoreConflicts(emptyList, PROVIDER_NAME);

            // Assert: Verify the outcome (empty Mono) and no interactions with dependencies
            StepVerifier.create(resultMono).verifyComplete();
            verifyNoInteractions(databaseClient, quoteEntityMapper, quoteR2dbcRepository);
        }

        @Test
        @DisplayName("2. Should bind and execute SQL correctly for multiple quotes")
        void bulkInsert_nonEmpty_success() {
            // Arrange: Setup mocks for DatabaseClient fluent API and QuoteEntityMapper
            when(databaseClient.sql(anyString())).thenReturn(mockGenericExecuteSpec);
            when(mockGenericExecuteSpec.bind(anyString(), any())).thenReturn(mockGenericExecuteSpec);
            when(mockGenericExecuteSpec.then()).thenReturn(Mono.empty());

            entityQuote1.setProvider(PROVIDER_NAME); // Ensure provider is set for verification
            entityQuote2.setProvider(PROVIDER_NAME);
            when(quoteEntityMapper.toQuoteEntity(domainQuote1)).thenReturn(entityQuote1);
            when(quoteEntityMapper.toQuoteEntity(domainQuote2)).thenReturn(entityQuote2);
            List<Quote> quotesToInsert = List.of(domainQuote1, domainQuote2);

            // Act: Execute the method under test
            Mono<Void> resultMono = quoteRepository.bulkInsertQuotesIgnoreConflicts(quotesToInsert, PROVIDER_NAME);

            // Assert: Verify successful completion and correct interactions
            StepVerifier.create(resultMono).verifyComplete();

            // Verify SQL structure was generated correctly
            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
            verify(databaseClient).sql(sqlCaptor.capture());
            assertThat(sqlCaptor.getValue()).startsWith("INSERT INTO quotes (author, text, likes, text_author_hash, provider) VALUES");
            assertThat(sqlCaptor.getValue()).contains("(:author0, :text0, :likes0, :hash0, :provider0)");
            assertThat(sqlCaptor.getValue()).contains("(:author1, :text1, :likes1, :hash1, :provider1)");
            assertThat(sqlCaptor.getValue()).endsWith("ON CONFLICT (text_author_hash, provider) DO NOTHING");

            // Verify all parameters were bound correctly (5 per quote * 2 quotes = 10 bindings)
            verify(mockGenericExecuteSpec, times(10)).bind(anyString(), any());
            verify(mockGenericExecuteSpec).bind("author0", entityQuote1.getAuthor());
            verify(mockGenericExecuteSpec).bind("text0", entityQuote1.getText());
            verify(mockGenericExecuteSpec).bind("likes0", entityQuote1.getLikes());
            verify(mockGenericExecuteSpec).bind("hash0", entityQuote1.getTextAuthorHash());
            verify(mockGenericExecuteSpec).bind("provider0", entityQuote1.getProvider()); // Uses providerName passed to method
            verify(mockGenericExecuteSpec).bind("author1", entityQuote2.getAuthor());
            verify(mockGenericExecuteSpec).bind("text1", entityQuote2.getText());
            verify(mockGenericExecuteSpec).bind("likes1", entityQuote2.getLikes());
            verify(mockGenericExecuteSpec).bind("hash1", entityQuote2.getTextAuthorHash());
            verify(mockGenericExecuteSpec).bind("provider1", entityQuote2.getProvider()); // Uses providerName passed to method

            // Verify the final execution step
            verify(mockGenericExecuteSpec).then();
        }

        @Test
        @DisplayName("3. Should wrap database errors in QuotePersistenceException")
        void bulkInsert_dbError() {
            // Arrange: Setup mocks to simulate a database error during execution
            DataAccessResourceFailureException dbError = new DataAccessResourceFailureException("DB connection failed");
            when(databaseClient.sql(anyString())).thenReturn(mockGenericExecuteSpec);
            when(mockGenericExecuteSpec.bind(anyString(), any())).thenReturn(mockGenericExecuteSpec);
            when(mockGenericExecuteSpec.then()).thenReturn(Mono.error(dbError)); // Simulate error on .then()

            entityQuote1.setProvider(PROVIDER_NAME);
            when(quoteEntityMapper.toQuoteEntity(domainQuote1)).thenReturn(entityQuote1);
            List<Quote> quotesToInsert = List.of(domainQuote1);

            // Act: Execute the method under test
            Mono<Void> resultMono = quoteRepository.bulkInsertQuotesIgnoreConflicts(quotesToInsert, PROVIDER_NAME);

            // Assert: Verify the expected exception wrapping occurs
            StepVerifier.create(resultMono)
                    .expectErrorSatisfies(ex -> {
                        assertThat(ex).isInstanceOf(QuotePersistenceException.class)
                                .hasMessageContaining("Error during bulk quote insert from provider " + PROVIDER_NAME)
                                .hasCause(dbError);
                    })
                    .verify();

            // Verify interactions happened up to the point of error
            verify(databaseClient).sql(anyString());
            verify(mockGenericExecuteSpec, times(5)).bind(anyString(), any()); // 5 bindings for one quote
            verify(mockGenericExecuteSpec).then();
        }
    }


    @Nested
    @DisplayName("findByTextAuthorHash Tests")
    class FindByHashTests {

        @Test
        @DisplayName("1. Should map entity to domain and wrap in Optional when found")
        void findByTextAuthorHash_whenFound_shouldReturnMappedDomainObjectInOptional() {
            // Arrange: Mock repository and mapper for the successful find case
            when(quoteR2dbcRepository.findByTextAuthorHash(hash1)).thenReturn(Mono.just(entityQuote1));
            when(quoteEntityMapper.toQuote(entityQuote1)).thenReturn(domainQuote1);

            // Act: Execute the method under test
            Mono<Optional<Quote>> resultMono = quoteRepository.findByTextAuthorHash(hash1);

            // Assert: Verify the correct Optional<Quote> is emitted
            StepVerifier.create(resultMono)
                    .expectNext(Optional.of(domainQuote1))
                    .verifyComplete();
            verify(quoteR2dbcRepository).findByTextAuthorHash(hash1);
            verify(quoteEntityMapper).toQuote(entityQuote1);
        }

        @Test
        @DisplayName("2. Should return empty Optional when quote is not found")
        void findByTextAuthorHash_whenNotFound_shouldReturnEmptyOptional() {
            // Arrange: Mock repository to return empty, simulating not found
            String unknownHash = "unknownHashValue";
            when(quoteR2dbcRepository.findByTextAuthorHash(unknownHash)).thenReturn(Mono.empty());

            // Act: Execute the method under test
            Mono<Optional<Quote>> resultMono = quoteRepository.findByTextAuthorHash(unknownHash);

            // Assert: Verify an empty Optional is emitted
            StepVerifier.create(resultMono)
                    .expectNext(Optional.empty())
                    .verifyComplete();
            verify(quoteR2dbcRepository).findByTextAuthorHash(unknownHash);
            verifyNoInteractions(quoteEntityMapper); // Mapper not called if nothing found
        }

        @Test
        @DisplayName("3. Should wrap repository errors in QuotePersistenceException")
        void findByTextAuthorHash_whenRepositoryThrowsError_shouldWrapInPersistenceException() {
            // Arrange: Mock repository to throw an error
            DataAccessResourceFailureException dbError = new DataAccessResourceFailureException("DB connection failed");
            when(quoteR2dbcRepository.findByTextAuthorHash(hash1)).thenReturn(Mono.error(dbError));

            // Act: Execute the method under test
            Mono<Optional<Quote>> resultMono = quoteRepository.findByTextAuthorHash(hash1);

            // Assert: Verify the specific persistence exception is thrown, wrapping the original DB error
            StepVerifier.create(resultMono)
                    .expectErrorSatisfies(throwable -> {
                        assertThat(throwable)
                                .isInstanceOf(QuotePersistenceException.class)
                                .hasMessageContaining("Error getting quote by textAuthorHash")
                                .hasCause(dbError);
                    })
                    .verify();
            verify(quoteR2dbcRepository).findByTextAuthorHash(hash1);
            verifyNoInteractions(quoteEntityMapper); // Mapper not called on error
        }
    }


    @Nested
    @DisplayName("findById Tests")
    class FindByIdTests {

        @Test
        @DisplayName("1. Should map entity to domain and wrap in Optional when found")
        void findById_whenFound_shouldReturnMappedDomainObjectInOptional() {
            // Arrange: Mock repository and mapper for a successful find
            Long quoteId = 1L;
            when(quoteR2dbcRepository.findById(quoteId)).thenReturn(Mono.just(entityQuote1));
            when(quoteEntityMapper.toQuote(entityQuote1)).thenReturn(domainQuote1);

            // Act: Execute the method under test
            Mono<Optional<Quote>> resultMono = quoteRepository.findById(quoteId);

            // Assert: Verify the correct Optional<Quote> is returned
            StepVerifier.create(resultMono)
                    .expectNext(Optional.of(domainQuote1))
                    .verifyComplete();
            verify(quoteR2dbcRepository).findById(quoteId);
            verify(quoteEntityMapper).toQuote(entityQuote1);
        }

        @Test
        @DisplayName("2. Should return empty Optional when quote is not found")
        void findById_whenNotFound_shouldReturnEmptyOptional() {
            // Arrange: Mock repository to return empty Mono for a non-existent ID
            Long nonExistentId = 999L;
            when(quoteR2dbcRepository.findById(nonExistentId)).thenReturn(Mono.empty());

            // Act: Execute the method under test
            Mono<Optional<Quote>> resultMono = quoteRepository.findById(nonExistentId);

            // Assert: Verify an empty Optional is returned
            StepVerifier.create(resultMono)
                    .expectNext(Optional.empty())
                    .verifyComplete();
            verify(quoteR2dbcRepository).findById(nonExistentId);
            verifyNoInteractions(quoteEntityMapper);
        }

        @Test
        @DisplayName("3. Should wrap repository errors in QuotePersistenceException")
        void findById_whenRepositoryThrowsError_shouldWrapInPersistenceException() {
            // Arrange: Mock repository to simulate a database error
            Long quoteId = 1L;
            DataAccessResourceFailureException dbError = new DataAccessResourceFailureException("DB connection failed");
            when(quoteR2dbcRepository.findById(quoteId)).thenReturn(Mono.error(dbError));

            // Act: Execute the method under test
            Mono<Optional<Quote>> resultMono = quoteRepository.findById(quoteId);

            // Assert: Verify the correct persistence exception wraps the original error
            StepVerifier.create(resultMono)
                    .expectErrorSatisfies(throwable -> {
                        assertThat(throwable)
                                .isInstanceOf(QuotePersistenceException.class)
                                .hasMessageContaining("Error finding quote by id: " + dbError.getMessage())
                                .hasCause(dbError);
                    })
                    .verify();
            verify(quoteR2dbcRepository).findById(quoteId);
            verifyNoInteractions(quoteEntityMapper);
        }
    }


    @Nested
    @DisplayName("incrementLikeCount Tests")
    class IncrementLikeTests {

        private final Long TEST_QUOTE_ID = 1L;
        private final Long NON_EXISTENT_QUOTE_ID = 999L;

        // Helper for common DatabaseClient mock setup in Arrange phase
        private void arrangeDatabaseClientCounterUpdateMocks() {
            when(databaseClient.sql(anyString())).thenReturn(mockGenericExecuteSpec);
            when(mockGenericExecuteSpec.bind(eq("quoteId"), anyLong())).thenReturn(mockGenericExecuteSpec);
            when(mockGenericExecuteSpec.fetch()).thenReturn(mockFetchSpecMap);
        }

        @Test
        @DisplayName("1. Should return true when update is successful (1 row affected)")
        void incrementLikeCount_whenSuccessful_shouldReturnTrue() {
            // Arrange: Setup mocks for a successful update (1 row affected)
            arrangeDatabaseClientCounterUpdateMocks();
            when(mockFetchSpecMap.rowsUpdated()).thenReturn(Mono.just(1L));

            // Act: Execute the increment method
            Mono<Boolean> resultMono = quoteRepository.incrementLikeCount(TEST_QUOTE_ID);

            // Assert: Verify true is returned and interactions are correct
            StepVerifier.create(resultMono)
                    .expectNext(true)
                    .verifyComplete();

            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
            verify(databaseClient).sql(sqlCaptor.capture());
            assertThat(sqlCaptor.getValue()).contains("UPDATE quotes SET likes = likes + 1 WHERE id = :quoteId");
            verify(mockGenericExecuteSpec).bind("quoteId", TEST_QUOTE_ID);
            verify(mockGenericExecuteSpec).fetch();
            verify(mockFetchSpecMap).rowsUpdated();
        }

        @Test
        @DisplayName("2. Should return false when no rows are updated (quote not found)")
        void incrementLikeCount_whenNoRowsUpdated_shouldReturnFalse() {
            // Arrange: Setup mocks for an update affecting 0 rows
            arrangeDatabaseClientCounterUpdateMocks();
            when(mockFetchSpecMap.rowsUpdated()).thenReturn(Mono.just(0L));

            // Act: Execute the increment method for a non-existent ID
            Mono<Boolean> resultMono = quoteRepository.incrementLikeCount(NON_EXISTENT_QUOTE_ID);

            // Assert: Verify false is returned and interactions are correct
            StepVerifier.create(resultMono)
                    .expectNext(false)
                    .verifyComplete();

            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
            verify(databaseClient).sql(sqlCaptor.capture());
            assertThat(sqlCaptor.getValue()).contains("UPDATE quotes SET likes = likes + 1 WHERE id = :quoteId");
            verify(mockGenericExecuteSpec).bind("quoteId", NON_EXISTENT_QUOTE_ID);
            verify(mockGenericExecuteSpec).fetch();
            verify(mockFetchSpecMap).rowsUpdated();
        }

        @Test
        @DisplayName("3. Should return IllegalArgumentException when quote ID is null")
        void incrementLikeCount_whenIdIsNull_shouldThrowIllegalArgumentException() {
            // Arrange: Prepare null input ID
            Long nullQuoteId = null;

            // Act: Execute the increment method with null ID
            Mono<Boolean> resultMono = quoteRepository.incrementLikeCount(nullQuoteId);

            // Assert: Verify the specific argument exception is thrown
            StepVerifier.create(resultMono)
                    .expectErrorSatisfies(throwable -> {
                        assertThat(throwable)
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessage("Quote ID cannot be null for incrementing likes.");
                    })
                    .verify();
            verifyNoInteractions(databaseClient); // No DB interaction expected
        }

        @Test
        @DisplayName("4. Should wrap database errors in QuotePersistenceException")
        void incrementLikeCount_whenDatabaseErrorOccurs_shouldWrapInPersistenceException() {
            // Arrange: Setup mocks to simulate a database error during the update
            arrangeDatabaseClientCounterUpdateMocks();
            DataAccessResourceFailureException dbError = new DataAccessResourceFailureException("Database connection failure during update");
            when(mockFetchSpecMap.rowsUpdated()).thenReturn(Mono.error(dbError));

            // Act: Execute the increment method
            Mono<Boolean> resultMono = quoteRepository.incrementLikeCount(TEST_QUOTE_ID);

            // Assert: Verify the correct persistence exception wraps the original DB error
            StepVerifier.create(resultMono)
                    .expectErrorSatisfies(throwable -> {
                        assertThat(throwable)
                                .isInstanceOf(QuotePersistenceException.class)
                                .hasMessageContaining("Failed to increment like count for quote " + TEST_QUOTE_ID)
                                .hasCause(dbError);
                    })
                    .verify();

            // Verify interactions happened up to the point of error
            verify(databaseClient).sql(anyString());
            verify(mockGenericExecuteSpec).bind("quoteId", TEST_QUOTE_ID);
            verify(mockGenericExecuteSpec).fetch();
            verify(mockFetchSpecMap).rowsUpdated();
        }
    }


    @Nested
    @DisplayName("decrementLikeCount Tests")
    class DecrementLikeTests {

        private final Long TEST_QUOTE_ID = 1L;
        private final Long NON_EXISTENT_QUOTE_ID = 999L;

        // Helper for common DatabaseClient mock setup in Arrange phase
        private void arrangeDatabaseClientCounterUpdateMocks() {
            when(databaseClient.sql(anyString())).thenReturn(mockGenericExecuteSpec);
            when(mockGenericExecuteSpec.bind(eq("quoteId"), anyLong())).thenReturn(mockGenericExecuteSpec);
            when(mockGenericExecuteSpec.fetch()).thenReturn(mockFetchSpecMap);
        }

        @Test
        @DisplayName("1. Should return true when update is successful (1 row affected)")
        void decrementLikeCount_whenSuccessful_shouldReturnTrue() {
            // Arrange: Setup mocks for a successful decrement
            arrangeDatabaseClientCounterUpdateMocks();
            when(mockFetchSpecMap.rowsUpdated()).thenReturn(Mono.just(1L));

            // Act: Execute the decrement method
            Mono<Boolean> resultMono = quoteRepository.decrementLikeCount(TEST_QUOTE_ID);

            // Assert: Verify true is returned and interactions/SQL are correct
            StepVerifier.create(resultMono)
                    .expectNext(true)
                    .verifyComplete();

            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
            verify(databaseClient).sql(sqlCaptor.capture());
            assertThat(sqlCaptor.getValue()).contains("UPDATE quotes SET likes = likes - 1 WHERE id = :quoteId AND likes > 0");
            verify(mockGenericExecuteSpec).bind("quoteId", TEST_QUOTE_ID);
            verify(mockGenericExecuteSpec).fetch();
            verify(mockFetchSpecMap).rowsUpdated();
        }

        @Test
        @DisplayName("2. Should return false when no rows are updated (quote not found or likes already 0)")
        void decrementLikeCount_whenNoRowsUpdated_shouldReturnFalse() {
            // Arrange: Setup mocks for decrement affecting 0 rows
            arrangeDatabaseClientCounterUpdateMocks();
            when(mockFetchSpecMap.rowsUpdated()).thenReturn(Mono.just(0L));

            // Act: Execute the decrement method for a non-existent/zero-like ID
            Mono<Boolean> resultMono = quoteRepository.decrementLikeCount(NON_EXISTENT_QUOTE_ID);

            // Assert: Verify false is returned and interactions/SQL are correct
            StepVerifier.create(resultMono)
                    .expectNext(false)
                    .verifyComplete();

            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
            verify(databaseClient).sql(sqlCaptor.capture());
            assertThat(sqlCaptor.getValue()).contains("UPDATE quotes SET likes = likes - 1 WHERE id = :quoteId AND likes > 0");
            verify(mockGenericExecuteSpec).bind("quoteId", NON_EXISTENT_QUOTE_ID);
            verify(mockGenericExecuteSpec).fetch();
            verify(mockFetchSpecMap).rowsUpdated();
        }

        @Test
        @DisplayName("3. Should return IllegalArgumentException when quote ID is null")
        void decrementLikeCount_whenIdIsNull_shouldThrowIllegalArgumentException() {
            // Arrange: Prepare null input ID
            Long nullQuoteId = null;

            // Act: Execute the decrement method with null ID
            Mono<Boolean> resultMono = quoteRepository.decrementLikeCount(nullQuoteId);

            // Assert: Verify the specific argument exception
            StepVerifier.create(resultMono)
                    .expectErrorSatisfies(throwable -> {
                        assertThat(throwable)
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessage("Quote ID cannot be null for decrementing likes.");
                    })
                    .verify();
            verifyNoInteractions(databaseClient); // No DB interaction expected
        }

        @Test
        @DisplayName("4. Should wrap database errors in QuotePersistenceException")
        void decrementLikeCount_whenDatabaseErrorOccurs_shouldWrapInPersistenceException() {
            // Arrange: Setup mocks to simulate DB error during decrement
            arrangeDatabaseClientCounterUpdateMocks();
            DataAccessResourceFailureException dbError = new DataAccessResourceFailureException("Database connection failure during update");
            when(mockFetchSpecMap.rowsUpdated()).thenReturn(Mono.error(dbError));

            // Act: Execute the decrement method
            Mono<Boolean> resultMono = quoteRepository.decrementLikeCount(TEST_QUOTE_ID);

            // Assert: Verify the correct persistence exception wraps the original error
            StepVerifier.create(resultMono)
                    .expectErrorSatisfies(throwable -> {
                        assertThat(throwable)
                                .isInstanceOf(QuotePersistenceException.class)
                                .hasMessageContaining("Failed to decrement like count for quote " + TEST_QUOTE_ID)
                                .hasCause(dbError);
                    })
                    .verify();

            // Verify interactions happened up to the point of error
            verify(databaseClient).sql(anyString());
            verify(mockGenericExecuteSpec).bind("quoteId", TEST_QUOTE_ID);
            verify(mockGenericExecuteSpec).fetch();
            verify(mockFetchSpecMap).rowsUpdated();
        }
    }


    @Nested
    @DisplayName("countByProvider Tests")
    class CountByProviderTests {

        private static final long EXPECTED_COUNT = 123L;

        @Test
        @DisplayName("1. Should return count from R2DBC repository when successful")
        void countByProvider_whenSuccessful_shouldReturnCount() {
            // Arrange: Mock the R2DBC repository to return a specific count
            when(quoteR2dbcRepository.countByProvider(PROVIDER_NAME)).thenReturn(Mono.just(EXPECTED_COUNT));

            // Act: Execute the countByProvider method
            Mono<Long> resultMono = quoteRepository.countByProvider(PROVIDER_NAME);

            // Assert: Verify the expected count is returned
            StepVerifier.create(resultMono)
                    .expectNext(EXPECTED_COUNT)
                    .verifyComplete();
            verify(quoteR2dbcRepository).countByProvider(PROVIDER_NAME);
        }

        @Test
        @DisplayName("2. Should return 0 when R2DBC repository returns empty Mono")
        void countByProvider_whenRepositoryReturnsEmpty_shouldReturnZero() {
            // Arrange: Mock the R2DBC repository to return empty (simulating no matching provider)
            when(quoteR2dbcRepository.countByProvider(PROVIDER_NAME)).thenReturn(Mono.empty());

            // Act: Execute the countByProvider method
            Mono<Long> resultMono = quoteRepository.countByProvider(PROVIDER_NAME);

            // Assert: Verify 0 is returned due to the switchIfEmpty operator
            StepVerifier.create(resultMono)
                    .expectNext(0L)
                    .verifyComplete();
            verify(quoteR2dbcRepository).countByProvider(PROVIDER_NAME);
        }

        @Test
        @DisplayName("3. Should wrap repository errors in QuotePersistenceException")
        void countByProvider_whenRepositoryThrowsError_shouldWrapInPersistenceException() {
            // Arrange: Mock the R2DBC repository to throw an error
            DataAccessResourceFailureException dbError = new DataAccessResourceFailureException("DB count operation failed");
            when(quoteR2dbcRepository.countByProvider(PROVIDER_NAME)).thenReturn(Mono.error(dbError));

            // Act: Execute the countByProvider method
            Mono<Long> resultMono = quoteRepository.countByProvider(PROVIDER_NAME);

            // Assert: Verify the correct persistence exception wraps the original error
            StepVerifier.create(resultMono)
                    .expectErrorSatisfies(throwable -> {
                        assertThat(throwable)
                                .isInstanceOf(QuotePersistenceException.class)
                                .hasMessageContaining("Database error counting quotes for provider " + PROVIDER_NAME)
                                .hasCause(dbError);
                    })
                    .verify();
            verify(quoteR2dbcRepository).countByProvider(PROVIDER_NAME);
        }
    }


    @Nested
    @DisplayName("findRandomQuotes Tests")
    class FindRandomTests {

        // Helper method for arranging the DatabaseClient mock chain for map().all() scenarios
        @SuppressWarnings("unchecked")
        private void arrangeDbClientMapAllChain(Flux<QuoteEntity> resultingEntityFlux) {
            // Mock the DatabaseClient fluent API chain: sql().bind().map().all()
            when(databaseClient.sql(anyString())).thenReturn(mockGenericExecuteSpec);
            when(mockGenericExecuteSpec.bind(eq("amount"), anyInt())).thenReturn(mockGenericExecuteSpec);
            ArgumentCaptor<BiFunction<Row, RowMetadata, QuoteEntity>> mapperCaptor = ArgumentCaptor.forClass(BiFunction.class);
            when(mockGenericExecuteSpec.map(mapperCaptor.capture())).thenReturn(mockRowsFetchSpec);
            when(mockRowsFetchSpec.all()).thenReturn(resultingEntityFlux);
        }

        @Test
        @DisplayName("1. Should return Optional of list when quotes are found")
        void findRandomQuotes_whenFound_shouldReturnMappedQuotesInOptionalList() {
            // Arrange: Setup mocks for finding and mapping random quotes
            int requestedAmount = 2;
            QuoteEntity randomEntity1 = new QuoteEntity(10L, "Random Author 1", "Random Text 1", 1, "hashR1", "providerR1");
            QuoteEntity randomEntity2 = new QuoteEntity(20L, "Random Author 2", "Random Text 2", 2, "hashR2", "providerR2");
            Flux<QuoteEntity> entityFlux = Flux.just(randomEntity1, randomEntity2);

            arrangeDbClientMapAllChain(entityFlux); // Setup DB client mock chain

            Quote mappedQuote1 = new Quote(10L, "Random Author 1", "Random Text 1", 1);
            Quote mappedQuote2 = new Quote(20L, "Random Author 2", "Random Text 2", 2);
            when(quoteEntityMapper.toQuote(randomEntity1)).thenReturn(mappedQuote1);
            when(quoteEntityMapper.toQuote(randomEntity2)).thenReturn(mappedQuote2);

            // Act: Execute the findRandomQuotes method
            Mono<Optional<List<Quote>>> resultMono = quoteRepository.findRandomQuotes(requestedAmount);

            // Assert: Verify the Optional contains the correctly mapped list of quotes
            StepVerifier.create(resultMono)
                    .assertNext(optionalList -> {
                        assertThat(optionalList).isPresent();
                        assertThat(optionalList.get())
                                .hasSize(requestedAmount)
                                .containsExactlyInAnyOrder(mappedQuote1, mappedQuote2);
                    })
                    .verifyComplete();

            // Verify SQL, binding, and mock interactions
            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
            verify(databaseClient).sql(sqlCaptor.capture());
            assertThat(sqlCaptor.getValue()).contains("SELECT id, author, text, likes, text_author_hash, provider FROM quotes ORDER BY RANDOM() LIMIT :amount");
            verify(mockGenericExecuteSpec).bind("amount", requestedAmount);
            verify(mockGenericExecuteSpec).map(any(BiFunction.class));
            verify(mockRowsFetchSpec).all();
            verify(quoteEntityMapper).toQuote(randomEntity1);
            verify(quoteEntityMapper).toQuote(randomEntity2);
        }

        @Test
        @DisplayName("2. Should return IllegalArgumentException when amount is zero")
        void findRandomQuotes_whenAmountIsZero_shouldThrowIllegalArgumentException() {
            // Arrange: Define zero amount input
            int zeroAmount = 0;

            // Act: Execute findRandomQuotes with zero amount
            Mono<Optional<List<Quote>>> resultMono = quoteRepository.findRandomQuotes(zeroAmount);

            // Assert: Verify the specific argument exception
            StepVerifier.create(resultMono)
                    .expectErrorSatisfies(throwable -> {
                        assertThat(throwable)
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessage("Amount cannot be 0");
                    })
                    .verify();
            verifyNoInteractions(databaseClient, quoteEntityMapper);
        }

        @Test
        @DisplayName("3. Should return IllegalArgumentException when amount is negative")
        void findRandomQuotes_whenAmountIsNegative_shouldThrowIllegalArgumentException() {
            // Arrange: Define negative amount input
            int negativeAmount = -5;

            // Act: Execute findRandomQuotes with negative amount
            Mono<Optional<List<Quote>>> resultMono = quoteRepository.findRandomQuotes(negativeAmount);

            // Assert: Verify the specific argument exception
            StepVerifier.create(resultMono)
                    .expectErrorSatisfies(throwable -> {
                        assertThat(throwable)
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessage("Amount cannot be 0");
                    })
                    .verify();
            verifyNoInteractions(databaseClient, quoteEntityMapper);
        }

        @Test
        @DisplayName("4. Should return Optional of empty list when database returns no rows")
        void findRandomQuotes_whenNoResults_shouldReturnOptionalOfEmptyList() {
            // Arrange: Setup DB client mock chain to return an empty Flux
            int requestedAmount = 5;
            arrangeDbClientMapAllChain(Flux.empty());

            // Act: Execute findRandomQuotes
            Mono<Optional<List<Quote>>> resultMono = quoteRepository.findRandomQuotes(requestedAmount);

            // Assert: Verify the result is an Optional containing an empty list
            StepVerifier.create(resultMono)
                    .expectNext(Optional.of(Collections.emptyList()))
                    .verifyComplete();

            // Verify interactions: SQL executed, but mapper not called
            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
            verify(databaseClient).sql(sqlCaptor.capture());
            assertThat(sqlCaptor.getValue()).contains("ORDER BY RANDOM() LIMIT :amount");
            verify(mockGenericExecuteSpec).bind("amount", requestedAmount);
            verify(mockGenericExecuteSpec).map(any(BiFunction.class));
            verify(mockRowsFetchSpec).all();
            verifyNoInteractions(quoteEntityMapper);
        }

        @Test
        @DisplayName("5. Should wrap database errors in QuotePersistenceException")
        void findRandomQuotes_whenDatabaseErrorOccurs_shouldWrapInPersistenceException() {
            // Arrange: Setup DB client mock chain to simulate an error during .all()
            int requestedAmount = 5;
            DataAccessResourceFailureException dbError = new DataAccessResourceFailureException("DB fetch random failed");
            arrangeDbClientMapAllChain(Flux.error(dbError));

            // Act: Execute findRandomQuotes
            Mono<Optional<List<Quote>>> resultMono = quoteRepository.findRandomQuotes(requestedAmount);

            // Assert: Verify the correct persistence exception wraps the original error
            StepVerifier.create(resultMono)
                    .expectErrorSatisfies(throwable -> {
                        assertThat(throwable)
                                .isInstanceOf(QuotePersistenceException.class)
                                .hasMessage(String.format("Database error fetching %d random quotes", requestedAmount))
                                .hasCause(dbError);
                    })
                    .verify();

            // Verify interactions occurred up to the error point
            verify(databaseClient).sql(anyString());
            verify(mockGenericExecuteSpec).bind("amount", requestedAmount);
            verify(mockGenericExecuteSpec).map(any(BiFunction.class));
            verify(mockRowsFetchSpec).all();
            verifyNoInteractions(quoteEntityMapper);
        }
    }


    @Nested
    @DisplayName("findAllQuotes Tests")
    class FindAllTests {

        // Helper method specific to findAllQuotes, binding 'limit'
        @SuppressWarnings("unchecked")
        private void arrangeDbClientFindAllChain(Flux<QuoteEntity> resultingEntityFlux) {
            // Mock the DatabaseClient fluent API chain: sql().bind().map().all()
            when(databaseClient.sql(anyString())).thenReturn(mockGenericExecuteSpec);
            when(mockGenericExecuteSpec.bind(eq("limit"), anyInt())).thenReturn(mockGenericExecuteSpec); // Specific binding
            ArgumentCaptor<BiFunction<Row, RowMetadata, QuoteEntity>> mapperCaptor = ArgumentCaptor.forClass(BiFunction.class);
            when(mockGenericExecuteSpec.map(mapperCaptor.capture())).thenReturn(mockRowsFetchSpec);
            when(mockRowsFetchSpec.all()).thenReturn(resultingEntityFlux);
        }

        @Test
        @DisplayName("1. Should return Flux of quotes when quotes are found")
        void findAllQuotes_whenFound_shouldReturnMappedQuotesFlux() {
            // Arrange: Setup mocks for finding and mapping quotes
            int requestedLimit = 2;
            QuoteEntity entityA = new QuoteEntity(10L, "Author A", "Text A", 1, "hashA", "providerA");
            QuoteEntity entityB = new QuoteEntity(20L, "Author B", "Text B", 2, "hashB", "providerB");
            Flux<QuoteEntity> entityFlux = Flux.just(entityA, entityB);

            arrangeDbClientFindAllChain(entityFlux); // Use specific helper

            Quote mappedQuoteA = new Quote(10L, "Author A", "Text A", 1);
            Quote mappedQuoteB = new Quote(20L, "Author B", "Text B", 2);
            when(quoteEntityMapper.toQuote(entityA)).thenReturn(mappedQuoteA);
            when(quoteEntityMapper.toQuote(entityB)).thenReturn(mappedQuoteB);

            // Act: Execute findAllQuotes
            Flux<Quote> resultFlux = quoteRepository.findAllQuotes(requestedLimit);

            // Assert: Verify the Flux emits the mapped quotes in order
            StepVerifier.create(resultFlux)
                    .expectNext(mappedQuoteA)
                    .expectNext(mappedQuoteB)
                    .verifyComplete();

            // Verify SQL, binding, and interactions
            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
            verify(databaseClient).sql(sqlCaptor.capture());
            assertThat(sqlCaptor.getValue()).contains("SELECT id, author, text, likes, text_author_hash, provider FROM quotes ORDER BY id LIMIT :limit");
            verify(mockGenericExecuteSpec).bind("limit", requestedLimit);
            verify(mockGenericExecuteSpec).map(any(BiFunction.class));
            verify(mockRowsFetchSpec).all();
            verify(quoteEntityMapper).toQuote(entityA);
            verify(quoteEntityMapper).toQuote(entityB);
        }

        @Test
        @DisplayName("2. Should return IllegalArgumentException when limit is zero")
        void findAllQuotes_whenLimitIsZero_shouldThrowIllegalArgumentException() {
            // Arrange: Define zero limit input
            int zeroLimit = 0;

            // Act: Execute findAllQuotes with zero limit
            Flux<Quote> resultFlux = quoteRepository.findAllQuotes(zeroLimit);

            // Assert: Verify the specific argument exception
            StepVerifier.create(resultFlux)
                    .expectErrorSatisfies(throwable -> {
                        assertThat(throwable)
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessage("Limit cannot be 0");
                    })
                    .verify();
            verifyNoInteractions(databaseClient, quoteEntityMapper);
        }

        @Test
        @DisplayName("3. Should return IllegalArgumentException when limit is negative")
        void findAllQuotes_whenLimitIsNegative_shouldThrowIllegalArgumentException() {
            // Arrange: Define negative limit input
            int negativeLimit = -1;

            // Act: Execute findAllQuotes with negative limit
            Flux<Quote> resultFlux = quoteRepository.findAllQuotes(negativeLimit);

            // Assert: Verify the specific argument exception
            StepVerifier.create(resultFlux)
                    .expectErrorSatisfies(throwable -> {
                        assertThat(throwable)
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessage("Limit cannot be 0");
                    })
                    .verify();
            verifyNoInteractions(databaseClient, quoteEntityMapper);
        }

        @Test
        @DisplayName("4. Should return empty Flux when database returns no rows")
        void findAllQuotes_whenNoResults_shouldReturnEmptyFlux() {
            // Arrange: Setup DB client mock chain for empty results
            int requestedLimit = 5;
            arrangeDbClientFindAllChain(Flux.empty());

            // Act: Execute findAllQuotes
            Flux<Quote> resultFlux = quoteRepository.findAllQuotes(requestedLimit);

            // Assert: Verify the Flux completes without emitting items
            StepVerifier.create(resultFlux)
                    .expectNextCount(0)
                    .verifyComplete();

            // Verify interactions: SQL executed, but mapper not called
            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
            verify(databaseClient).sql(sqlCaptor.capture());
            assertThat(sqlCaptor.getValue()).contains("ORDER BY id LIMIT :limit");
            verify(mockGenericExecuteSpec).bind("limit", requestedLimit);
            verify(mockGenericExecuteSpec).map(any(BiFunction.class));
            verify(mockRowsFetchSpec).all();
            verifyNoInteractions(quoteEntityMapper);
        }

        @Test
        @DisplayName("5. Should wrap database errors in QuotePersistenceException")
        void findAllQuotes_whenDatabaseErrorOccurs_shouldWrapInPersistenceException() {
            // Arrange: Setup DB client mock chain to simulate an error
            int requestedLimit = 5;
            DataAccessResourceFailureException dbError = new DataAccessResourceFailureException("DB fetch all failed");
            arrangeDbClientFindAllChain(Flux.error(dbError));

            // Act: Execute findAllQuotes
            Flux<Quote> resultFlux = quoteRepository.findAllQuotes(requestedLimit);

            // Assert: Verify the correct persistence exception wraps the original error
            StepVerifier.create(resultFlux)
                    .expectErrorSatisfies(throwable -> {
                        assertThat(throwable)
                                .isInstanceOf(QuotePersistenceException.class)
                                .hasMessage("Database error fetching all quotes")
                                .hasCause(dbError);
                    })
                    .verify();

            // Verify interactions occurred up to the error point
            verify(databaseClient).sql(anyString());
            verify(mockGenericExecuteSpec).bind("limit", requestedLimit);
            verify(mockGenericExecuteSpec).map(any(BiFunction.class));
            verify(mockRowsFetchSpec).all();
            verifyNoInteractions(quoteEntityMapper);
        }
    }
}