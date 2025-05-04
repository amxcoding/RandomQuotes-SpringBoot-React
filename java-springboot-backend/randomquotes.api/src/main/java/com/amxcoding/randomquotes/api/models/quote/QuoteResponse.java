package com.amxcoding.randomquotes.api.models.quote;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JsonIgnoreProperties(ignoreUnknown = true)
@JacksonXmlRootElement(localName = "quote")
public class QuoteResponse {
    @JacksonXmlProperty(isAttribute = true)
    private long id;
    @JacksonXmlProperty(localName = "author")
    private String author;
    @JacksonXmlProperty(localName = "text")
    private String text;
    @JacksonXmlProperty(localName = "likes")
    private int likes;
    @JacksonXmlProperty(localName = "isLiked")
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
