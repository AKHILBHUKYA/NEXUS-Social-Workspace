package com.akhil.social.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "follows")
@IdClass(Follow.FollowId.class)
public class Follow {
    @Id @Column(name = "follower_id") private Long followerId;
    @Id @Column(name = "following_id") private Long followingId;
    @Column(name = "created_at", nullable = false) private Instant createdAt = Instant.now();

    public static class FollowId implements Serializable {
        private Long followerId; private Long followingId;
        public FollowId() {}
        public FollowId(Long followerId, Long followingId) { this.followerId = followerId; this.followingId = followingId; }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof FollowId that)) return false;
            return Objects.equals(followerId, that.followerId) && Objects.equals(followingId, that.followingId);
        }
        @Override public int hashCode() { return Objects.hash(followerId, followingId); }
    }
    public Long getFollowerId() { return followerId; }
    public void setFollowerId(Long followerId) { this.followerId = followerId; }
    public Long getFollowingId() { return followingId; }
    public void setFollowingId(Long followingId) { this.followingId = followingId; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
