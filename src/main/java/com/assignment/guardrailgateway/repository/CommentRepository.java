package com.assignment.guardrailgateway.repository;

import com.assignment.guardrailgateway.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {
}
