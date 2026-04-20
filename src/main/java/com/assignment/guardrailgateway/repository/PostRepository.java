package com.assignment.guardrailgateway.repository;

import com.assignment.guardrailgateway.model.Post;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {
}
