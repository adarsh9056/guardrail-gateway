package com.assignment.guardrailgateway.service;

import com.assignment.guardrailgateway.exception.GuardrailViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.time.Duration;

@Service
public class GuardrailService {

    private static final int MAX_BOT_REPLIES_PER_POST = 100;
    private static final int MAX_DEPTH_LEVEL = 20;

    private final StringRedisTemplate redisTemplate;

    public GuardrailService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void enforceVerticalCap(int depthLevel) {
        if (depthLevel > MAX_DEPTH_LEVEL) {
            throw new GuardrailViolationException("Vertical cap exceeded: depth level cannot be greater than 20", HttpStatus.TOO_MANY_REQUESTS);
        }
    }

    public void enforceCooldown(long botId, long humanId) {
        String key = "cooldown:bot_" + botId + ":human_" + humanId;
        Boolean created = redisTemplate.opsForValue().setIfAbsent(key, "1", Duration.ofMinutes(10));
        if (Boolean.FALSE.equals(created)) {
            throw new GuardrailViolationException("Cooldown cap active: bot cannot interact with this user yet", HttpStatus.TOO_MANY_REQUESTS);
        }
    }

    public boolean tryAcquireHorizontalSlot(long postId) {
        String key = "post:" + postId + ":bot_count";
        Long count = redisTemplate.opsForValue().increment(key);
        if (count == null) {
            throw new GuardrailViolationException("Unable to process guardrail counter", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        if (count > MAX_BOT_REPLIES_PER_POST) {
            redisTemplate.opsForValue().decrement(key);
            return false;
        }
        return true;
    }

    public void releaseHorizontalSlot(long postId) {
        redisTemplate.opsForValue().decrement("post:" + postId + ":bot_count");
    }
}
