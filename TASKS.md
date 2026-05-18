# TASKS.md — URL Shortener Service

> **Legend:** 🔲 Not started · 🔄 In progress · ✅ Done · ⏸ Blocked · 🔵 Phase 2+ (deferred)

---

## Milestone M0 — Scaffold (Complete)

| # | Task | Status |
|---|------|--------|
| M0-1 | Spring Initializr scaffold with correct dependencies | ✅ |
| M0-2 | `pom.xml`: Spring Web, Data JPA, Validation, PostgreSQL, Lombok | ✅ |
| M0-3 | Java 21 baseline configured in `pom.xml` | ✅ |
| M0-4 | Maven Wrapper (`mvnw`, `.mvn/`) committed | ✅ |
| M0-5 | Write `PROJECT_SPEC.md`, `ARCHITECTURE.md`, `TASKS.md`, `DB_SCHEMA.md` | ✅ |

---

## Milestone M1 — Core Feature: Shorten + Redirect + Persistence

### M1-1 · Database Schema

| # | Task | Status |
|---|------|--------|
| M1-1-1 | Create `src/main/resources/db/migration/V1__create_urls_table.sql` (Flyway) | 🔲 |
| M1-1-2 | Add `flyway-core` dependency to `pom.xml` | 🔲 |
| M1-1-3 | Configure `spring.flyway.*` in `application.properties` | 🔲 |

### M1-2 · Entity & Repository

| # | Task | Status |
|---|------|--------|
| M1-2-1 | Create `UrlEntity.java` — `@Entity`, `@Table(name="urls")` | 🔲 |
| M1-2-2 | Fields: `id` (BIGSERIAL PK), `short_code` (UNIQUE), `original_url`, `hit_count`, `created_at` | 🔲 |
| M1-2-3 | Annotate with Lombok `@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor` | 🔲 |
| M1-2-4 | Create `UrlRepository.java` — `JpaRepository<UrlEntity, Long>` | 🔲 |
| M1-2-5 | Add `findByShortCode(String)` query method | 🔲 |
| M1-2-6 | Add `findByOriginalUrl(String)` for duplicate detection | 🔲 |
| M1-2-7 | Add `existsByShortCode(String)` for alias validation | 🔲 |

### M1-3 · Base62 Encoder

| # | Task | Status |
|---|------|--------|
| M1-3-1 | Create `common/encoder/Base62Encoder.java` | 🔲 |
| M1-3-2 | Implement `encode(long id): String` — divide-and-mod over 62-char alphabet | 🔲 |
| M1-3-3 | Pad to minimum 6 characters | 🔲 |
| M1-3-4 | Write unit test `Base62EncoderTest.java` | 🔲 |

### M1-4 · DTOs

| # | Task | Status |
|---|------|--------|
| M1-4-1 | Create `ShortenRequest.java` — `originalUrl` (`@NotBlank`, `@URL`), `alias` (nullable) | 🔲 |
| M1-4-2 | Create `ShortenResponse.java` — `shortCode`, `shortUrl`, `originalUrl`, `createdAt` | 🔲 |
| M1-4-3 | Create `UrlInfoResponse.java` — `shortCode`, `originalUrl`, `hitCount`, `createdAt` | 🔲 |

### M1-5 · Service Layer

| # | Task | Status |
|---|------|--------|
| M1-5-1 | Create `UrlService.java` interface: `shorten()`, `resolve()`, `getInfo()` | 🔲 |
| M1-5-2 | Create `UrlServiceImpl.java` | 🔲 |
| M1-5-3 | Implement `shorten()`: duplicate check → save → encode id → update shortCode → return | 🔲 |
| M1-5-4 | Implement `resolve()`: findByShortCode → increment hitCount → save → return originalUrl | 🔲 |
| M1-5-5 | Implement `getInfo()`: findByShortCode → map to UrlInfoResponse | 🔲 |
| M1-5-6 | Handle alias logic in `shorten()` (skip Base62 if alias provided) | 🔲 |

### M1-6 · Controllers

| # | Task | Status |
|---|------|--------|
| M1-6-1 | Create `UrlController.java` — `@RestController`, `@RequestMapping("/api/v1/urls")` | 🔲 |
| M1-6-2 | Implement `POST /api/v1/urls` endpoint | 🔲 |
| M1-6-3 | Implement `GET /api/v1/urls/{shortCode}` info endpoint | 🔲 |
| M1-6-4 | Create `RedirectController.java` — `@Controller`, `@RequestMapping("/")` | 🔲 |
| M1-6-5 | Implement `GET /{shortCode}` → `ResponseEntity` with 302 redirect | 🔲 |

### M1-7 · Exception Handling

| # | Task | Status |
|---|------|--------|
| M1-7-1 | Create custom exceptions: `ShortCodeNotFoundException`, `AliasAlreadyExistsException` | 🔲 |
| M1-7-2 | Create `GlobalExceptionHandler.java` — `@ControllerAdvice` | 🔲 |
| M1-7-3 | Map exceptions to `ProblemDetail` (RFC 7807, native in Spring Boot 3) | 🔲 |
| M1-7-4 | Handle `MethodArgumentNotValidException` → 400 with field error details | 🔲 |

