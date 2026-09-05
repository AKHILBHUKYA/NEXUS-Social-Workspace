package com.akhil.social.repository;

import com.akhil.social.entity.Follow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

public interface FollowRepository extends JpaRepository<Follow, Follow.FollowId> {
    boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId);

    @Modifying
    @Transactional
    void deleteByFollowerIdAndFollowingId(Long followerId, Long followingId);

    List<Follow> findByFollowerId(Long followerId);
    List<Follow> findByFollowingId(Long followingId);
    long countByFollowerId(Long followerId);
    long countByFollowingId(Long followingId);
}
