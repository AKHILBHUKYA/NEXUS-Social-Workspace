package com.akhil.social.repository;

import com.akhil.social.entity.PostShare;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostShareRepository extends JpaRepository<PostShare, Long> {
    long countByPostId(Long postId);
}
