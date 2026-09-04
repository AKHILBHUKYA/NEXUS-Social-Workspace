package com.akhil.social.controller;

import com.akhil.social.dto.MessageDtos.*;
import com.akhil.social.entity.User;
import com.akhil.social.service.MessageService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/messages")
public class MessageController {
    private final MessageService messageService;
    public MessageController(MessageService messageService) { this.messageService = messageService; }

    @GetMapping
    public List<MessageResponse> list(
            @RequestParam String platform,
            @RequestParam String conversation,
            @AuthenticationPrincipal User user) {
        return messageService.list(platform, conversation, user);
    }

    @PostMapping
    public MessageResponse send(@Valid @RequestBody SendMessageRequest req, @AuthenticationPrincipal User user) {
        return messageService.send(req, user);
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id, @AuthenticationPrincipal User user) {
        messageService.delete(id, user);
        return Map.of("success", true);
    }
}
