package com.assignment.guardrailgateway.controller;

import com.assignment.guardrailgateway.dto.ApiResponse;
import com.assignment.guardrailgateway.dto.CreateCommentRequest;
import com.assignment.guardrailgateway.dto.CreatePostRequest;
import com.assignment.guardrailgateway.dto.LikePostRequest;
import com.assignment.guardrailgateway.model.Comment;
import com.assignment.guardrailgateway.model.Post;
import com.assignment.guardrailgateway.service.PostService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Post createPost(@Valid @RequestBody CreatePostRequest request) {
        return postService.createPost(request);
    }

    @PostMapping("/{postId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public Comment addComment(@PathVariable Long postId, @Valid @RequestBody CreateCommentRequest request) {
        return postService.addComment(postId, request);
    }

    @PostMapping("/{postId}/like")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse likePost(@PathVariable Long postId, @Valid @RequestBody LikePostRequest request) {
        postService.likePost(postId, request);
        return new ApiResponse("Post liked successfully");
    }
}
