package com.assignment.guardrailgateway.dto;

import com.assignment.guardrailgateway.model.AuthorType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateCommentRequest {

    @NotNull
    private Long authorId;

    @NotNull
    private AuthorType authorType;

    private Long parentCommentId;

    @NotBlank
    private String content;

    public Long getAuthorId() { return authorId; }
    public AuthorType getAuthorType() { return authorType; }
    public Long getParentCommentId() { return parentCommentId; }
    public String getContent() { return content; }

    public void setAuthorId(Long authorId) { this.authorId = authorId; }
    public void setAuthorType(AuthorType authorType) { this.authorType = authorType; }
    public void setParentCommentId(Long parentCommentId) { this.parentCommentId = parentCommentId; }
    public void setContent(String content) { this.content = content; }
}
