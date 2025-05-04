package com.amxcoding.randomquotes.domain.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Objects;

// Caveat: according to clean code practice these annotations should not be used on domain classes
@JsonIgnoreProperties(ignoreUnknown = true)
public class Quote {
    private Long id;

    @JsonProperty("a")
    private String author;

    @JsonProperty("q")
    private String text;

    private int likes = 0;

    public Quote(Long id, String author, String text, int likes) {
        this.id = id;
        this.author = author;
        this.text = text;
        this.likes = likes;
    }

    public Quote(Long id, String author, String text) {
        this(id, author, text, 0);
    }

    // new quote constructor
    public Quote(String author, String text) {
        this(null, author, text);
    }

    public Quote() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public int getLikes() { return likes; }
    public void setLikes(int likes) { this.likes = likes; }

    public String generateTextAuthorHash() {
        String combined = author.trim().toLowerCase() + "::" + text.trim().toLowerCase();

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(combined.getBytes(StandardCharsets.UTF_8));
            Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
            return encoder.encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Quote quote = (Quote) o;

        // If IDs are assigned (i.e., not null), compare by ID
        if (id != null && quote.id != null) {
            return Objects.equals(id, quote.id);
        }
        // If IDs are not assigned, compare by natural key (author, text)
        return Objects.equals(author, quote.author) &&
                Objects.equals(text, quote.text);
    }

    @Override
    public int hashCode() {
        return id != null ? Objects.hash(id, author, text) : Objects.hash(author, text);
    }


    // Update toString
    @Override
    public String toString() {
        return "Quote{" +
                "id=" + id +
                ", author='" + author + '\'' +
                ", text='" + text + '\'' +
                ", likes=" + likes +
                '}';
    }
}