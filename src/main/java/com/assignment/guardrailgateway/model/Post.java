package com.assignment.guardrailgateway.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "posts")
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "author_type", nullable = false)
    private AuthorType authorType;

    @Column(nullable = false, length = 5000)
    private String content;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getAuthorId() { return authorId; }
    public AuthorType getAuthorType() { return authorType; }
    public String getContent() { return content; }
    public Instant getCreatedAt() { return createdAt; }

    public void setId(Long id) { this.id = id; }
    public void setAuthorId(Long authorId) { this.authorId = authorId; }
    public void setAuthorType(AuthorType authorType) { this.authorType = authorType; }
    public void setContent(String content) { this.content = content; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
