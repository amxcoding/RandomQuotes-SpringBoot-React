package com.amxcoding.randomquotes.infrastructure.persistence.models;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

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

    @Column("provider")
    private String provider;

    public QuoteEntity(Long id, String author, String text, int likes, String textAuthorHash, String provider) {
        this.id = id;
        this.author = author;
        this.text = text;
        this.likes = likes;
        this.textAuthorHash = textAuthorHash;
        this.provider = provider;
    }

    public QuoteEntity() {
    }
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getAuthor() {
        return author;
    }
    public void setAuthor(String author) {
        this.author = author;
    }
    public String getText() {
        return text;
    }
    public void setText(String text) {
        this.text = text;
    }
    public int getLikes() {
        return likes;
    }
    public void setLikes(int likes) {
        this.likes = likes;
    }
    public String getTextAuthorHash() {
        return textAuthorHash;
    }
    public void setTextAuthorHash(String textAuthorHash) {
        this.textAuthorHash = textAuthorHash;
    }
    public String getProvider() {
        return provider;
    }
    public void setProvider(String provider) {
        this.provider = provider;
    }
}
