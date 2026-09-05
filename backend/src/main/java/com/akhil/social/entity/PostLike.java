package com.akhil.social.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "post_likes")
@IdClass(PostLike.PostLikeId.class)
public class PostLike {
    @Id
    @Column(name = "post_id")
    private Long postId;
    @Id
    @Column(name = "user_id")
    private Long userId;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public static class PostLikeId implements Serializable {
        private Long postId;
        private Long userId;
        public PostLikeId() {}
        public PostLikeId(Long postId, Long userId) { this.postId = postId; this.userId = userId; }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PostLikeId that)) return false;
            return Objects.equals(postId, that.postId) && Objects.equals(userId, that.userId);
        }
        @Override public int hashCode() { return Objects.hash(postId, userId); }
    }

    public Long getPostId() { return postId; }
    public void setPostId(Long postId) { this.postId = postId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
