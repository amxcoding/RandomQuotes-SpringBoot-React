package com.amxcoding.randomquotes.domain.entities;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;

public class QuoteLike {
    private Long id;
    private String userId;
    private Long quoteId;
    private OffsetDateTime likedAt;

    public QuoteLike(Long id, String userId, Long quoteId, OffsetDateTime likedAt) {
        this.id = id;
        this.userId = userId;
        this.quoteId = quoteId;
        this.likedAt = likedAt;
    }

    public QuoteLike(Long id, String userId, Long quoteId) {
        this(id, userId, quoteId, OffsetDateTime.now(ZoneOffset.UTC));
    }

    public QuoteLike(String userId, Long quoteId) {
        this(null, userId, quoteId);
    }

    public QuoteLike() {
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
        QuoteLike quoteLike = (QuoteLike) o;
        return Objects.equals(id, quoteLike.id) && Objects.equals(userId, quoteLike.userId) && Objects.equals(quoteId, quoteLike.quoteId) && Objects.equals(likedAt, quoteLike.likedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, userId, quoteId, likedAt);
    }

    @Override
    public String toString() {
        return "QuoteLike{" +
                "id=" + id +
                ", userId='" + userId + '\'' +
                ", quoteId=" + quoteId +
                ", likedAt=" + likedAt +
                '}';
    }
}
