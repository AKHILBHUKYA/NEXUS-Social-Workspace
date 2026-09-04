package com.akhil.social.repository;

import com.akhil.social.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {
    @Query(value = "SELECT p FROM Post p JOIN FETCH p.user WHERE LOWER(p.platform) = LOWER(:platform) ORDER BY p.createdAt DESC",
           countQuery = "SELECT COUNT(p) FROM Post p WHERE LOWER(p.platform) = LOWER(:platform)")
    Page<Post> findByPlatformIgnoreCaseOrderByCreatedAtDesc(String platform, Pageable pageable);

    @Query(value = "SELECT p FROM Post p JOIN FETCH p.user ORDER BY p.createdAt DESC",
           countQuery = "SELECT COUNT(p) FROM Post p")
    Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByPlatformIgnoreCase(String platform);

    @Query("SELECT p.platform, COUNT(p) FROM Post p GROUP BY p.platform")
    List<Object[]> countByPlatform();
}
