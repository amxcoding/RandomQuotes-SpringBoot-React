package com.amxcoding.randomquotes.infrastructure.persistence.models;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Objects;

@Table(name = "quotes")
public class QuoteEntity {

    @Id
    @Column("id")
    private Long id;

    @Column("author")
    private String author;

    @Column("text")
    private String text;

    @Column("likes")
    private int likes = 0;

    @Column("text_author_hash")
    private String textAuthorHash;

    public QuoteEntity() {
    }

    public QuoteEntity(String author, String text, int likes) {
        this.author = author;
        this.text = text;
        this.likes = likes;
        this.textAuthorHash = generateTextAuthorHash(author, text);
    }

    public QuoteEntity(Long id, String author, String text, int likes, String textAuthorHash) {
        this.id = id;
        this.author = author;
        this.text = text;
        this.likes = likes;
        this.textAuthorHash = textAuthorHash;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public int getLikes() { return likes; }
    public void setLikes(int likes) { this.likes = likes; }

    public String getTextAuthorHash() { return textAuthorHash; }
    // needed to set manually
    public void setTextAuthorHash(String textAuthorHash) {
        this.textAuthorHash = textAuthorHash;
    }

    // public because MapStruct  does not work well with constructor
    // set manually after mapping
    public String generateTextAuthorHash(String author, String text) {
        String combined = author + "::" + text;
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
        QuoteEntity that = (QuoteEntity) o;
        return Objects.equals(textAuthorHash, that.textAuthorHash);
    }

    @Override
    public int hashCode() {
        return Objects.hash(textAuthorHash);
    }

    @Override
    public String toString() {
        return "QuoteEntity{" +
                "id=" + id +
                ", author='" + author + '\'' +
                ", text='" + (text != null && text.length() > 50 ? text.substring(0, 50) + "..." : text) + '\'' +
                ", likes=" + likes +
                ", textAuthorHash='" + textAuthorHash + '\'' +
                '}';
    }
}
