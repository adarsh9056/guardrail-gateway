# Guardrail Gateway (Spring Boot Microservice)

High-performance Spring Boot 3 microservice implementing a central API gateway + Redis guardrail system for concurrent bot/human interactions.

---

## Prerequisites

- Java 17+
- Docker & Docker Compose
- Maven 3.x (or use the bundled wrapper at `.tools/apache-maven-3.9.9/bin/mvn`)

---

## Tech Stack

- Java 17
- Spring Boot 3.x (Web, JPA, Redis, Validation, Scheduling)
- PostgreSQL (source of truth)
- Redis (distributed gatekeeper + counters + cooldowns + notification queues)
- Docker Compose for local infrastructure

---

## Local Setup

### 1. Start Infrastructure

```bash
docker compose up -d
```

This project intentionally uses non-default host ports to avoid conflicts with a locally running PostgreSQL or Redis:

- PostgreSQL: `localhost:5433`
- Redis: `localhost:6380`

### 2. Run the Service

```bash
mvn spring-boot:run
```

> If you prefer the bundled Maven wrapper: `.tools/apache-maven-3.9.9/bin/mvn spring-boot:run`

### 3. Service URL> If port `8080` is already in use, start the app with:
> ```bash
> SERVER_PORT=8081 mvn spring-boot:run
> ```

### Environment Variable Overrides

| Variable       | Default                                         | Description           |
|----------------|-------------------------------------------------|-----------------------|
| `DB_URL`       | `jdbc:postgresql://localhost:5433/guardrail`    | PostgreSQL JDBC URL   |
| `DB_USERNAME`  | `postgres`                                      | Database username     |
| `DB_PASSWORD`  | `postgres`                                      | Database password     |
| `REDIS_HOST`   | `localhost`                                     | Redis host            |
| `REDIS_PORT`   | `6380`                                          | Redis port            |
| `SERVER_PORT`  | `8080`                                          | Application port      |

---

## Build

Dependencies and build configuration are declared in `pom.xml` at the project root. No additional setup is required beyond Java 17 and Maven.

```bash
mvn clean install
```

---

## API Endpoints

| Method | Endpoint                       | Description    |
|--------|--------------------------------|----------------|
| `POST` | `/api/posts`                   | Create a post  |
| `POST` | `/api/posts/{postId}/comments` | Add a comment  |
| `POST` | `/api/posts/{postId}/like`     | Like a post    |

### Postman Collection

A sample Postman collection is provided at `postman/guardrail-gateway.postman_collection.json`.

To use it: open Postman → **Import** → select the file. The base URL defaults to `http://localhost:8080`. No additional configuration is needed for local testing.

---

## Data Model

| Table        | Columns                                                                                                        |
|--------------|----------------------------------------------------------------------------------------------------------------|
| `users`      | `id`, `username`, `is_premium`                                                                                 |
| `bots`       | `id`, `name`, `persona_description`                                                                            |
| `posts`      | `id`, `author_id`, `author_type`, `content`, `created_at`                                                     |
| `comments`   | `id`, `post_id`, `parent_comment_id`, `author_id`, `author_type`, `content`, `depth_level`, `created_at`      |
| `post_likes` | Normalized likes table with unique `(post_id, user_id)`                                                        |

> Tables are created automatically on first run via `spring.jpa.hibernate.ddl-auto=update`. No manual schema migration is needed.

---

## Redis Guardrails Implementation

### 1. Virality Score (Real-Time)

`ViralityService` writes to `post:{id}:virality_score` using atomic `INCRBY` through `StringRedisTemplate`:

| Action        | Score Delta |
|---------------|-------------|
| Bot reply     | `+1`        |
| Human like    | `+20`       |
| Human comment | `+50`       |

### 2. Atomic Locks / Caps

Implemented in `GuardrailService`:

- **Horizontal cap**: `post:{id}:bot_count` is incremented atomically via Redis `INCR`. If the result exceeds 100, an immediate `DECR` is issued and the request is rejected with HTTP `429`.
- **Vertical cap**: Depth level is calculated before persistence. If `depth_level > 20`, the request is rejected with HTTP `429`.
- **Cooldown cap**: `cooldown:bot_{id}:human_{id}` is set using `SETNX` with a 10-minute TTL. If the key already exists, the request is rejected with HTTP `429`.

All `429` responses include a JSON body with a descriptive `reason` field, e.g.:
```json
{ "error": "HORIZONTAL_CAP_EXCEEDED", "message": "Bot comment limit reached for this post." }
```

### 3. Notification Engine

Implemented in `NotificationService`:

- If `user:{id}:notif_cooldown` is absent, an immediate push notification is logged and a 15-minute cooldown is set.
- If the cooldown key exists, the message is queued into `user:{id}:pending_notifs` and the user is tracked in the `pending_notif_users` set.
- A scheduled sweeper (`@Scheduled`, every 5 minutes by default) drains each user's pending list and logs:> Sweep interval is configurable via `app.scheduler.notif-sweep-ms` in `application.yml`.

---

## Thread Safety & Concurrency Guarantees

### Why Redis Atomicity Replaces Java-Level Locking

Redis processes all commands on a **single thread**, meaning operations like `INCR`, `DECR`, `SETNX`, and `INCRBY` are inherently atomic — no two concurrent requests can interleave a read-increment-write sequence. This eliminates the need for Java `synchronized` blocks, `ReentrantLock`, or database-level pessimistic locking.

### How the Bot-Count Cap is Made Race-Safe

Under a burst of, say, 200 simultaneous bot comment requests:

1. Each request calls Redis `INCR` on `post:{id}:bot_count`.
2. Redis guarantees each `INCR` returns a unique, sequential result.
3. The first 100 requests receive results 1–100 and proceed.
4. Requests 101–200 receive results > 100, triggering an immediate `DECR` rollback and returning HTTP `429`.
5. The counter never drifts above 100, with no Java-level synchronization required.

### Failure Compensation

- If a request passes the Redis guardrail but the **database write fails**, the service compensates by issuing a `DECR` on the bot counter. This prevents the counter from permanently consuming a slot for a transaction that never committed.
- The service is **fully stateless**: no in-memory maps or static counters hold guardrail state. Redis is the sole runtime coordinator for counters, cooldowns, and notification queues.

### Summary

| Concern                       | Mechanism                                     |
|-------------------------------|-----------------------------------------------|
| Concurrent bot-count updates  | Redis atomic `INCR` (single-threaded server)  |
| Overflow rollback             | Immediate `DECR` on cap breach                |
| Cooldown uniqueness           | `SETNX` (set-if-not-exists) + TTL             |
| DB/Redis drift on failure     | Compensating `DECR` after failed DB write     |
| No shared in-memory state     | Stateless service; Redis is single source     |

---

## Project Structure
guardrail-gateway/
├── src/
│   └── main/
│       ├── java/          # Spring Boot application source
│       └── resources/     # application.yml configuration
├── postman/
│   └── guardrail-gateway.postman_collection.json
├── docker-compose.yml     # Spins up PostgreSQL + Redis locally
├── pom.xml                # Maven build & dependency config
└── README.md
