package com.akhil.social.controller;

import com.akhil.social.entity.Follow;
import com.akhil.social.entity.User;
import com.akhil.social.exception.ApiException;
import com.akhil.social.repository.FollowRepository;
import com.akhil.social.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/follows")
public class FollowController {
    private final FollowRepository followRepository;
    private final UserRepository userRepository;

    public FollowController(FollowRepository followRepository, UserRepository userRepository) {
        this.followRepository = followRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/{userId}")
    public Map<String, Object> follow(@PathVariable Long userId, @AuthenticationPrincipal User user) {
        if (userId.equals(user.getId())) throw new ApiException("Cannot follow yourself", HttpStatus.BAD_REQUEST);
        if (!userRepository.existsById(userId)) throw new ApiException("User not found", HttpStatus.NOT_FOUND);
        if (followRepository.existsByFollowerIdAndFollowingId(user.getId(), userId))
            throw new ApiException("Already following", HttpStatus.CONFLICT);
        Follow f = new Follow();
        f.setFollowerId(user.getId());
        f.setFollowingId(userId);
        followRepository.save(f);
        return Map.of("success", true, "following", true);
    }

    @DeleteMapping("/{userId}")
    public Map<String, Object> unfollow(@PathVariable Long userId, @AuthenticationPrincipal User user) {
        followRepository.deleteByFollowerIdAndFollowingId(user.getId(), userId);
        return Map.of("success", true, "following", false);
    }

    @GetMapping("/followers")
    public List<Map<String, Object>> followers(@AuthenticationPrincipal User user) {
        return followRepository.findByFollowingId(user.getId()).stream()
                .map(f -> userRepository.findById(f.getFollowerId())
                        .map(u -> Map.<String, Object>of("id", u.getId(), "username", u.getUsername(), "displayName", u.getDisplayName() != null ? u.getDisplayName() : u.getUsername()))
                        .orElse(null))
                .filter(x -> x != null)
                .collect(Collectors.toList());
    }

    @GetMapping("/following")
    public List<Map<String, Object>> following(@AuthenticationPrincipal User user) {
        return followRepository.findByFollowerId(user.getId()).stream()
                .map(f -> userRepository.findById(f.getFollowingId())
                        .map(u -> Map.<String, Object>of("id", u.getId(), "username", u.getUsername(), "displayName", u.getDisplayName() != null ? u.getDisplayName() : u.getUsername()))
                        .orElse(null))
                .filter(x -> x != null)
                .collect(Collectors.toList());
    }
}
