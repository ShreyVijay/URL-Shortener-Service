# ARCHITECTURE.md — URL Shortener Service

## 1. Architectural Style

**Modular Monolith** — a single deployable Spring Boot JAR whose internal code is partitioned into cohesive feature modules with explicit, enforced boundaries. This is the correct starting point: monolith simplicity now, straightforward extraction later if needed.

No microservices. No inter-service networking overhead. No distributed tracing complexity — yet.

---

## 2. High-Level System Diagram

```
┌──────────────────────────────────────────────────────────────┐
│                        CLIENT                                │
│          (Browser / Mobile App / cURL)                       │
└───────────────────────┬──────────────────────────────────────┘
                        │ HTTP
                        ▼
               [Phase 3: Nginx]
               TLS termination
               Rate limiting
               Gzip compression
                        │
                        ▼
┌──────────────────────────────────────────────────────────────┐
│              Spring Boot Application (JVM)                   │
│                                                              │
│  ┌────────────┐   ┌─────────────┐   ┌────────────────────┐  │
│  │  Redirect  │   │  URL Mgmt   │   │   Common / Shared  │  │
│  │  Module    │   │  Module     │   │   Module           │  │
│  │            │   │             │   │                    │  │
│  │ GET /{code}│   │POST /api/.. │   │ Base62Encoder      │  │
│  │ → 302      │   │GET  /api/.. │   │ GlobalExcHandler   │  │
│  └─────┬──────┘   └──────┬──────┘   └────────────────────┘  │
│        │                 │                                   │
│        └────────┬────────┘                                   │
│                 ▼                                            │
│          [Service Layer]                                     │
│                 │                                            │
│        [Phase 2: Redis]      [Phase 2 only]                  │
│        Cache-Aside lookup                                    │
│                 │                                            │
│                 ▼                                            │
│          [Repository Layer]                                  │
│          Spring Data JPA                                     │
└─────────────────┬────────────────────────────────────────────┘
                  │ JDBC / pgJDBC
                  ▼
        ┌─────────────────────┐
        │    PostgreSQL 16     │
        │    urls table        │
        └─────────────────────┘
```

---

## 3. Package / Module Structure

```
com.shrey.urlshortener
│
├── UrlshortenerApplication.java          ← Spring Boot entry point
│
├── url/                                  ← URL management feature module
│   ├── controller/
│   │   └── UrlController.java            ← POST /api/v1/urls, GET /api/v1/urls/{code}
│   ├── service/
│   │   ├── UrlService.java               ← interface
│   │   └── UrlServiceImpl.java           ← business logic, duplicate detection
│   ├── repository/
│   │   └── UrlRepository.java            ← extends JpaRepository<UrlEntity, Long>
│   ├── entity/
│   │   └── UrlEntity.java                ← JPA entity (@Entity)
│   ├── dto/
│   │   ├── ShortenRequest.java           ← @Valid input DTO
│   │   └── ShortenResponse.java          ← output DTO
│   └── mapper/
│       └── UrlMapper.java                ← entity ↔ DTO conversion
│
├── redirect/                             ← Redirect feature module
│   └── controller/
│       └── RedirectController.java       ← GET /{shortCode} → HTTP 302
│
└── common/                               ← Shared utilities (no business logic)
    ├── encoder/
    │   └── Base62Encoder.java            ← pure static/singleton utility
    ├── exception/
    │   ├── ShortCodeNotFoundException.java
    │   ├── AliasAlreadyExistsException.java
    │   └── InvalidUrlException.java
    └── handler/
        └── GlobalExceptionHandler.java   ← @ControllerAdvice, RFC 7807 Problem JSON
```

### Module Dependency Rules

```
redirect  ──────────────────────────────────────►  url (service only)
url       ──────────────────────────────────────►  common
redirect  ──────────────────────────────────────►  common
```

- **`common`** has **zero** dependencies on `url` or `redirect`
- **`redirect`** calls `UrlService` (interface), never touches `UrlRepository` directly
- **`url`** controllers never call other controllers

---

## 4. Request Lifecycle

### 4.1 Shorten Request

```
POST /api/v1/urls
        │
        ▼
UrlController.shorten(@Valid ShortenRequest)
        │  Bean Validation fires (@NotBlank, @URL on originalUrl)
        │  400 if invalid
        ▼
UrlServiceImpl.shorten(ShortenRequest)
        │
        ├─ [Check duplicate] UrlRepository.findByOriginalUrl(url)
        │       └─ if found → return existing shortCode (idempotent)
        │
        ├─ [Check alias] if alias provided → UrlRepository.existsByShortCode(alias)
        │       └─ if taken → throw AliasAlreadyExistsException → 409
        │
        ├─ UrlEntity entity = new UrlEntity(originalUrl, null, 0, now())
        │
        ├─ UrlEntity saved = urlRepository.save(entity)  ← gets auto-generated id
        │
        ├─ shortCode = alias ?? Base62Encoder.encode(saved.getId())
        │
        ├─ saved.setShortCode(shortCode)
        │
        └─ urlRepository.save(saved) → return ShortenResponse
```

