package com.assignment.guardrailgateway.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class ViralityService {

    public enum InteractionType {
        BOT_REPLY(1),
        HUMAN_LIKE(20),
        HUMAN_COMMENT(50);

        private final int points;

        InteractionType(int points) {
            this.points = points;
        }

        public int points() {
            return points;
        }
    }

    private final StringRedisTemplate redisTemplate;

    public ViralityService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public long addScore(long postId, InteractionType type) {
        Long value = redisTemplate.opsForValue().increment("post:" + postId + ":virality_score", type.points());
        return value == null ? 0L : value;
    }
}
