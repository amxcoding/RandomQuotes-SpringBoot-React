package com.amxcoding.randomquotes.infrastructure.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ZenQuote {
    @JsonProperty("a")
    private String author;

    @JsonProperty("q")
    private String text;

    public ZenQuote(String author, String text) {
        this.author = author;
        this.text = text;
    }

    public ZenQuote() {
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
}
