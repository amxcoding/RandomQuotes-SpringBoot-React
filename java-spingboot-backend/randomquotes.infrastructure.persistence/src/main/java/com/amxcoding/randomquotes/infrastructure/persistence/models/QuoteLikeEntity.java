package com.amxcoding.randomquotes.infrastructure.persistence.models;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;
import java.util.Objects;

@Table(name = "quote_like")
public class QuoteLikeEntity {

    @Id
    @Column("id")
    private Long id;

    @Column("user_id")
    private String userId;

    @Column("quote_id")
    private Long quoteId;

    @Column("liked_at")
    private OffsetDateTime likedAt;

    public QuoteLikeEntity() {
    }

    public QuoteLikeEntity(Long id, String userId, Long quoteId, OffsetDateTime likedAt) {
        this.id = id;
        this.userId = userId;
        this.quoteId = quoteId;
        this.likedAt = likedAt;
    }

    public QuoteLikeEntity(String userId, Long quoteId, OffsetDateTime likedAt) {
        this.userId = userId;
        this.quoteId = quoteId;
        this.likedAt = likedAt;
    }

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }
    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Long getQuoteId() {
        return quoteId;
    }
    public void setQuoteId(Long quoteId) {
        this.quoteId = quoteId;
    }

    public OffsetDateTime getLikedAt() {
        return likedAt;
    }
    public void setLikedAt(OffsetDateTime likedAt) {
        this.likedAt = likedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        QuoteLikeEntity that = (QuoteLikeEntity) o;
        return Objects.equals(id, that.id) && Objects.equals(userId, that.userId) && Objects.equals(quoteId, that.quoteId) && Objects.equals(likedAt, that.likedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, userId, quoteId, likedAt);
    }
}
