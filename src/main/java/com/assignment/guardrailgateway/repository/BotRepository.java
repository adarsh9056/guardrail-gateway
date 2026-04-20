package com.assignment.guardrailgateway.repository;

import com.assignment.guardrailgateway.model.Bot;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BotRepository extends JpaRepository<Bot, Long> {
}
