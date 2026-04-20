package com.assignment.guardrailgateway.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "comments")
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    private Comment parentComment;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "author_type", nullable = false)
    private AuthorType authorType;

    @Column(nullable = false, length = 3000)
    private String content;

    @Column(name = "depth_level", nullable = false)
    private int depthLevel;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public Post getPost() { return post; }
    public Comment getParentComment() { return parentComment; }
    public Long getAuthorId() { return authorId; }
    public AuthorType getAuthorType() { return authorType; }
    public String getContent() { return content; }
    public int getDepthLevel() { return depthLevel; }
    public Instant getCreatedAt() { return createdAt; }

    public void setId(Long id) { this.id = id; }
    public void setPost(Post post) { this.post = post; }
    public void setParentComment(Comment parentComment) { this.parentComment = parentComment; }
    public void setAuthorId(Long authorId) { this.authorId = authorId; }
    public void setAuthorType(AuthorType authorType) { this.authorType = authorType; }
    public void setContent(String content) { this.content = content; }
    public void setDepthLevel(int depthLevel) { this.depthLevel = depthLevel; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
