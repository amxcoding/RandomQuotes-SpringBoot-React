package com.amxcoding.randomquotes.infrastructure.repositories;

import com.amxcoding.randomquotes.application.exceptions.repositories.QuotePersistenceException;
import com.amxcoding.randomquotes.application.interfaces.repositories.IQuoteRepository;
import com.amxcoding.randomquotes.domain.entities.Quote;
import com.amxcoding.randomquotes.infrastructure.persistence.models.ZenQuoteEntity;
import io.r2dbc.spi.ConnectionFactory;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.data.relational.core.query.Criteria.where;
import static org.springframework.data.relational.core.query.Query.query;

@DataR2dbcTest
@Import(QuoteRepository.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.DisplayName.class)
@DisplayName("QuoteRepository Integration Tests")
class QuoteRepositoryIntegrationTest {

    @Autowired
    IQuoteRepository quoteRepository;

    @Autowired
    DatabaseClient databaseClient;

    @Autowired
    ConnectionFactory connectionFactory;

    private R2dbcEntityTemplate entityTemplate;

    // --- Test Data ---
    private final Quote quote1 = new Quote("Author 1", "Text 1");
    private final Quote quote2 = new Quote("Author 2", "Text 2");
    private final Quote quote1Duplicate = new Quote("Author 1", "Text 1");

    @BeforeAll
    void setupTemplate() {
        entityTemplate = new R2dbcEntityTemplate(connectionFactory);
    }

    @BeforeEach
    void cleanDatabase() {
        databaseClient
                .sql("DELETE FROM quotes")
                .then()
                .block();
    }

    // --- Helper Methods ---
    private Mono<ZenQuoteEntity> findEntityById(Long id) {
        return entityTemplate.selectOne(
                query(where("id").is(id)),
                ZenQuoteEntity.class
        );
    }

    private Mono<Long> countQuotes() {
        return databaseClient
                .sql("SELECT COUNT(*) FROM quotes")
                .map(row -> row.get(0, Long.class))
                .one()
                .defaultIfEmpty(0L);
    }

    // =========================================================================
    // Test Cases
    // =========================================================================

    @Nested
    @DisplayName("saveQuote()")
    class SaveQuoteTests {

