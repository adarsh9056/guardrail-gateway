package com.assignment.guardrailgateway.service;

import com.assignment.guardrailgateway.dto.CreateCommentRequest;
import com.assignment.guardrailgateway.dto.CreatePostRequest;
import com.assignment.guardrailgateway.dto.LikePostRequest;
import com.assignment.guardrailgateway.exception.GuardrailViolationException;
import com.assignment.guardrailgateway.exception.NotFoundException;
import com.assignment.guardrailgateway.model.AuthorType;
import com.assignment.guardrailgateway.model.Bot;
import com.assignment.guardrailgateway.model.Comment;
import com.assignment.guardrailgateway.model.Post;
import com.assignment.guardrailgateway.model.PostLike;
import com.assignment.guardrailgateway.model.UserEntity;
import com.assignment.guardrailgateway.repository.BotRepository;
import com.assignment.guardrailgateway.repository.CommentRepository;
import com.assignment.guardrailgateway.repository.PostLikeRepository;
import com.assignment.guardrailgateway.repository.PostRepository;
import com.assignment.guardrailgateway.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final BotRepository botRepository;
    private final PostLikeRepository postLikeRepository;
    private final GuardrailService guardrailService;
    private final ViralityService viralityService;
    private final NotificationService notificationService;

    public PostService(
            PostRepository postRepository,
            CommentRepository commentRepository,
            UserRepository userRepository,
            BotRepository botRepository,
            PostLikeRepository postLikeRepository,
            GuardrailService guardrailService,
            ViralityService viralityService,
            NotificationService notificationService) {
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
        this.botRepository = botRepository;
        this.postLikeRepository = postLikeRepository;
        this.guardrailService = guardrailService;
        this.viralityService = viralityService;
        this.notificationService = notificationService;
    }

    @Transactional
    public Post createPost(CreatePostRequest request) {
        validateAuthor(request.getAuthorType(), request.getAuthorId());

        Post post = new Post();
        post.setAuthorType(request.getAuthorType());
        post.setAuthorId(request.getAuthorId());
        post.setContent(request.getContent());
        return postRepository.save(post);
    }

    @Transactional
    public Comment addComment(Long postId, CreateCommentRequest request) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("Post not found: " + postId));

        validateAuthor(request.getAuthorType(), request.getAuthorId());
        Comment parent = null;
        int depth = 1;
        if (request.getParentCommentId() != null) {
            parent = commentRepository.findById(request.getParentCommentId())
                    .orElseThrow(() -> new NotFoundException("Parent comment not found: " + request.getParentCommentId()));
            if (!parent.getPost().getId().equals(postId)) {
                throw new GuardrailViolationException("Parent comment does not belong to the target post", HttpStatus.BAD_REQUEST);
            }
            depth = parent.getDepthLevel() + 1;
        }

        guardrailService.enforceVerticalCap(depth);

        boolean acquiredBotSlot = false;

        if (request.getAuthorType() == AuthorType.BOT) {
            acquiredBotSlot = guardrailService.tryAcquireHorizontalSlot(postId);
            if (!acquiredBotSlot) {
                throw new GuardrailViolationException("Horizontal cap exceeded: max 100 bot replies per post", HttpStatus.TOO_MANY_REQUESTS);
            }

            try {
                Long targetHumanForCooldown = resolveTargetHumanForCooldown(post, parent);
                if (targetHumanForCooldown != null) {
                    guardrailService.enforceCooldown(request.getAuthorId(), targetHumanForCooldown);
                }
            } catch (RuntimeException ex) {
                guardrailService.releaseHorizontalSlot(postId);
                throw ex;
            }
        }

        try {
            Comment comment = new Comment();
            comment.setPost(post);
            comment.setParentComment(parent);
            comment.setAuthorId(request.getAuthorId());
            comment.setAuthorType(request.getAuthorType());
            comment.setContent(request.getContent());
            comment.setDepthLevel(depth);

            Comment saved = commentRepository.save(comment);

            if (request.getAuthorType() == AuthorType.BOT) {
                viralityService.addScore(postId, ViralityService.InteractionType.BOT_REPLY);
                notifyHumanPostOwnerIfNeeded(post, request.getAuthorId());
            } else {
                viralityService.addScore(postId, ViralityService.InteractionType.HUMAN_COMMENT);
            }

            return saved;
        } catch (RuntimeException ex) {
            if (acquiredBotSlot) {
                guardrailService.releaseHorizontalSlot(postId);
            }
            throw ex;
        }
    }

    @Transactional
    public void likePost(Long postId, LikePostRequest request) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("Post not found: " + postId));

        UserEntity user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new NotFoundException("User not found: " + request.getUserId()));

        postLikeRepository.findByPostAndUser(post, user).ifPresent(existing -> {
            throw new GuardrailViolationException("Post already liked by this user", HttpStatus.CONFLICT);
        });

        PostLike like = new PostLike();
        like.setPost(post);
        like.setUser(user);
        postLikeRepository.save(like);

        viralityService.addScore(postId, ViralityService.InteractionType.HUMAN_LIKE);
    }

    private void validateAuthor(AuthorType authorType, Long authorId) {
        if (authorType == AuthorType.USER) {
            userRepository.findById(authorId).orElseThrow(() -> new NotFoundException("User not found: " + authorId));
            return;
        }
        botRepository.findById(authorId).orElseThrow(() -> new NotFoundException("Bot not found: " + authorId));
    }

    private Long resolveTargetHumanForCooldown(Post post, Comment parent) {
        if (parent != null && parent.getAuthorType() == AuthorType.USER) {
            return parent.getAuthorId();
        }
        if (post.getAuthorType() == AuthorType.USER) {
            return post.getAuthorId();
        }
        return null;
    }

    private void notifyHumanPostOwnerIfNeeded(Post post, Long botId) {
        if (post.getAuthorType() != AuthorType.USER) {
            return;
        }

        Bot bot = botRepository.findById(botId)
                .orElseThrow(() -> new NotFoundException("Bot not found: " + botId));

        String message = "Bot " + bot.getName() + " replied to your post";
        notificationService.notifyBotInteraction(post.getAuthorId(), message);
    }
}
