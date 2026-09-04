package com.akhil.social.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public class MessageDtos {
    public record SendMessageRequest(
            @NotBlank String platform,
            @NotBlank String conversation,
            @NotBlank @Size(max = 4000) String content,
            String mediaUrl
    ) {}
    public record MessageResponse(
            Long id, Long conversationId, Long senderId, String senderName,
            String content, String mediaUrl, boolean mine, Instant createdAt
    ) {}
}
