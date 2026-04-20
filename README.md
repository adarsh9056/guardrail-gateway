# Guardrail Gateway (Spring Boot Microservice)

High-performance Spring Boot 3 microservice implementing a central API gateway + Redis guardrail system for concurrent bot/human interactions.

## Tech Stack

- Java 17
- Spring Boot 3.x (Web, JPA, Redis, Validation, Scheduling)
- PostgreSQL (source of truth)
- Redis (distributed gatekeeper + counters + cooldowns + notification queues)
- Docker Compose for local infra

## Local Setup

1. Start infrastructure:

```bash
docker compose up -d
```

This project intentionally uses non-default host ports to avoid conflicts with a locally running PostgreSQL or Redis:

- PostgreSQL: `localhost:5433`
- Redis: `localhost:6380`

2. Run service:

```bash
.tools/apache-maven-3.9.9/bin/mvn spring-boot:run
```

3. Service URL: `http://localhost:8080`

If you want to point the app at different infrastructure, override `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `REDIS_HOST`, or `REDIS_PORT`.
If port `8080` is already in use on your machine, start the app with `SERVER_PORT=8081 .tools/apache-maven-3.9.9/bin/mvn spring-boot:run`.

## API Endpoints

- `POST /api/posts` - create a post
- `POST /api/posts/{postId}/comments` - add a comment
- `POST /api/posts/{postId}/like` - like a post

A sample Postman collection is provided at `postman/guardrail-gateway.postman_collection.json`.

## Data Model

- `users`: `id`, `username`, `is_premium`
- `bots`: `id`, `name`, `persona_description`
- `posts`: `id`, `author_id`, `author_type`, `content`, `created_at`
- `comments`: `id`, `post_id`, `parent_comment_id`, `author_id`, `author_type`, `content`, `depth_level`, `created_at`
- `post_likes`: normalized likes table with unique `(post_id, user_id)`

## Redis Guardrails Implementation

### 1) Virality Score (real-time)

`ViralityService` writes to `post:{id}:virality_score` using atomic `INCRBY` through `StringRedisTemplate`:

- Bot reply: `+1`
- Human like: `+20`
- Human comment: `+50`

### 2) Atomic Locks / Caps

Implemented in `GuardrailService`:

- **Horizontal cap**: `post:{id}:bot_count` incremented atomically with Redis `INCR`. If result > 100, immediate `DECR` and request rejected with HTTP `429`.
- **Vertical cap**: reject if calculated `depth_level > 20` (HTTP `429`).
- **Cooldown cap**: `cooldown:bot_{id}:human_{id}` set with `SETNX` + TTL `10m`. Existing key blocks request with HTTP `429`.

### 3) Notification Engine

Implemented in `NotificationService`:

- If `user:{id}:notif_cooldown` absent, log immediate push and set 15-minute cooldown.
- If cooldown exists, queue message into `user:{id}:pending_notifs` list and track user in set `pending_notif_users`.
- Scheduled sweeper (`@Scheduled`, every 5 min by default) drains per-user list and logs:
  - `Summarized Push Notification: <first-message> and <N> others interacted with your posts.`

## Concurrency and Integrity Guarantees

- The service is stateless: no in-memory maps/static counters used for guardrail state.
- Redis is the single runtime coordinator for counters, cooldowns, and pending notifications.
- Bot-comment writes use **Redis-first gating** before DB persistence.
- For race bursts (e.g., 200 concurrent bot comment requests), atomic `INCR` + rollback-on-overflow guarantees no more than 100 accepted bot comments per post.
- Database transactions only commit if Redis guardrails pass.
- If DB save fails after acquiring a bot slot, the code compensates by decrementing the Redis bot counter to avoid drift.

## Notes

- `spring.jpa.hibernate.ddl-auto=update` is enabled for assignment convenience.
- You can tune sweep interval with `app.scheduler.notif-sweep-ms` in `application.yml`.
- Spring Data Redis repositories are disabled because Redis is used only as a runtime state store via `StringRedisTemplate`, not as a repository-backed entity store.
