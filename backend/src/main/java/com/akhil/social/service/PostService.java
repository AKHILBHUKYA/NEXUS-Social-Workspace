package com.akhil.social.service;

import com.akhil.social.dto.PostDtos.*;
import com.akhil.social.entity.*;
import com.akhil.social.exception.ApiException;
import com.akhil.social.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PostService {
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PostLikeRepository likeRepository;
    private final PostSaveRepository saveRepository;
    private final PostShareRepository shareRepository;
    private final NotificationRepository notificationRepository;

    public PostService(PostRepository postRepository, CommentRepository commentRepository,
                       PostLikeRepository likeRepository, PostSaveRepository saveRepository,
                       PostShareRepository shareRepository, NotificationRepository notificationRepository) {
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.likeRepository = likeRepository;
        this.saveRepository = saveRepository;
        this.shareRepository = shareRepository;
        this.notificationRepository = notificationRepository;
    }

    public Page<PostResponse> list(String platform, int page, int size, User current) {
        Page<Post> posts = (platform == null || platform.isBlank() || "all".equalsIgnoreCase(platform))
                ? postRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size))
                : postRepository.findByPlatformIgnoreCaseOrderByCreatedAtDesc(platform, PageRequest.of(page, size));
        return posts.map(p -> toResponse(p, current));
    }

    @Transactional
    public PostResponse create(CreatePostRequest req, User user) {
        Post p = new Post();
        p.setUser(user);
        p.setPlatform(req.platform().toLowerCase());
        p.setContent(req.content().trim());
        p.setMediaUrl(req.mediaUrl());
        p.setMediaType(req.mediaType());
        postRepository.save(p);
        return toResponse(p, user);
    }

    @Transactional
    public PostResponse like(Long postId, User user) {
        Post p = findPost(postId);
        if (likeRepository.existsByPostIdAndUserId(postId, user.getId())) {
            throw new ApiException("Already liked", HttpStatus.CONFLICT);
        }
        PostLike like = new PostLike();
        like.setPostId(postId);
        like.setUserId(user.getId());
        likeRepository.save(like);
        p.setLikeCount(p.getLikeCount() + 1);
        postRepository.save(p);
        notify(p.getUser(), "LIKE", user.getDisplayName() + " liked your post", postId, "post");
        return toResponse(p, user);
    }

    @Transactional
    public PostResponse unlike(Long postId, User user) {
        Post p = findPost(postId);
        if (!likeRepository.existsByPostIdAndUserId(postId, user.getId())) {
            throw new ApiException("Not liked", HttpStatus.CONFLICT);
        }
        likeRepository.deleteByPostIdAndUserId(postId, user.getId());
        p.setLikeCount(Math.max(0, p.getLikeCount() - 1));
        postRepository.save(p);
        return toResponse(p, user);
    }

    @Transactional
    public PostResponse save(Long postId, User user) {
        Post p = findPost(postId);
        if (saveRepository.existsByPostIdAndUserId(postId, user.getId())) {
            throw new ApiException("Already saved", HttpStatus.CONFLICT);
        }
        PostSave s = new PostSave();
        s.setPostId(postId);
        s.setUserId(user.getId());
        saveRepository.save(s);
        p.setSaveCount(p.getSaveCount() + 1);
        postRepository.save(p);
        return toResponse(p, user);
    }

    @Transactional
    public PostResponse unsave(Long postId, User user) {
        Post p = findPost(postId);
        if (!saveRepository.existsByPostIdAndUserId(postId, user.getId())) {
            throw new ApiException("Not saved", HttpStatus.CONFLICT);
        }
        saveRepository.deleteByPostIdAndUserId(postId, user.getId());
        p.setSaveCount(Math.max(0, p.getSaveCount() - 1));
        postRepository.save(p);
        return toResponse(p, user);
    }

    @Transactional
    public PostResponse share(Long postId, User user) {
        Post p = findPost(postId);
        PostShare sh = new PostShare();
        sh.setPostId(postId);
        sh.setUserId(user.getId());
        shareRepository.save(sh);
        p.setShareCount(p.getShareCount() + 1);
        postRepository.save(p);
        notify(p.getUser(), "SHARE", user.getDisplayName() + " shared your post", postId, "post");
        return toResponse(p, user);
    }

    @Transactional
    public CommentResponse comment(Long postId, CommentRequest req, User user) {
        Post p = findPost(postId);
        Comment c = new Comment();
        c.setPost(p);
        c.setUser(user);
        c.setContent(req.content().trim());
        commentRepository.save(c);
        p.setCommentCount(p.getCommentCount() + 1);
        postRepository.save(p);
        notify(p.getUser(), "COMMENT", user.getDisplayName() + " commented on your post", postId, "post");
        return new CommentResponse(c.getId(), postId, user.getId(), user.getDisplayName(), c.getContent(), c.getCreatedAt());
    }

    public List<CommentResponse> getComments(Long postId) {
        return commentRepository.findByPostIdOrderByCreatedAtAsc(postId).stream()
                .map(c -> new CommentResponse(c.getId(), postId, c.getUser().getId(),
                        c.getUser().getDisplayName(), c.getContent(), c.getCreatedAt()))
                .collect(Collectors.toList());
    }

    @Transactional
    public void delete(Long postId, User user) {
        Post p = findPost(postId);
        if (!p.getUser().getId().equals(user.getId()) && !"ADMIN".equals(user.getRole())) {
            throw new ApiException("Not authorized to delete this post", HttpStatus.FORBIDDEN);
        }
        postRepository.delete(p);
    }

    public List<PostResponse> saved(User user) {
        return saveRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(s -> postRepository.findById(s.getPostId()).orElse(null))
                .filter(p -> p != null)
                .map(p -> toResponse(p, user))
                .collect(Collectors.toList());
    }

    private Post findPost(Long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new ApiException("Post not found", HttpStatus.NOT_FOUND));
    }

    private PostResponse toResponse(Post p, User current) {
        boolean liked = current != null && likeRepository.existsByPostIdAndUserId(p.getId(), current.getId());
        boolean saved = current != null && saveRepository.existsByPostIdAndUserId(p.getId(), current.getId());
        return new PostResponse(p.getId(), p.getUser().getId(), p.getUser().getDisplayName(),
                p.getPlatform(), p.getContent(), p.getMediaUrl(), p.getMediaType(),
                p.getLikeCount(), p.getCommentCount(), p.getShareCount(), p.getSaveCount(),
                liked, saved, p.getCreatedAt());
    }

    private void notify(User target, String type, String msg, Long refId, String refType) {
        if (target == null) return;
        Notification n = new Notification();
        n.setUser(target);
        n.setType(type);
        n.setMessage(msg);
        n.setReferenceId(refId);
        n.setReferenceType(refType);
        notificationRepository.save(n);
    }
}
