package com.akhil.social.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public class PostDtos {
    public record CreatePostRequest(
            @NotBlank String platform,
            @NotBlank @Size(max = 5000) String content,
            String mediaUrl,
            String mediaType
    ) {}
    public record CommentRequest(@NotBlank @Size(max = 2000) String content) {}
    public record PostResponse(
            Long id, Long userId, String author, String platform, String content,
            String mediaUrl, String mediaType,
            int likeCount, int commentCount, int shareCount, int saveCount,
            boolean likedByMe, boolean savedByMe, Instant createdAt
    ) {}
    public record CommentResponse(Long id, Long postId, Long userId, String author, String content, Instant createdAt) {}
}
