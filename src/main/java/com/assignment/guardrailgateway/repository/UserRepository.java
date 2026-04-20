package com.assignment.guardrailgateway.repository;

import com.assignment.guardrailgateway.model.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
}
