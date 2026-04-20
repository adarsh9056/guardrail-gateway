package com.assignment.guardrailgateway.dto;

import com.assignment.guardrailgateway.model.AuthorType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreatePostRequest {

    @NotNull
    private Long authorId;

    @NotNull
    private AuthorType authorType;

    @NotBlank
    private String content;

    public Long getAuthorId() { return authorId; }
    public AuthorType getAuthorType() { return authorType; }
    public String getContent() { return content; }

    public void setAuthorId(Long authorId) { this.authorId = authorId; }
    public void setAuthorType(AuthorType authorType) { this.authorType = authorType; }
    public void setContent(String content) { this.content = content; }
}
