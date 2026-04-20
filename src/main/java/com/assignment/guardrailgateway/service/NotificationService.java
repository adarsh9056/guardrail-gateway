package com.assignment.guardrailgateway.service;

import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final String PENDING_USERS_SET = "pending_notif_users";

    private final StringRedisTemplate redisTemplate;

    public NotificationService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void notifyBotInteraction(long userId, String notification) {
        String cooldownKey = "user:" + userId + ":notif_cooldown";
        String pendingListKey = "user:" + userId + ":pending_notifs";

        Boolean created = redisTemplate.opsForValue().setIfAbsent(cooldownKey, "1", Duration.ofMinutes(15));
        if (Boolean.TRUE.equals(created)) {
            log.info("Push Notification Sent to User {}: {}", userId, notification);
            return;
        }

        redisTemplate.opsForList().rightPush(pendingListKey, notification);
        redisTemplate.opsForSet().add(PENDING_USERS_SET, String.valueOf(userId));
    }

    @Scheduled(fixedDelayString = "${app.scheduler.notif-sweep-ms:300000}")
    public void sweepPendingNotifications() {
        var users = redisTemplate.opsForSet().members(PENDING_USERS_SET);
        if (users == null || users.isEmpty()) {
            return;
        }

        for (String userId : users) {
            String listKey = "user:" + userId + ":pending_notifs";
            Long size = redisTemplate.opsForList().size(listKey);
            if (size == null || size == 0) {
                redisTemplate.opsForSet().remove(PENDING_USERS_SET, userId);
                continue;
            }

            List<String> messages = redisTemplate.opsForList().range(listKey, 0, -1);
            redisTemplate.delete(listKey);
            redisTemplate.opsForSet().remove(PENDING_USERS_SET, userId);

            if (messages == null || messages.isEmpty()) {
                continue;
            }

            String first = messages.get(0);
            int others = messages.size() - 1;
            if (others <= 0) {
                log.info("Summarized Push Notification: {}", first);
            } else {
                log.info("Summarized Push Notification: {} and {} others interacted with your posts.", first, others);
            }
        }
    }
}
