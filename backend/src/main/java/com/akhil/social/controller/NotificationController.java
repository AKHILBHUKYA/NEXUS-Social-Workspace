package com.akhil.social.controller;

import com.akhil.social.entity.Notification;
import com.akhil.social.entity.User;
import com.akhil.social.exception.ApiException;
import com.akhil.social.repository.NotificationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationRepository repo;
    public NotificationController(NotificationRepository repo) { this.repo = repo; }

    public record NotifResponse(Long id, String type, String message, boolean read, String createdAt) {}

    @GetMapping
    public Map<String, Object> list(@AuthenticationPrincipal User user) {
        List<NotifResponse> items = repo.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(n -> new NotifResponse(n.getId(), n.getType(), n.getMessage(), n.isRead(), n.getCreatedAt().toString()))
                .collect(Collectors.toList());
        long unread = repo.countByUserIdAndReadFalse(user.getId());
        return Map.of("items", items, "unreadCount", unread);
    }

    @PatchMapping("/{id}/read")
    public Map<String, Object> markRead(@PathVariable Long id, @AuthenticationPrincipal User user) {
        Notification n = repo.findById(id).orElseThrow(() -> new ApiException("Not found", HttpStatus.NOT_FOUND));
        if (!n.getUser().getId().equals(user.getId())) throw new ApiException("Forbidden", HttpStatus.FORBIDDEN);
        n.setRead(true);
        repo.save(n);
        return Map.of("success", true);
    }

    @PatchMapping("/read-all")
    @Transactional
    public Map<String, Object> markAll(@AuthenticationPrincipal User user) {
        repo.markAllRead(user.getId());
        return Map.of("success", true);
    }
}
