package com.amxcoding.randomquotes.domain.entities;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

import static org.assertj.core.api.Assertions.*;

class QuoteTest {

    @Test
    @DisplayName("generateTextAuthorHash is deterministic for same author/text")
    void generateTextAuthorHash_isDeterministic() {
        // Arrange
        String author = "Albert Einstein";
        String text   = "Life is like riding a bicycle. To keep your balance you must keep moving.";
        Quote q1 = new Quote(author, text);
        Quote q2 = new Quote(author, text);

        // Act
        String hash1 = q1.generateTextAuthorHash();
        String hash2 = q2.generateTextAuthorHash();

        // Assert
        assertThat(hash1).isEqualTo(hash2);
        assertThat(hash1).isNotEmpty();
    }

    @Test
    @DisplayName("generateTextAuthorHash normalizes whitespace and case")
    void generateTextAuthorHash_ignoresCaseAndWhitespace() {
        // Arrange: same content but different casing and extra spaces
        Quote q1 = new Quote("  Albert EINSTEIN  ",
                "Life IS like riding a BICYCLE. ");
        Quote q2 = new Quote("albert einstein",
                " life is like riding a bicycle.");

        // Act
        String hash1 = q1.generateTextAuthorHash();
        String hash2 = q2.generateTextAuthorHash();

        // Assert: they must match because generateTextAuthorHash trims & lowercases
        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    @DisplayName("generateTextAuthorHash produces expected value (cross-check)")
    void generateTextAuthorHash_matchesManualDigest() throws Exception {
        // Arrange
        String author = "Foo";
        String text   = "Bar";
        Quote q       = new Quote(author, text);

        // Manually compute expected: SHA-256 of "foo::bar", then Base64 URL without padding
        String combined = (author + "::" + text)
                .trim()
                .toLowerCase();
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(combined.getBytes(StandardCharsets.UTF_8));
        String expected = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(hashBytes);

        // Act
        String actual = q.generateTextAuthorHash();

        // Assert
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    @DisplayName("generateTextAuthorHash differs for different inputs")
    void generateTextAuthorHash_isUniqueAcrossDifferentQuotes() {
        // Arrange
        Quote q1 = new Quote("Alice", "Hello world");
        Quote q2 = new Quote("Bob",   "Hello world");
        Quote q3 = new Quote("Alice", "Goodbye world");

        // Act
        String h1 = q1.generateTextAuthorHash();
        String h2 = q2.generateTextAuthorHash();
        String h3 = q3.generateTextAuthorHash();

        // Assert: all three should be distinct
        assertThat(h1).isNotEqualTo(h2)
                .isNotEqualTo(h3);
        assertThat(h2).isNotEqualTo(h3);
    }
}
