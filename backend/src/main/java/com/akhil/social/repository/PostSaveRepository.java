package com.akhil.social.repository;

import com.akhil.social.entity.PostSave;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

public interface PostSaveRepository extends JpaRepository<PostSave, PostSave.PostSaveId> {
    boolean existsByPostIdAndUserId(Long postId, Long userId);
    @Modifying @Transactional
    void deleteByPostIdAndUserId(Long postId, Long userId);
    List<PostSave> findByUserIdOrderByCreatedAtDesc(Long userId);
}
