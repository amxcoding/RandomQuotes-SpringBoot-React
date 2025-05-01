package com.amxcoding.randomquotes.api.models.quote;

public class CreateQuoteRequest {
    public String author;
    public String text;
    public int likes;

    public CreateQuoteRequest() {
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
}