### 4.2 Redirect Request

```
GET /{shortCode}
        │
        ▼
RedirectController.redirect(shortCode)
        │
        ▼
[Phase 2] Check Redis cache → cache hit? return 302 immediately
        │
        ▼  cache miss
UrlServiceImpl.resolve(shortCode)
        │
        ├─ urlRepository.findByShortCode(shortCode)
        │       └─ if empty → throw ShortCodeNotFoundException → 404
        │
        ├─ entity.incrementHitCount()
        │
        ├─ urlRepository.save(entity)
        │
        ├─ [Phase 2] write originalUrl to Redis cache (TTL 24h)
        │
        └─ return ResponseEntity.status(302).location(URI(originalUrl)).build()
```

---

## 5. Base62 Encoding Design

```
Alphabet: "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
                                                          [0..61]

encode(id):
    result = ""
    while id > 0:
        result = alphabet[id % 62] + result
        id = id / 62
    pad left with '0' to minimum length of 6
    return result

decode(code):                          // for future use
    result = 0
    for each char c in code:
        result = result * 62 + indexOf(alphabet, c)
    return result
```

**Collision guarantee:** The `id` column is `BIGSERIAL` (auto-incrementing 64-bit integer). Each ID is unique by definition, so each Base62-encoded short code is unique without any retry logic.

**Custom aliases** bypass encoding entirely and are stored directly as the `short_code`.

---

## 6. Data Flow Diagram (Phase 2 with Redis)

```
Client GET /{code}
        │
        ▼
RedirectController
        │
        ├──► Redis GET "url:{code}"
        │         │
        │    HIT  └──► 302 Location: <cached url>   (microseconds)
        │
        │    MISS
        │         │
        ▼         ▼
UrlRepository.findByShortCode(code)
        │
   NOT FOUND ──► 404
        │
   FOUND
        │
        ├──► Redis SET "url:{code}" <originalUrl> EX 86400
        │
        └──► 302 Location: <originalUrl>
```

---

## 7. Configuration Strategy

All environment-specific configuration lives in `application.properties` (overrideable via environment variables per Spring Boot externalized config).

```
# src/main/resources/application.properties  (development defaults)
spring.datasource.url=jdbc:postgresql://localhost:5432/urlshortener
spring.datasource.username=${DB_USERNAME:postgres}
spring.datasource.password=${DB_PASSWORD:secret}

spring.jpa.hibernate.ddl-auto=validate          # never 'create-drop' in production
spring.jpa.show-sql=false

app.base-url=http://localhost:8080
app.short-code.min-length=6

# Phase 2
spring.data.redis.host=${REDIS_HOST:localhost}
spring.data.redis.port=6379
```

Profile strategy:
- `application.properties` — shared defaults
- `application-dev.properties` — local dev overrides
- `application-prod.properties` — production (secrets injected via env vars)

---

## 8. Error Handling

**GlobalExceptionHandler** (`@ControllerAdvice`) maps exceptions to RFC 7807 `ProblemDetail` responses:

| Exception | HTTP Status | `type` |
|-----------|-------------|--------|
| `ShortCodeNotFoundException` | 404 | `urn:problem:short-code-not-found` |
| `AliasAlreadyExistsException` | 409 | `urn:problem:alias-conflict` |
| `InvalidUrlException` | 400 | `urn:problem:invalid-url` |
| `MethodArgumentNotValidException` | 400 | `urn:problem:validation-error` |
| `Exception` (fallback) | 500 | `urn:problem:internal-error` |

Spring Boot 3 natively supports `ProblemDetail` (RFC 7807) — no extra library needed.

---

## 9. Phase 2 — Redis Cache-Aside Plan

When Redis is introduced:
1. Add `spring-boot-starter-data-redis` to `pom.xml`
2. `RedirectController` → `UrlService.resolveWithCache(shortCode)`
3. Cache key pattern: `"url:{shortCode}"`
4. TTL: 24 hours (`86400` seconds)
5. Cache invalidation: not required initially (URLs are immutable after creation)
6. On Redis failure: fall through to database (resilient, non-blocking)

---

## 10. Phase 3 — Docker & Nginx Plan

```
docker-compose.yml
├── db        (postgres:16-alpine)   port 5432
├── app       (eclipse-temurin:21)   port 8080
└── nginx     (nginx:alpine)         port 80/443   ← entry point

nginx.conf responsibilities:
  - TLS termination (Let's Encrypt via Certbot)
  - Proxy pass to app:8080
  - Rate limiting (limit_req_zone)
  - Gzip compression
  - Cache headers for redirect responses
```

---

## 11. Scalability Path

The stateless application design enables horizontal scaling with zero code changes:

```
Load Balancer (Nginx / AWS ALB)
       │
       ├── App Instance 1 ─┐
       ├── App Instance 2 ─┼──► Shared PostgreSQL
       └── App Instance N ─┘         │
                                      └──► Shared Redis (Phase 2)
```

The only shared mutable state lives in PostgreSQL and (later) Redis — both are external, cluster-capable services.
