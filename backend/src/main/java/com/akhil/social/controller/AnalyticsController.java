package com.akhil.social.controller;

import com.akhil.social.repository.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.*;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {
    private final PostRepository postRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final PostLikeRepository likeRepository;
    private final PostShareRepository shareRepository;
    private final PostSaveRepository saveRepository;

    public AnalyticsController(PostRepository postRepository, MessageRepository messageRepository,
                               UserRepository userRepository, CommentRepository commentRepository,
                               PostLikeRepository likeRepository, PostShareRepository shareRepository,
                               PostSaveRepository saveRepository) {
        this.postRepository = postRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.commentRepository = commentRepository;
        this.likeRepository = likeRepository;
        this.shareRepository = shareRepository;
        this.saveRepository = saveRepository;
    }

    @GetMapping
    public Map<String, Object> analytics() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("totalUsers", userRepository.count());
        m.put("totalPosts", postRepository.count());
        m.put("totalMessages", messageRepository.count());
        m.put("totalComments", commentRepository.count());
        m.put("totalLikes", likeRepository.count());
        m.put("totalShares", shareRepository.count());
        m.put("totalSaves", saveRepository.count());

        Map<String, Long> byPlatform = new LinkedHashMap<>();
        for (Object[] row : postRepository.countByPlatform()) {
            byPlatform.put(String.valueOf(row[0]), ((Number) row[1]).longValue());
        }
        m.put("postsByPlatform", byPlatform);
        m.put("engagementByPlatform", byPlatform); // simplified
        m.put("recentActivity", List.of());
        m.put("topPosts", List.of());
        return m;
    }
}
