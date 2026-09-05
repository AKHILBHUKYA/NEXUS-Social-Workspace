package com.akhil.social.controller;

import com.akhil.social.entity.User;
import com.akhil.social.repository.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/search")
public class SearchController {
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final ContactRepository contactRepository;
    private final MessageRepository messageRepository;

    public SearchController(UserRepository userRepository, PostRepository postRepository,
                            ContactRepository contactRepository, MessageRepository messageRepository) {
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.contactRepository = contactRepository;
        this.messageRepository = messageRepository;
    }

    @GetMapping
    public Map<String, Object> search(@RequestParam String q, @AuthenticationPrincipal User user) {
        String query = q == null ? "" : q.trim().toLowerCase();
        Map<String, Object> result = new LinkedHashMap<>();
        if (query.isBlank()) {
            result.put("users", List.of());
            result.put("posts", List.of());
            result.put("contacts", List.of());
            result.put("messages", List.of());
            return result;
        }
        result.put("users", userRepository.findAll().stream()
                .filter(u -> u.getUsername().toLowerCase().contains(query) ||
                        (u.getDisplayName() != null && u.getDisplayName().toLowerCase().contains(query)))
                .limit(10)
                .map(u -> Map.of("id", u.getId(), "username", u.getUsername(), "displayName", u.getDisplayName() != null ? u.getDisplayName() : u.getUsername()))
                .collect(Collectors.toList()));
        result.put("posts", postRepository.findAll().stream()
                .filter(p -> p.getContent().toLowerCase().contains(query))
                .limit(10)
                .map(p -> Map.of("id", p.getId(), "content", p.getContent().substring(0, Math.min(120, p.getContent().length())), "platform", p.getPlatform()))
                .collect(Collectors.toList()));
        result.put("contacts", contactRepository.findByOwnerIdOrderByNameAsc(user.getId()).stream()
                .filter(c -> c.getName().toLowerCase().contains(query))
                .limit(10)
                .map(c -> Map.of("id", c.getId(), "name", c.getName()))
                .collect(Collectors.toList()));
        result.put("messages", List.of()); // avoid heavy scan in demo
        return result;
    }
}