### M1-8 · Configuration

| # | Task | Status |
|---|------|--------|
| M1-8-1 | Write `application.properties` with datasource, JPA, and app-specific properties | 🔲 |
| M1-8-2 | Add `app.base-url` property (used to build `shortUrl` in response) | 🔲 |
| M1-8-3 | Set `spring.jpa.hibernate.ddl-auto=validate` | 🔲 |
| M1-8-4 | Set `spring.jpa.open-in-view=false` | 🔲 |

### M1-9 · Smoke Testing

| # | Task | Status |
|---|------|--------|
| M1-9-1 | Verify application starts (`./mvnw spring-boot:run`) | 🔲 |
| M1-9-2 | `POST /api/v1/urls` with a valid URL — confirm 201 + shortCode | 🔲 |
| M1-9-3 | `GET /{shortCode}` — confirm 302 redirect to original URL | 🔲 |
| M1-9-4 | `GET /api/v1/urls/{shortCode}` — confirm info response with hitCount | 🔲 |
| M1-9-5 | Re-POST same URL — confirm same shortCode returned (idempotent) | 🔲 |
| M1-9-6 | POST with taken alias — confirm 409 response | 🔲 |
| M1-9-7 | `GET /nonexistent` — confirm 404 ProblemDetail response | 🔲 |

---

## Milestone M2 — Robustness & Integration Tests

| # | Task | Status |
|---|------|--------|
| M2-1 | Write `UrlServiceImplTest.java` (Mockito unit tests) | 🔲 |
| M2-2 | Write `UrlControllerTest.java` (`@WebMvcTest` slice tests) | 🔲 |
| M2-3 | Write `RedirectControllerTest.java` (`@WebMvcTest`) | 🔲 |
| M2-4 | Write `Base62EncoderTest.java` (pure unit test — no Spring context) | 🔲 |
| M2-5 | Add `@SpringBootTest` integration test with Testcontainers (PostgreSQL) | 🔲 |
| M2-6 | Add `testcontainers` dependency to `pom.xml` (test scope) | 🔲 |
| M2-7 | Add structured logging configuration (`logback-spring.xml`) | 🔲 |
| M2-8 | Add `application-dev.properties` for local dev profile | 🔲 |

---

## Milestone M3 — Redis Cache-Aside 🔵

| # | Task | Status |
|---|------|--------|
| M3-1 | Add `spring-boot-starter-data-redis` to `pom.xml` | 🔵 |
| M3-2 | Configure `spring.data.redis.*` in `application.properties` | 🔵 |
| M3-3 | Create `UrlCacheService.java` — wraps Redis get/set operations | 🔵 |
| M3-4 | Integrate cache-aside into `RedirectController.redirect()` | 🔵 |
| M3-5 | Add fallback: on Redis exception, log warning and continue to DB | 🔵 |
| M3-6 | Add `spring-boot-starter-data-redis` test support / embedded Redis for tests | 🔵 |
| M3-7 | Add Redis service to `docker-compose.yml` | 🔵 |

---

## Milestone M4 — Docker Compose 🔵

| # | Task | Status |
|---|------|--------|
| M4-1 | Create `Dockerfile` using `eclipse-temurin:21-jre-alpine` | 🔵 |
| M4-2 | Create `docker-compose.yml` with `db`, `app` services | 🔵 |
| M4-3 | Add health checks for `db` service | 🔵 |
| M4-4 | Add `depends_on` with condition `service_healthy` for `app` | 🔵 |
| M4-5 | Configure `.env` file template for secrets | 🔵 |
| M4-6 | Test full stack boot: `docker compose up --build` | 🔵 |

---

## Milestone M5 — Nginx 🔵

| # | Task | Status |
|---|------|--------|
| M5-1 | Add `nginx` service to `docker-compose.yml` | 🔵 |
| M5-2 | Write `nginx/nginx.conf` — proxy pass to app:8080 | 🔵 |
| M5-3 | Configure rate limiting (`limit_req_zone`) | 🔵 |
| M5-4 | Configure gzip compression | 🔵 |
| M5-5 | Configure TLS (self-signed for local, Certbot for prod) | 🔵 |
| M5-6 | Set appropriate `Cache-Control` headers for redirect responses | 🔵 |

---

## Backlog / Deferred

| Task | Notes |
|------|-------|
| URL expiry (TTL) | Requires `expires_at` column + scheduled cleanup job |
| Custom domain support | Nginx vhost per domain, entity needs `domain` field |
| API key authentication | `Spring Security` + `api_keys` table |
| Rate limiting per API key | Redis counter or Bucket4j |
| Click analytics (geo, UA) | Separate `url_clicks` table + async write |
| Admin UI | React or Thymeleaf dashboard |
| Bulk URL import | CSV upload endpoint, async processing |
