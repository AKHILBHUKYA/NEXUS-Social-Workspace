package com.akhil.social.controller;

import com.akhil.social.dto.PostDtos.*;
import com.akhil.social.entity.User;
import com.akhil.social.service.PostService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/posts")
public class PostController {
    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping
    public Page<PostResponse> list(
            @RequestParam(required = false) String platform,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User user) {
        return postService.list(platform, page, size, user);
    }

    @PostMapping
    public PostResponse create(@Valid @RequestBody CreatePostRequest req, @AuthenticationPrincipal User user) {
        return postService.create(req, user);
    }

    @PostMapping("/{id}/like")
    public PostResponse like(@PathVariable Long id, @AuthenticationPrincipal User user) {
        return postService.like(id, user);
    }

    @DeleteMapping("/{id}/like")
    public PostResponse unlike(@PathVariable Long id, @AuthenticationPrincipal User user) {
        return postService.unlike(id, user);
    }

    @PostMapping("/{id}/save")
    public PostResponse save(@PathVariable Long id, @AuthenticationPrincipal User user) {
        return postService.save(id, user);
    }

    @DeleteMapping("/{id}/save")
    public PostResponse unsave(@PathVariable Long id, @AuthenticationPrincipal User user) {
        return postService.unsave(id, user);
    }

    @PostMapping("/{id}/share")
    public PostResponse share(@PathVariable Long id, @AuthenticationPrincipal User user) {
        return postService.share(id, user);
    }

    @GetMapping("/saved")
    public List<PostResponse> saved(@AuthenticationPrincipal User user) {
        return postService.saved(user);
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id, @AuthenticationPrincipal User user) {
        postService.delete(id, user);
        return Map.of("success", true, "message", "Post deleted");
    }

    @GetMapping("/{id}/comments")
    public List<CommentResponse> comments(@PathVariable Long id) {
        return postService.getComments(id);
    }

    @PostMapping("/{id}/comments")
    public CommentResponse addComment(@PathVariable Long id, @Valid @RequestBody CommentRequest req,
                                      @AuthenticationPrincipal User user) {
        return postService.comment(id, req, user);
    }
}
