# PROJECT_SPEC.md — URL Shortener Service

## 1. Overview

A **scalable, production-oriented URL redirect infrastructure** built as a modular Spring Boot monolith.  
The service accepts long URLs, assigns them a unique short code via Base62 encoding, and redirects clients to the original URL on subsequent lookups.

**Runtime:** Java 21 (LTS)  
**Framework:** Spring Boot 3.5.x  
**Build tool:** Maven (Maven Wrapper included)  
**Group / Artifact:** `com.shrey` / `urlshortener`

---

## 2. Goals

| # | Goal | Priority |
|---|------|----------|
| G1 | Shorten any HTTP/HTTPS URL and return a unique short code | P0 |
| G2 | Resolve a short code to its original URL and issue an HTTP 302 redirect | P0 |
| G3 | Persist all URL mappings durably in PostgreSQL | P0 |
| G4 | Expose a clean, versioned REST API | P0 |
| G5 | Validate incoming URLs (format, length) | P1 |
| G6 | Return deterministic short codes for duplicate URLs (idempotent shorten) | P1 |
| G7 | Track hit counts per short URL | P1 |
| G8 | Support optional custom aliases for short codes | P2 |
| G9 | Cache hot redirect lookups in Redis (cache-aside) | P2 |
| G10 | Containerise the entire stack with Docker Compose | P3 |
| G11 | Front the service with Nginx for TLS termination and rate limiting | P3 |

---

## 3. Non-Goals (for this iteration)

- User authentication / multi-tenancy
- Analytics dashboard / reporting UI
- Link expiry (TTL) — deferred to a later milestone
- QR code generation
- Bulk import of URLs

---

## 4. API Surface

### 4.1 Shorten a URL

```
POST /api/v1/urls
Content-Type: application/json

{
  "originalUrl": "https://example.com/very/long/path?query=true",
  "alias": "my-alias"          // optional
}
```

**Response 201 Created**
```json
{
  "shortCode": "aB3xYz",
  "shortUrl": "https://short.ly/aB3xYz",
  "originalUrl": "https://example.com/very/long/path?query=true",
  "createdAt": "2026-05-19T01:00:00Z"
}
```

**Error responses**
- `400 Bad Request` — invalid URL format or alias collision
- `409 Conflict` — alias already taken

---

### 4.2 Redirect

```
GET /{shortCode}
```

**Response 302 Found**  
`Location: https://example.com/very/long/path?query=true`

- `404 Not Found` — unknown short code

---

### 4.3 Get URL Info

```
GET /api/v1/urls/{shortCode}
```

**Response 200 OK**
```json
{
  "shortCode": "aB3xYz",
  "originalUrl": "https://example.com/...",
  "hitCount": 142,
  "createdAt": "2026-05-19T01:00:00Z"
}
```

---

## 5. Encoding Strategy — Base62

| Property | Value |
|----------|-------|
| Alphabet | `0-9`, `a-z`, `A-Z` (62 characters) |
| Code length | 6 characters minimum |
| Collision resistance | 62⁶ ≈ 56.8 billion unique codes |
| Input | Auto-incremented numeric `id` from the `urls` table |
| Algorithm | Divide-and-mod over the alphabet string |

**Why Base62?**  
URL-safe (no `+`, `/`, `=`), compact, human-readable, no external dependency.

**Duplicate detection:** Before encoding, query for an existing record with the same `originalUrl`. If found, return its existing `shortCode` (idempotent).

---

## 6. Technology Stack

### Phase 1 — Core (current)

| Layer | Technology |
|-------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 3.5 |
| Web | Spring MVC (embedded Tomcat) |
| Persistence | Spring Data JPA + Hibernate |
| Database | PostgreSQL 16 |
| Validation | Jakarta Bean Validation (Hibernate Validator) |
| Boilerplate | Lombok |
| Build | Maven 3.9 + Maven Wrapper |

### Phase 2 — Caching

| Layer | Technology |
|-------|-----------|
| Cache | Redis 7 (cache-aside pattern) |
| Client | Spring Data Redis / Lettuce |

### Phase 3 — Infrastructure

| Component | Technology |
|-----------|-----------|
| Containerisation | Docker + Docker Compose |
| Reverse proxy | Nginx (TLS, rate-limiting, gzip) |

---

## 7. Quality & Constraints

- **Stateless application tier** — no session state; horizontally scalable behind a load balancer
- **Modular monolith** — feature packages (`url`, `redirect`, `common`) with clear internal contracts; no premature microservice split
- **Clean architecture** — controller → service → repository layers; no business logic in controllers or entities
- **Validation at the boundary** — all input validated via `@Valid` before entering the service layer
- **No premature abstraction** — interfaces introduced only where multiple implementations exist or are planned (e.g., `UrlRepository` already abstracted by Spring Data; `ShortCodeGenerator` as a service interface to allow swapping strategies)
- **Error handling** — global `@ControllerAdvice` returning RFC 7807 Problem JSON
- **Logging** — SLF4J + Logback structured JSON in production profile
- **Configuration** — all environment-specific values in `application.properties` / environment variables (12-factor)

---

## 8. Project Milestones

| Milestone | Scope | Status |
|-----------|-------|--------|
| M0 | Scaffold & pom.xml baseline | ✅ Done |
| M1 | URL shorten + redirect + PostgreSQL | 🔲 Planned |
| M2 | Validation, error handling, info endpoint | 🔲 Planned |
| M3 | Redis cache-aside integration | 🔲 Planned |
| M4 | Docker Compose stack | 🔲 Planned |
| M5 | Nginx reverse proxy config | 🔲 Planned |
