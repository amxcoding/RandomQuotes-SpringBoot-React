// TODO FIX
//package com.amxcoding.randomquotes.infrastructure.repositories;
//
//import com.amxcoding.randomquotes.application.exceptions.repositories.QuotePersistenceException;
//import com.amxcoding.randomquotes.domain.entities.Quote;
//import com.amxcoding.randomquotes.infrastructure.persistence.mappers.QuoteEntityMapper;
//import com.amxcoding.randomquotes.infrastructure.persistence.models.QuoteEntity;
//import com.amxcoding.randomquotes.infrastructure.persistence.r2dbcs.IQuoteR2dbcRepository;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Nested;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.*;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.dao.DataAccessResourceFailureException; // Example DB exception
//import org.springframework.dao.DuplicateKeyException; // Example DB exception for save
//import org.springframework.r2dbc.core.DatabaseClient;
//import org.springframework.r2dbc.core.FetchSpec; // Import FetchSpec
//import reactor.core.publisher.Mono;
//import reactor.test.StepVerifier;
//
//import java.util.*;
//
//import static org.assertj.core.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class QuoteRepositoryUnitTest {
//
//    // Mocks for dependencies
//    @Mock
//    DatabaseClient databaseClient;
//    @Mock
//    IQuoteR2dbcRepository quoteR2dbcRepository;
//    @Mock
//    QuoteEntityMapper quoteEntityMapper;
//
//    // Mocks for DatabaseClient fluent API
//    @Mock
//    DatabaseClient.GenericExecuteSpec genericExecuteSpec;
//    @Mock
//    FetchSpec<Map<String, Object>> fetchSpec;
//
//    // Class under test, injects the mocks
//    @InjectMocks
//    QuoteRepository quoteRepository;
//
//    // Common test data
//    private Quote domainQuote1;
//    private Quote domainQuote2;
//    private QuoteEntity entityQuote1;
//    private QuoteEntity entityQuote2;
//    private String hash1;
//    private String hash2;
//
//    @BeforeEach
//    void setUp() {
//        // Initialize common test data
//        domainQuote1 = new Quote(1L, "Author One", " Text One ", 5);
//        domainQuote2 = new Quote(2L, "author two", "text two", 0);
//
//        hash1 = domainQuote1.generateTextAuthorHash();
//        hash2 = domainQuote2.generateTextAuthorHash();
//
//        entityQuote1 = new QuoteEntity(1L, "Author One", " Text One ", 5, hash1);
//        entityQuote2 = new QuoteEntity(2L, "author two", "text two", 0, hash2);
//    }
//
//    // --- Tests for bulkInsertQuotesIgnoreConflicts ---
//    @Nested
//    @DisplayName("bulkInsertQuotesIgnoreConflicts")
//    class BulkInsertTests {
//
//        @Test
//        @DisplayName("returns empty Mono when input list is empty")
//        void bulkInsert_emptyList() {
//            // Arrange
//            List<Quote> empty = Collections.emptyList();
//            // Act
//            Mono<Void> result = quoteRepository.bulkInsertQuotesIgnoreConflicts(empty);
//            // Assert
//            StepVerifier.create(result).verifyComplete();
//            // Verify no interactions with dependencies relevant to the non-empty path
//            verifyNoInteractions(databaseClient, quoteEntityMapper, quoteR2dbcRepository);
//        }
//
//        @Test
//        @DisplayName("binds and executes SQL correctly for multiple quotes")
//        void bulkInsert_nonEmpty_success() {
//            // Arrange
//            when(databaseClient.sql(anyString())).thenReturn(genericExecuteSpec);
//            when(genericExecuteSpec.bind(anyString(), any())).thenReturn(genericExecuteSpec);
//            when(genericExecuteSpec.then()).thenReturn(Mono.empty());
//
//            // Mock the mapper
//            when(quoteEntityMapper.toQuoteEntity(domainQuote1)).thenReturn(entityQuote1);
//            when(quoteEntityMapper.toQuoteEntity(domainQuote2)).thenReturn(entityQuote2);
//
//            // Act
//            Mono<Void> result = quoteRepository.bulkInsertQuotesIgnoreConflicts(List.of(domainQuote1, domainQuote2));
//
//            // Assert
//            StepVerifier.create(result).verifyComplete();
//
//            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
//            verify(databaseClient, times(1)).sql(sqlCaptor.capture());
//            assertThat(sqlCaptor.getValue()).startsWith("INSERT INTO quotes");
//            assertThat(sqlCaptor.getValue()).contains("(:author0, :text0, :likes0, :hash0)");
//            assertThat(sqlCaptor.getValue()).contains("(:author1, :text1, :likes1, :hash1)");
//            assertThat(sqlCaptor.getValue()).endsWith("ON CONFLICT (text_author_hash) DO NOTHING");
//
//            verify(genericExecuteSpec, times(8)).bind(anyString(), any());
//            verify(genericExecuteSpec, times(1)).bind("author0", entityQuote1.getAuthor());
//            verify(genericExecuteSpec, times(1)).bind("text0", entityQuote1.getText());
//            verify(genericExecuteSpec, times(1)).bind("likes0", entityQuote1.getLikes());
//            verify(genericExecuteSpec, times(1)).bind("hash0", entityQuote1.getTextAuthorHash());
//            verify(genericExecuteSpec, times(1)).bind("author1", entityQuote2.getAuthor());
//            verify(genericExecuteSpec, times(1)).bind("text1", entityQuote2.getText());
//            verify(genericExecuteSpec, times(1)).bind("likes1", entityQuote2.getLikes());
//            verify(genericExecuteSpec, times(1)).bind("hash1", entityQuote2.getTextAuthorHash());
//
//            verify(genericExecuteSpec, times(1)).then(); // Verify then() was called
//        }
//
//        @Test
//        @DisplayName("wraps database errors in QuotePersistenceException")
//        void bulkInsert_dbError() {
//            // Arrange
//            when(databaseClient.sql(anyString())).thenReturn(genericExecuteSpec);
//            when(genericExecuteSpec.bind(anyString(), any())).thenReturn(genericExecuteSpec);
//            when(genericExecuteSpec.then()).thenReturn(Mono.error(new DataAccessResourceFailureException("DB connection failed")));
//
//            // Mock mapper
//            when(quoteEntityMapper.toQuoteEntity(domainQuote1)).thenReturn(entityQuote1);
//
//            // Act
//            Mono<Void> result = quoteRepository.bulkInsertQuotesIgnoreConflicts(List.of(domainQuote1));
//
//            StepVerifier.create(result)
//                    .expectErrorSatisfies(ex -> {
//                        // Assert Exception Type and Cause Type (less brittle)
//                        assertThat(ex).isInstanceOf(QuotePersistenceException.class);
//                        assertThat(ex.getCause()).isInstanceOf(DataAccessResourceFailureException.class);
//                    })
//                    .verify();
//
//            verify(databaseClient, times(1)).sql(anyString());
//            verify(genericExecuteSpec, times(4)).bind(anyString(), any());
//            verify(genericExecuteSpec, times(1)).then();
//        }
//    }
//
//    // --- Tests for findByTextAuthorHash ---
//    // (These tests don't use databaseClient directly, no changes needed here)
//    @Nested
//    @DisplayName("findByTextAuthorHash")
//    class FindByHashTests {
//        // ... tests remain the same ...
//        @Test
//        @DisplayName("maps entity to domain and wraps Optional when found")
//        void findByTextAuthorHash_found() {
//            // Arrange
//            // Use the hash calculated in setUp
//            when(quoteR2dbcRepository.findByTextAuthorHash(hash1)).thenReturn(Mono.just(entityQuote1));
//            when(quoteEntityMapper.toQuote(entityQuote1)).thenReturn(domainQuote1);
//
//            // Act
//            Mono<Optional<Quote>> result = quoteRepository.findByTextAuthorHash(hash1);
//
//            // Assert
//            StepVerifier.create(result)
//                    .assertNext(opt -> {
//                        assertThat(opt).isPresent();
//                        assertThat(opt.get()).isEqualTo(domainQuote1);
//                        // Optional: Assert specific fields if equals is complex
//                        assertThat(opt.get().getId()).isEqualTo(domainQuote1.getId());
//                        assertThat(opt.get().getAuthor()).isEqualTo(domainQuote1.getAuthor());
//                        assertThat(opt.get().getText()).isEqualTo(domainQuote1.getText());
//                        assertThat(opt.get().getLikes()).isEqualTo(domainQuote1.getLikes());
//                    })
//                    .verifyComplete();
//            verify(quoteR2dbcRepository).findByTextAuthorHash(hash1);
//            verify(quoteEntityMapper).toQuote(entityQuote1);
//        }
//
//        @Test
//        @DisplayName("returns empty Optional when not found")
//        void findByTextAuthorHash_notFound() {
//            // Arrange
//            String unknownHash = "unknown_hash";
//            when(quoteR2dbcRepository.findByTextAuthorHash(unknownHash)).thenReturn(Mono.empty());
//
//            // Act & Assert
//            StepVerifier.create(quoteRepository.findByTextAuthorHash(unknownHash))
//                    .expectNext(Optional.empty())
//                    .verifyComplete();
//            verify(quoteR2dbcRepository).findByTextAuthorHash(unknownHash);
//            verifyNoInteractions(quoteEntityMapper);
//        }
//
//        @Test
//        @DisplayName("wraps repository errors in QuotePersistenceException")
//        void findByTextAuthorHash_repoError() {
//            // Arrange
//            String hashToTest = "some_hash";
//            when(quoteR2dbcRepository.findByTextAuthorHash(hashToTest))
//                    .thenReturn(Mono.error(new DataAccessResourceFailureException("DB down")));
//
//            // Act
//            Mono<Optional<Quote>> result = quoteRepository.findByTextAuthorHash(hashToTest);
//
//            // Assert
//            StepVerifier.create(result)
//                    .expectErrorSatisfies(ex -> {
//                        assertThat(ex).isInstanceOf(QuotePersistenceException.class)
//                                .hasMessageContaining("Error getting quote by textAuthorHash");
//                        assertThat(ex.getCause()).isInstanceOf(DataAccessResourceFailureException.class);
//                    })
//                    .verify();
//            verify(quoteR2dbcRepository).findByTextAuthorHash(hashToTest);
//            verifyNoInteractions(quoteEntityMapper);
//        }
//    }
//
//    // --- Tests for findById ---
//    // (These tests don't use databaseClient directly, no changes needed here)
//    @Nested
//    @DisplayName("findById")
//    class FindByIdTests {
//        // ... tests remain the same ...
//        @Test
//        @DisplayName("maps entity to domain and wraps Optional when found")
//        void findById_found() {
//            // Arrange
//            when(quoteR2dbcRepository.findById(1L)).thenReturn(Mono.just(entityQuote1));
//            when(quoteEntityMapper.toQuote(entityQuote1)).thenReturn(domainQuote1);
//
//            // Act
//            Mono<Optional<Quote>> result = quoteRepository.findById(1L);
//
//            // Assert
//            StepVerifier.create(result)
//                    .assertNext(opt -> {
//                        assertThat(opt).isPresent();
//                        assertThat(opt.get()).isEqualTo(domainQuote1);
//                    })
//                    .verifyComplete();
//            verify(quoteR2dbcRepository).findById(1L);
//            verify(quoteEntityMapper).toQuote(entityQuote1);
//        }
//
//        @Test
//        @DisplayName("returns empty Optional when not found")
//        void findById_notFound() {
//            // Arrange
//            when(quoteR2dbcRepository.findById(999L)).thenReturn(Mono.empty());
//
//            // Act & Assert
//            StepVerifier.create(quoteRepository.findById(999L))
//                    .expectNext(Optional.empty())
//                    .verifyComplete();
//            verify(quoteR2dbcRepository).findById(999L);
//            verifyNoInteractions(quoteEntityMapper);
//        }
//
//        @Test
//        @DisplayName("propagates repository errors (no specific wrapping)")
//        void findById_repoError() {
//            // Arrange
//            RuntimeException dbError = new DataAccessResourceFailureException("DB down");
//            when(quoteR2dbcRepository.findById(1L)).thenReturn(Mono.error(dbError));
//
//            // Act
//            Mono<Optional<Quote>> result = quoteRepository.findById(1L);
//
//            // Assert
//            StepVerifier.create(result)
//                    .expectErrorMatches(error -> error == dbError)
//                    .verify();
//            verify(quoteR2dbcRepository).findById(1L);
//            verifyNoInteractions(quoteEntityMapper);
//        }
//    }
//
//    // --- Tests for saveQuote ---
//    // (These tests don't use databaseClient directly, no changes needed here)
//    @Nested
//    @DisplayName("saveQuote")
//    class SaveQuoteTests {
//        // ... tests remain the same ...
//        @Test
//        @DisplayName("maps domain to entity (incl. hash), saves, maps back, returns domain")
//        void saveQuote_success() {
//            // Arrange
//            Quote newDomainQuote = new Quote(null, "New Author", "New Text", 0); // Domain object without ID or explicit hash
//            String expectedHash = newDomainQuote.generateTextAuthorHash();
//            // Entity that the mapper *should* create (with hash)
//            QuoteEntity entityToSave = new QuoteEntity(null, "New Author", "New Text", 0, expectedHash);
//            // Entity returned by the repo after save (with ID and hash)
//            QuoteEntity savedEntity = new QuoteEntity(100L, "New Author", "New Text", 0, expectedHash);
//            // Domain object mapped back
//            Quote savedDomain = new Quote(100L, "New Author", "New Text", 0);
//
//            // Mock mapper to correctly create entity with hash
//            when(quoteEntityMapper.toQuoteEntity(newDomainQuote)).thenReturn(entityToSave);
//            when(quoteR2dbcRepository.save(entityToSave)).thenReturn(Mono.just(savedEntity));
//            when(quoteEntityMapper.toQuote(savedEntity)).thenReturn(savedDomain);
//
//            // Act
//            Mono<Quote> result = quoteRepository.saveQuote(newDomainQuote);
//
//            // Assert
//            StepVerifier.create(result)
//                    .expectNext(savedDomain)
//                    .verifyComplete();
//            verify(quoteEntityMapper).toQuoteEntity(newDomainQuote);
//            verify(quoteR2dbcRepository).save(entityToSave); // Verify the entity with hash was saved
//            verify(quoteEntityMapper).toQuote(savedEntity);
//        }
//
//        @Test
//        @DisplayName("wraps repository errors (like DuplicateKey) in QuotePersistenceException")
//        void saveQuote_repoError() {
//            // Arrange
//            Quote newQuote = new Quote(null, "Duplicate Author", "Duplicate Text", 0);
//            String expectedHash = newQuote.generateTextAuthorHash();
//            QuoteEntity entityToSave = new QuoteEntity(null, "Duplicate Author", "Duplicate Text", 0, expectedHash);
//            DuplicateKeyException dbError = new DuplicateKeyException("Duplicate key error on text_author_hash");
//
//            when(quoteEntityMapper.toQuoteEntity(newQuote)).thenReturn(entityToSave);
//            when(quoteR2dbcRepository.save(entityToSave)).thenReturn(Mono.error(dbError));
//
//            // Act
//            Mono<Quote> result = quoteRepository.saveQuote(newQuote);
//
//            // Assert
//            StepVerifier.create(result)
//                    .expectErrorSatisfies(ex -> {
//                        assertThat(ex).isInstanceOf(QuotePersistenceException.class)
//                                .hasMessageContaining("Error saving quote");
//                        assertThat(ex.getCause()).isEqualTo(dbError);
//                    })
//                    .verify();
//            verify(quoteEntityMapper).toQuoteEntity(newQuote);
//            verify(quoteR2dbcRepository).save(entityToSave);
//            verify(quoteEntityMapper, never()).toQuote(any(QuoteEntity.class));
//        }
//    }
//
//    // --- Tests for incrementLikeCount ---
//    @Nested
//    @DisplayName("incrementLikeCount")
//    class IncrementLikeTests {
//
//        // Helper method to set up common DB client mocks for these tests
//        private void setupDbClientMocks() {
//            when(databaseClient.sql(anyString())).thenReturn(genericExecuteSpec);
//            when(genericExecuteSpec.bind(anyString(), any())).thenReturn(genericExecuteSpec);
//            when(genericExecuteSpec.fetch()).thenReturn(fetchSpec);
//        }
//
//        @Test
//        @DisplayName("returns true when update is successful (rows updated > 0)")
//        void incrementLikeCount_success() {
//            // Arrange
//            setupDbClientMocks(); // Set up mocks needed for this test
//            when(fetchSpec.rowsUpdated()).thenReturn(Mono.just(1L)); // Simulate 1 row updated
//
//            // Act
//            Mono<Boolean> result = quoteRepository.incrementLikeCount(1L);
//
//            // Assert
//            StepVerifier.create(result)
//                    .expectNext(true)
//                    .verifyComplete();
//            verify(databaseClient).sql(startsWith("UPDATE quotes SET likes = likes + 1"));
//            verify(genericExecuteSpec).bind("quoteId", 1L);
//            verify(genericExecuteSpec).fetch();
//            verify(fetchSpec).rowsUpdated();
//        }
//
//        @Test
//        @DisplayName("returns false when no rows are updated (e.g., quote ID not found)")
//        void incrementLikeCount_noRowsUpdated() {
//            // Arrange
//            setupDbClientMocks(); // Set up mocks needed for this test
//            when(fetchSpec.rowsUpdated()).thenReturn(Mono.just(0L)); // Simulate 0 rows updated
//
//            // Act
//            Mono<Boolean> result = quoteRepository.incrementLikeCount(999L); // Non-existent ID
//
//            // Assert
//            StepVerifier.create(result)
//                    .expectNext(false)
//                    .verifyComplete();
//            verify(databaseClient).sql(startsWith("UPDATE quotes SET likes = likes + 1"));
//            verify(genericExecuteSpec).bind("quoteId", 999L);
//            verify(genericExecuteSpec).fetch();
//            verify(fetchSpec).rowsUpdated();
//        }
//
//        @Test
//        @DisplayName("returns error when quoteId is null")
//        void incrementLikeCount_nullId() {
//            // Act
//            Mono<Boolean> result = quoteRepository.incrementLikeCount(null);
//
//            // Assert
//            StepVerifier.create(result)
//                    .expectError(IllegalArgumentException.class)
//                    .verify();
//            verifyNoInteractions(databaseClient); // Should fail before DB interaction
//        }
//
//        @Test
//        @DisplayName("wraps database errors in QuotePersistenceException")
//        void incrementLikeCount_dbError() {
//            // Arrange
//            setupDbClientMocks(); // Set up mocks needed for this test
//            DataAccessResourceFailureException dbError = new DataAccessResourceFailureException("DB connection failed during update");
//            when(fetchSpec.rowsUpdated()).thenReturn(Mono.error(dbError)); // Simulate error during fetch/update
//
//            // Act
//            Mono<Boolean> result = quoteRepository.incrementLikeCount(1L);
//
//            // Assert
//            StepVerifier.create(result)
//                    .expectErrorSatisfies(ex -> {
//                        assertThat(ex).isInstanceOf(QuotePersistenceException.class)
//                                .hasMessageContaining("Failed to increment like count for quote 1");
//                        assertThat(ex.getCause()).isEqualTo(dbError);
//                    })
//                    .verify();
//            verify(databaseClient).sql(startsWith("UPDATE quotes SET likes = likes + 1"));
//            verify(genericExecuteSpec).bind("quoteId", 1L);
//            verify(genericExecuteSpec).fetch();
//            verify(fetchSpec).rowsUpdated();
//        }
//    }
//
//    // --- Tests for decrementLikeCount ---
//    @Nested
//    @DisplayName("decrementLikeCount")
//    class DecrementLikeTests {
//
//        // Helper method to set up common DB client mocks for these tests
//        private void setupDbClientMocks() {
//            when(databaseClient.sql(anyString())).thenReturn(genericExecuteSpec);
//            when(genericExecuteSpec.bind(anyString(), any())).thenReturn(genericExecuteSpec);
//            when(genericExecuteSpec.fetch()).thenReturn(fetchSpec);
//        }
//
//        @Test
//        @DisplayName("returns true when update is successful (rows updated > 0)")
//        void decrementLikeCount_success() {
//            // Arrange
//            setupDbClientMocks(); // Set up mocks needed for this test
//            when(fetchSpec.rowsUpdated()).thenReturn(Mono.just(1L)); // Simulate 1 row updated
//
//            // Act
//            Mono<Boolean> result = quoteRepository.decrementLikeCount(1L);
//
//            // Assert
//            StepVerifier.create(result)
//                    .expectNext(true)
//                    .verifyComplete();
//            verify(databaseClient).sql(startsWith("UPDATE quotes SET likes = likes - 1"));
//            verify(genericExecuteSpec).bind("quoteId", 1L);
//            verify(genericExecuteSpec).fetch();
//            verify(fetchSpec).rowsUpdated();
//        }
//
//        @Test
//        @DisplayName("returns false when no rows are updated (e.g., quote ID not found or likes already 0)")
//        void decrementLikeCount_noRowsUpdated() {
//            // Arrange
//            setupDbClientMocks(); // Set up mocks needed for this test
//            when(fetchSpec.rowsUpdated()).thenReturn(Mono.just(0L)); // Simulate 0 rows updated
//
//            // Act
//            Mono<Boolean> result = quoteRepository.decrementLikeCount(999L); // Non-existent ID or likes=0
//
//            // Assert
//            StepVerifier.create(result)
//                    .expectNext(false)
//                    .verifyComplete();
//            verify(databaseClient).sql(startsWith("UPDATE quotes SET likes = likes - 1"));
//            verify(genericExecuteSpec).bind("quoteId", 999L);
//            verify(genericExecuteSpec).fetch();
//            verify(fetchSpec).rowsUpdated();
//        }
//
//        @Test
//        @DisplayName("returns error when quoteId is null")
//        void decrementLikeCount_nullId() {
//            // Act
//            Mono<Boolean> result = quoteRepository.decrementLikeCount(null);
//
//            // Assert
//            StepVerifier.create(result)
//                    .expectError(IllegalArgumentException.class)
//                    .verify();
//            verifyNoInteractions(databaseClient); // Should fail before DB interaction
//        }
//
//        @Test
//        @DisplayName("wraps database errors in QuotePersistenceException")
//        void decrementLikeCount_dbError() {
//            // Arrange
//            setupDbClientMocks(); // Set up mocks needed for this test
//            DataAccessResourceFailureException dbError = new DataAccessResourceFailureException("DB connection failed during update");
//            when(fetchSpec.rowsUpdated()).thenReturn(Mono.error(dbError)); // Simulate error during fetch/update
//
//            // Act
//            Mono<Boolean> result = quoteRepository.decrementLikeCount(1L);
//
//            // Assert
//            StepVerifier.create(result)
//                    .expectErrorSatisfies(ex -> {
//                        assertThat(ex).isInstanceOf(QuotePersistenceException.class)
//                                .hasMessageContaining("Failed to decrement like count for quote 1");
//                        assertThat(ex.getCause()).isEqualTo(dbError);
//                    })
//                    .verify();
//            verify(databaseClient).sql(startsWith("UPDATE quotes SET likes = likes - 1"));
//            verify(genericExecuteSpec).bind("quoteId", 1L);
//            verify(genericExecuteSpec).fetch();
//            verify(fetchSpec).rowsUpdated();
//        }
//    }
//}