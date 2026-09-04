package com.akhil.social.controller;

import com.akhil.social.dto.PostDtos.CommentRequest;
import com.akhil.social.dto.PostDtos.CommentResponse;
import com.akhil.social.entity.User;
import com.akhil.social.service.PostService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/comments")
public class CommentController {
    private final PostService postService;
    public CommentController(PostService postService) { this.postService = postService; }

    @GetMapping("/post/{postId}")
    public List<CommentResponse> list(@PathVariable Long postId) {
        return postService.getComments(postId);
    }

    @PostMapping("/post/{postId}")
    public CommentResponse create(@PathVariable Long postId, @Valid @RequestBody CommentRequest req,
                                  @AuthenticationPrincipal User user) {
        return postService.comment(postId, req, user);
    }
}
