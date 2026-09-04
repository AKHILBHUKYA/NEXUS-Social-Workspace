package com.akhil.social.repository;

import com.akhil.social.entity.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

public interface PostLikeRepository extends JpaRepository<PostLike, PostLike.PostLikeId> {
    boolean existsByPostIdAndUserId(Long postId, Long userId);
    @Modifying @Transactional
    void deleteByPostIdAndUserId(Long postId, Long userId);
    long countByPostId(Long postId);
}