        @Test
        @DisplayName("should persist a new quote and return it with an ID")
        void saveQuote_success() {
            // Use AtomicReference to capture the ID from the reactive stream safely
            AtomicReference<Long> savedIdRef = new AtomicReference<>();

            // Step 1: Verify the save operation returns the expected domain object
            StepVerifier.create(quoteRepository.saveQuote(quote1))
                    .assertNext(savedQuote -> {
                        assertThat(savedQuote.getId()).isNotNull();
                        savedIdRef.set(savedQuote.getId()); // Capture the ID
                        assertThat(savedQuote.getAuthor()).isEqualTo(quote1.getAuthor());
                        assertThat(savedQuote.getText()).isEqualTo(quote1.getText());
                        assertThat(savedQuote.getLikes()).isZero();
                    })
                    .verifyComplete(); // Complete the verification of the save operation

            // Ensure ID was captured before proceeding
            Long savedId = savedIdRef.get();
            assertThat(savedId).isNotNull();

            // Step 2: Verify the entity directly in the DB using the captured ID
            // This runs *after* the first StepVerifier completes.
            StepVerifier.create(findEntityById(savedId))
                    .assertNext(entity -> {
                        assertThat(entity.getId()).isEqualTo(savedId);
                        assertThat(entity.getAuthor()).isEqualTo(quote1.getAuthor());
                        assertThat(entity.getText()).isEqualTo(quote1.getText());
                        assertThat(entity.getLikes()).isZero();
                        assertThat(entity.getTextAuthorHash()).isEqualTo(quote1.generateTextAuthorHash());
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("should fail with QuotePersistenceException when saving a duplicate quote")
        void saveQuote_duplicate() {
            // Arrange: Save the first quote using StepVerifier for consistency
            AtomicReference<Long> savedIdRef = new AtomicReference<>();
            StepVerifier.create(quoteRepository.saveQuote(quote1))
                    .assertNext(savedQuote -> {
                        assertThat(savedQuote.getId()).isNotNull();
                        savedIdRef.set(savedQuote.getId());
                    })
                    .verifyComplete();
            assertThat(savedIdRef.get()).isNotNull(); // Ensure save completed and ID was set


            // Act & Assert: Try saving the duplicate
            StepVerifier.create(quoteRepository.saveQuote(quote1Duplicate)) // Use correct variable name
                    .expectErrorSatisfies(error -> {
                        assertThat(error).isInstanceOf(QuotePersistenceException.class);
                        // Check the cause for H2 specific unique constraint violation
                        assertThat(error.getCause()).isInstanceOf(DataIntegrityViolationException.class);
                        // H2 message might mention the constraint name or "Unique index or primary key violation"
                        // Avoid checking exact message string for less brittleness
                        assertThat(error.getCause().getMessage()).containsIgnoringCase("UNIQUE");
                    })
                    .verify(); // Verify the error signal completes the stream

            // Verify only one quote exists *after* the error verification
            StepVerifier.create(countQuotes())
                    .expectNext(1L)
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("findById()")
    class FindByIdTests {

        @Test
        @DisplayName("should return Optional containing the quote when found")
        void findById_found() {
            // Arrange
            Quote savedQuote = quoteRepository.saveQuote(quote1).block();
            assertThat(savedQuote).isNotNull();
            Long id = savedQuote.getId();

            // Act & Assert
            StepVerifier.create(quoteRepository.findById(id))
                    .assertNext(optionalQuote -> {
                        assertThat(optionalQuote).isPresent();
                        optionalQuote.ifPresent(q -> {
                            assertThat(q.getId()).isEqualTo(id);
                            assertThat(q.getAuthor()).isEqualTo(quote1.getAuthor());
                            assertThat(q.getText()).isEqualTo(quote1.getText());
                        });
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("should return empty Optional when ID does not exist")
        void findById_notFound() {
            // Arrange
            Long nonExistentId = 999L;

            // Act & Assert
            StepVerifier.create(quoteRepository.findById(nonExistentId))
                    .assertNext(optionalQuote -> assertThat(optionalQuote).isNotPresent())
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("findByTextAuthorHash()")
    class FindByHashTests {

        @Test
        @DisplayName("should return Optional containing the quote when found")
        void findByTextAuthorHash_found() {
            // Arrange
            Quote savedQuote = quoteRepository.saveQuote(quote1).block();
            assertThat(savedQuote).isNotNull();
            String hash = quote1.generateTextAuthorHash();

            // Act & Assert
            StepVerifier.create(quoteRepository.findByTextAuthorHash(hash))
                    .assertNext(optionalQuote -> {
                        assertThat(optionalQuote).isPresent();
                        optionalQuote.ifPresent(q -> {
                            assertThat(q.getId()).isEqualTo(savedQuote.getId());
                            assertThat(q.getAuthor()).isEqualTo(quote1.getAuthor());
                            assertThat(q.getText()).isEqualTo(quote1.getText());
                        });
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("should return empty Optional when hash does not exist")
        void findByTextAuthorHash_notFound() {
            // Arrange
            String nonExistentHash = "non_existent_hash_value";

            // Act & Assert
            StepVerifier.create(quoteRepository.findByTextAuthorHash(nonExistentHash))
                    .assertNext(optionalQuote -> assertThat(optionalQuote).isNotPresent())
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("bulkInsertQuotesIgnoreConflicts()")
    class BulkInsertTests {

        /**
         * Test skipped as h2 does not support on conflict ignore
         */
//        @Test
//        @DisplayName("should insert multiple distinct quotes")
//        void bulkInsert_multipleDistinct() {
//            StepVerifier.create(
//                            quoteRepository.bulkInsertQuotesIgnoreConflicts(List.of(quote1, quote2))
//                    )
//                    .verifyComplete();
//
//            StepVerifier.create(countQuotes())
//                    .expectNext(2L)
//                    .verifyComplete();
//
//            StepVerifier.create(
//                            quoteRepository.findByTextAuthorHash(quote1.generateTextAuthorHash())
//                    )
//                    .expectNextMatches(Optional::isPresent)
//                    .verifyComplete();
//
//            StepVerifier.create(
//                            quoteRepository.findByTextAuthorHash(quote2.generateTextAuthorHash())
//                    )
//                    .expectNextMatches(Optional::isPresent)
//                    .verifyComplete();
//        }

        /**
         * Test skipped as h2 does not support on conflict ignore
         */
//        @Test
//        @DisplayName("should insert new quotes and ignore existing duplicates")
//        void bulkInsert_mixNewAndDuplicate() {
//            quoteRepository.saveQuote(quote1).block();
//
//            StepVerifier.create(
//                            quoteRepository.bulkInsertQuotesIgnoreConflicts(List.of(quote1Duplicate, quote2))
//                    )
//                    .verifyComplete();
//
//            StepVerifier.create(countQuotes())
//                    .expectNext(2L)
//                    .verifyComplete();
//
//            StepVerifier.create(
//                            quoteRepository.findByTextAuthorHash(quote1.generateTextAuthorHash())
//                    )
//                    .expectNextMatches(Optional::isPresent)
//                    .verifyComplete();
//
//            StepVerifier.create(
//                            quoteRepository.findByTextAuthorHash(quote2.generateTextAuthorHash())
//                    )
//                    .expectNextMatches(Optional::isPresent)
//                    .verifyComplete();
//        }

        @Test
        @DisplayName("should do nothing when inserting an empty list")
        void bulkInsert_emptyList() {
            StepVerifier.create(
                            quoteRepository.bulkInsertQuotesIgnoreConflicts(List.of())
                    )
                    .verifyComplete();

            StepVerifier.create(countQuotes())
                    .expectNext(0L)
                    .verifyComplete();
        }

        /**
         * Test skipped as h2 does not support on conflict ignore
         */
//        @Test
//        @DisplayName("should ignore all duplicates when inserting only existing quotes")
//        void bulkInsert_allDuplicates() {
//            quoteRepository.saveQuote(quote1).block();
//            quoteRepository.saveQuote(quote2).block();
//
//            StepVerifier.create(
//                            quoteRepository.bulkInsertQuotesIgnoreConflicts(
//                                    List.of(quote1Duplicate, new Quote(quote2.getAuthor(), quote2.getText()))
//                            )
//                    )
//                    .verifyComplete();
//
//            StepVerifier.create(countQuotes())
//                    .expectNext(2L)
//                    .verifyComplete();
//        }
    }

    @Nested
    @DisplayName("incrementLikeCount() / decrementLikeCount()")
    class LikeCounterTests {

        private Long setupQuoteForLikeTest() {
            Quote saved = quoteRepository.saveQuote(new Quote("Like Author", "Like Text")).block();
            assertThat(saved).isNotNull();
            assertThat(saved.getLikes()).isZero();
            return saved.getId();
        }

        @Test
        @DisplayName("incrementLikeCount should increase likes and return true")
        void incrementLikeCount_success() {
            Long id = setupQuoteForLikeTest();

            StepVerifier.create(quoteRepository.incrementLikeCount(id))
                    .expectNext(true)
                    .verifyComplete();

            StepVerifier.create(findEntityById(id))
                    .assertNext(entity -> assertThat(entity.getLikes()).isEqualTo(1))
                    .verifyComplete();

            StepVerifier.create(quoteRepository.incrementLikeCount(id))
                    .expectNext(true)
                    .verifyComplete();

            StepVerifier.create(findEntityById(id))
                    .assertNext(entity -> assertThat(entity.getLikes()).isEqualTo(2))
                    .verifyComplete();
        }

        @Test
        @DisplayName("decrementLikeCount should decrease likes when > 0 and return true")
        void decrementLikeCount_success() {
            Long id = setupQuoteForLikeTest();
            quoteRepository.incrementLikeCount(id).block();
            quoteRepository.incrementLikeCount(id).block();

            StepVerifier.create(findEntityById(id))
                    .assertNext(entity -> assertThat(entity.getLikes()).isEqualTo(2))
                    .verifyComplete();

            StepVerifier.create(quoteRepository.decrementLikeCount(id))
                    .expectNext(true)
                    .verifyComplete();

            StepVerifier.create(findEntityById(id))
                    .assertNext(entity -> assertThat(entity.getLikes()).isEqualTo(1))
                    .verifyComplete();

            StepVerifier.create(quoteRepository.decrementLikeCount(id))
                    .expectNext(true)
                    .verifyComplete();

            StepVerifier.create(findEntityById(id))
                    .assertNext(entity -> assertThat(entity.getLikes()).isZero())
                    .verifyComplete();
        }

        @Test
        @DisplayName("decrementLikeCount should return false when likes are 0")
        void decrementLikeCount_atZero() {
            Long id = setupQuoteForLikeTest();

            StepVerifier.create(quoteRepository.decrementLikeCount(id))
                    .expectNext(false)
                    .verifyComplete();

            StepVerifier.create(findEntityById(id))
                    .assertNext(entity -> assertThat(entity.getLikes()).isZero())
                    .verifyComplete();
        }

        @Test
        @DisplayName("incrementLikeCount should return false when quote ID does not exist")
        void incrementLikeCount_notFound() {
            StepVerifier.create(quoteRepository.incrementLikeCount(999L))
                    .expectNext(false)
                    .verifyComplete();
        }

        @Test
        @DisplayName("decrementLikeCount should return false when quote ID does not exist")
        void decrementLikeCount_notFound() {
            StepVerifier.create(quoteRepository.decrementLikeCount(999L))
                    .expectNext(false)
                    .verifyComplete();
        }

        @Test
        @DisplayName("incrementLikeCount should fail on null ID")
        void incrementLikeCount_nullId() {
            StepVerifier.create(quoteRepository.incrementLikeCount(null))
                    .expectError(IllegalArgumentException.class)
                    .verify();
        }

        @Test
        @DisplayName("decrementLikeCount should fail on null ID")
        void decrementLikeCount_nullId() {
            StepVerifier.create(quoteRepository.decrementLikeCount(null))
                    .expectError(IllegalArgumentException.class)
                    .verify();
        }
    }
}
