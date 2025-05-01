package com.amxcoding.randomquotes.infrastructure.persistence.models;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.*;

public class QuoteEntityTest {

    // Helper method to calculate the expected hash, mirroring the private method in QuoteEntity
    private String calculateExpectedHash(String author, String text) {
        String combined = author + "::" + text;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(combined.getBytes(StandardCharsets.UTF_8));
            Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
            return encoder.encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available in test environment", e);
        }
    }

    @Test
    @DisplayName("Constructor should generate correct hash for author and text")
    void constructor_shouldGenerateCorrectHash() {
        // Arrange
        String author = "Test Author";
        String text = "This is a test quote.";
        int likes = 10;
        String expectedHash = calculateExpectedHash(author, text);

        // Act
        QuoteEntity quote = new QuoteEntity(author, text, likes);

        // Assert
        assertThat(quote.getTextAuthorHash()).isNotNull();
        assertThat(quote.getTextAuthorHash()).isEqualTo(expectedHash);
        assertThat(quote.getAuthor()).isEqualTo(author);
        assertThat(quote.getText()).isEqualTo(text);
        assertThat(quote.getLikes()).isEqualTo(likes);
        assertThat(quote.getId()).isEqualTo(0L);
    }

    @Test
    @DisplayName("Constructor with ID should generate correct hash")
    void constructorWithId_shouldGenerateCorrectHash() {
        // Arrange
        long id = 1L;
        String author = "Another Author";
        String text = "Another test quote with ID.";
        int likes = 5;
        String expectedHash = calculateExpectedHash(author, text);

        // Act
        QuoteEntity quote = new QuoteEntity(id, author, text, likes);

        // Assert
        assertThat(quote.getTextAuthorHash()).isNotNull();
        assertThat(quote.getTextAuthorHash()).isEqualTo(expectedHash);
        assertThat(quote.getId()).isEqualTo(id);
        assertThat(quote.getAuthor()).isEqualTo(author);
        assertThat(quote.getText()).isEqualTo(text);
        assertThat(quote.getLikes()).isEqualTo(likes);
    }


    @Test
    @DisplayName("Different author/text combinations should produce different hashes")
    void generateHash_differentInputs_shouldProduceDifferentHashes() {
        // Arrange
        String author1 = "Author A";
        String text1 = "Text 1";
        String author2 = "Author B";
        String text2 = "Text 1"; // Same text, different author
        String author3 = "Author A";
        String text3 = "Text 2"; // Same author, different text

        // Act
        QuoteEntity quote1 = new QuoteEntity(author1, text1, 0);
        QuoteEntity quote2 = new QuoteEntity(author2, text2, 0);
        QuoteEntity quote3 = new QuoteEntity(author3, text3, 0);

        // Assert: Hashes should be different
        assertThat(quote1.getTextAuthorHash()).isNotEqualTo(quote2.getTextAuthorHash());
        assertThat(quote1.getTextAuthorHash()).isNotEqualTo(quote3.getTextAuthorHash());
        assertThat(quote2.getTextAuthorHash()).isNotEqualTo(quote3.getTextAuthorHash());
    }

    @Test
    @DisplayName("Same author/text combination should produce the same hash consistently")
    void generateHash_sameInputs_shouldProduceSameHash() {
        // Arrange
        String author = "Consistent Author";
        String text = "This quote should always produce the same hash.";

        // Act
        QuoteEntity quote1 = new QuoteEntity(author, text, 0);
        QuoteEntity quote2 = new QuoteEntity(author, text, 0);
        QuoteEntity quote3 = new QuoteEntity(author, text, 100); // Different likes

        // Assert
        assertThat(quote1.getTextAuthorHash()).isEqualTo(quote2.getTextAuthorHash());
        assertThat(quote1.getTextAuthorHash()).isEqualTo(quote3.getTextAuthorHash());
    }

    @Test
    @DisplayName("Hash generation should handle special characters correctly")
    void generateHash_specialCharacters_shouldWork() {
        // Arrange
        String author = "Author with!@#$%^&*()_+";
        String text = "Text with <>?,\"'{}[];:\\|~`";
        String expectedHash = calculateExpectedHash(author, text);

        // Act
        QuoteEntity quote = new QuoteEntity(author, text, 0);

        // Assert
        assertThat(quote.getTextAuthorHash()).isEqualTo(expectedHash);
    }

    @Test
    @DisplayName("Hash generation should handle empty strings correctly")
    void generateHash_emptyStrings_shouldWork() {
        // Arrange 1: Both empty
        String author1 = "";
        String text1 = "";
        String expectedHash1 = calculateExpectedHash(author1, text1);

        // Arrange 2: Author only
        String author2 = "Author Only";
        String text2 = "";
        String expectedHash2 = calculateExpectedHash(author2, text2);

        // Arrange 3: Text only
        String author3 = "";
        String text3 = "Text Only";
        String expectedHash3 = calculateExpectedHash(author3, text3);


        // Act
        QuoteEntity quote1 = new QuoteEntity(author1, text1, 0);
        QuoteEntity quote2 = new QuoteEntity(author2, text2, 0);
        QuoteEntity quote3 = new QuoteEntity(author3, text3, 0);

        // Assert
        assertThat(quote1.getTextAuthorHash()).isEqualTo(expectedHash1);
        assertThat(quote2.getTextAuthorHash()).isEqualTo(expectedHash2);
        assertThat(quote3.getTextAuthorHash()).isEqualTo(expectedHash3);
    }
}