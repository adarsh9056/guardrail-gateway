package com.assignment.guardrailgateway.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.assignment.guardrailgateway.dto.CreateCommentRequest;
import com.assignment.guardrailgateway.exception.GuardrailViolationException;
import com.assignment.guardrailgateway.model.AuthorType;
import com.assignment.guardrailgateway.model.Bot;
import com.assignment.guardrailgateway.model.Comment;
import com.assignment.guardrailgateway.model.Post;
import com.assignment.guardrailgateway.repository.BotRepository;
import com.assignment.guardrailgateway.repository.CommentRepository;
import com.assignment.guardrailgateway.repository.PostLikeRepository;
import com.assignment.guardrailgateway.repository.PostRepository;
import com.assignment.guardrailgateway.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BotRepository botRepository;

    @Mock
    private PostLikeRepository postLikeRepository;

    @Mock
    private GuardrailService guardrailService;

    @Mock
    private ViralityService viralityService;

    @Mock
    private NotificationService notificationService;

    private PostService postService;

    @BeforeEach
    void setUp() {
        postService = new PostService(
                postRepository,
                commentRepository,
                userRepository,
                botRepository,
                postLikeRepository,
                guardrailService,
                viralityService,
                notificationService);
    }

    @Test
    void addComment_releasesBotSlotWhenCooldownCheckFails() {
        Long postId = 1L;
        Long botId = 7L;
        Long userId = 42L;

        when(postRepository.findById(postId)).thenReturn(Optional.of(userOwnedPost(postId, userId)));
        when(botRepository.findById(botId)).thenReturn(Optional.of(bot(botId)));
        when(guardrailService.tryAcquireHorizontalSlot(postId)).thenReturn(true);
        doThrow(new GuardrailViolationException("Cooldown cap active", HttpStatus.TOO_MANY_REQUESTS))
                .when(guardrailService)
                .enforceCooldown(botId, userId);

        assertThrows(GuardrailViolationException.class, () -> postService.addComment(postId, botCommentRequest(botId)));

        verify(guardrailService).releaseHorizontalSlot(postId);
        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    void addComment_releasesBotSlotWhenDatabaseSaveFails() {
        Long postId = 1L;
        Long botId = 7L;
        Long userId = 42L;

        when(postRepository.findById(postId)).thenReturn(Optional.of(userOwnedPost(postId, userId)));
        when(botRepository.findById(botId)).thenReturn(Optional.of(bot(botId)));
        when(guardrailService.tryAcquireHorizontalSlot(postId)).thenReturn(true);
        when(commentRepository.save(any(Comment.class))).thenThrow(new RuntimeException("db write failed"));

        assertThrows(RuntimeException.class, () -> postService.addComment(postId, botCommentRequest(botId)));

        verify(guardrailService).releaseHorizontalSlot(postId);
    }

    private static CreateCommentRequest botCommentRequest(Long botId) {
        CreateCommentRequest request = new CreateCommentRequest();
        request.setAuthorId(botId);
        request.setAuthorType(AuthorType.BOT);
        request.setContent("Bot reply");
        return request;
    }

    private static Post userOwnedPost(Long postId, Long userId) {
        Post post = new Post();
        post.setId(postId);
        post.setAuthorId(userId);
        post.setAuthorType(AuthorType.USER);
        post.setContent("Post");
        return post;
    }

    private static Bot bot(Long botId) {
        Bot bot = new Bot();
        bot.setId(botId);
        bot.setName("assistant-bot");
        bot.setPersonaDescription("Helpful persona");
        return bot;
    }
}
