package com.amxcoding.randomquotes.api.models.quote;

import com.fasterxml.jackson.annotation.JsonProperty;

public class QuoteResponse {
    private long id;
    private String author;
    private String text;
    private int likes;
    private boolean isLiked;

    public QuoteResponse() {
    }

    public long getId() {
        return id;
    }
    public void setId(long id) {
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

    @JsonProperty("isLiked")
    public boolean isLiked() {
        return isLiked;
    }

    @JsonProperty("isLiked")
    public void setIsLiked(boolean isLiked) {
        this.isLiked = isLiked;
    }

    @Override
    public String toString() {
        return "QuoteResponse{" +
                "id=" + id +
                ", author='" + author + '\'' +
                ", text='" + text + '\'' +
                ", likes=" + likes +
                ", isLiked=" + isLiked +
                '}';
    }
}
