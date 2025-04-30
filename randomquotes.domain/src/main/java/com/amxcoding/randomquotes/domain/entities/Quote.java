package com.amxcoding.randomquotes.domain.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Quote {
    private int id;

    @JsonProperty("a")
    private String author;

    @JsonProperty("q")
    private String text;

    public Quote(int id, String author, String text) {
        this.id = id;
        this.author = author;
        this.text = text;
    }

    public Quote(String author, String text) {
        this(0, author, text);
    }


    public Quote() {
    }

    public int getId() {
        return id;
    }

    public String getAuthor() {
        return author;
    }

    public String getText() {
        return text;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Quote quote = (Quote) o;
        if (id != 0 && quote.id != 0) {
            return Objects.equals(id, quote.id);
        }
        return Objects.equals(author, quote.author) &&
                Objects.equals(text, quote.text);
    }

    @Override
    public int hashCode() {
        return id != 0 ? Objects.hash(id, author, text) : Objects.hash(author, text);
    }

    @Override
    public String toString() {
        return "Quote{" +
                "id=" + id +
                ", author='" + author + '\'' +
                ", text='" + text + '\'' +
                '}';
    }
}
