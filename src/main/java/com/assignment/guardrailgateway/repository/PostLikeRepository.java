package com.assignment.guardrailgateway.repository;

import com.assignment.guardrailgateway.model.Post;
import com.assignment.guardrailgateway.model.PostLike;
import com.assignment.guardrailgateway.model.UserEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
    Optional<PostLike> findByPostAndUser(Post post, UserEntity user);
}
