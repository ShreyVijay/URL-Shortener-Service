# DB_SCHEMA.md — URL Shortener Service

## 1. Database Technology

| Property | Value |
|----------|-------|
| Engine | PostgreSQL 16 |
| Schema management | Flyway (versioned migrations) |
| JPA DDL auto | `validate` (Flyway owns the schema, Hibernate only validates) |
| Connection pooling | HikariCP (bundled with Spring Boot) |

---

## 2. Schema Overview

```
┌─────────────────────────────┐
│           urls              │
├─────────────────────────────┤
│ id            BIGSERIAL  PK │
│ short_code    VARCHAR(20)   │◄── UNIQUE INDEX
│ original_url  TEXT          │
│ hit_count     BIGINT        │
│ created_at    TIMESTAMPTZ   │
└─────────────────────────────┘
```

---

## 3. Table: `urls`

### 3.1 DDL

```sql
-- V1__create_urls_table.sql
CREATE TABLE IF NOT EXISTS urls (
    id           BIGSERIAL     NOT NULL,
    short_code   VARCHAR(20)   NOT NULL,
    original_url TEXT          NOT NULL,
    hit_count    BIGINT        NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_urls PRIMARY KEY (id),
    CONSTRAINT uq_urls_short_code UNIQUE (short_code)
);

CREATE INDEX IF NOT EXISTS idx_urls_short_code
    ON urls (short_code);

CREATE INDEX IF NOT EXISTS idx_urls_original_url
    ON urls (original_url);

COMMENT ON TABLE  urls IS 'Stores all shortened URL mappings';
COMMENT ON COLUMN urls.id           IS 'Auto-incrementing surrogate primary key; used as input to Base62 encoder';
COMMENT ON COLUMN urls.short_code   IS 'Base62-encoded short identifier or custom user alias';
COMMENT ON COLUMN urls.original_url IS 'Full original URL that the short code redirects to';
COMMENT ON COLUMN urls.hit_count    IS 'Number of times this short URL has been resolved (redirected)';
COMMENT ON COLUMN urls.created_at   IS 'UTC timestamp of record creation';
```

### 3.2 Column Reference

| Column | Type | Constraints | Description |
|--------|------|------------|-------------|
| `id` | `BIGSERIAL` | `PRIMARY KEY`, `NOT NULL` | Auto-incrementing 64-bit integer. Input to Base62 encoder. Supports up to ~9.2 × 10¹⁸ records. |
| `short_code` | `VARCHAR(20)` | `NOT NULL`, `UNIQUE` | 6-char Base62 code (e.g. `aB3xYz`) or custom alias (up to 20 chars). |
| `original_url` | `TEXT` | `NOT NULL` | Full original URL. `TEXT` avoids length limit issues with long URLs (PostgreSQL stores efficiently). |
| `hit_count` | `BIGINT` | `NOT NULL`, `DEFAULT 0` | Incremented on every redirect. `BIGINT` avoids integer overflow for popular links. |
| `created_at` | `TIMESTAMPTZ` | `NOT NULL`, `DEFAULT NOW()` | Timezone-aware UTC timestamp. Never updated after insert. |

### 3.3 Indexes

| Index Name | Columns | Type | Purpose |
|------------|---------|------|---------|
| `pk_urls` | `id` | B-tree (PK) | Primary key lookups |
| `uq_urls_short_code` | `short_code` | B-tree (UNIQUE) | Fast redirect lookup by short code; enforces uniqueness |
| `idx_urls_original_url` | `original_url` | B-tree | Duplicate detection during shorten (findByOriginalUrl) |

---

## 4. JPA Entity Mapping

```java
@Entity
@Table(name = "urls", uniqueConstraints = {
    @UniqueConstraint(name = "uq_urls_short_code", columnNames = "short_code")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UrlEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "short_code", nullable = false, unique = true, length = 20)
    private String shortCode;

    @Column(name = "original_url", nullable = false, columnDefinition = "TEXT")
    private String originalUrl;

    @Column(name = "hit_count", nullable = false)
    @Builder.Default
    private long hitCount = 0L;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = OffsetDateTime.now(ZoneOffset.UTC);
        }
    }

    public void incrementHitCount() {
        this.hitCount++;
    }
}
```

---

## 5. Flyway Migration Strategy

```
src/main/resources/
└── db/
    └── migration/
        ├── V1__create_urls_table.sql       ← Initial schema (M1)
        ├── V2__add_expires_at_column.sql   ← Future: TTL support
        └── V3__add_api_keys_table.sql      ← Future: auth
```

**Naming convention:** `V{version}__{description}.sql`  
**Flyway configuration:**
```properties
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
spring.flyway.baseline-on-migrate=false
spring.flyway.out-of-order=false
```

Flyway runs automatically on application startup, applying any unapplied migrations in version order. The `flyway_schema_history` table is created automatically to track applied versions.

---

## 6. HikariCP Connection Pool Configuration

```properties
# Recommended starting values (tune per load profile)
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=2
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000
spring.datasource.hikari.pool-name=UrlShortenerPool
```

---

## 7. Query Patterns

| Operation | Query | Index Used |
|-----------|-------|------------|
| Redirect lookup | `SELECT * FROM urls WHERE short_code = ?` | `uq_urls_short_code` |
| Duplicate detection | `SELECT * FROM urls WHERE original_url = ?` | `idx_urls_original_url` |
| Alias conflict check | `SELECT EXISTS(SELECT 1 FROM urls WHERE short_code = ?)` | `uq_urls_short_code` |
| Info endpoint | `SELECT * FROM urls WHERE short_code = ?` | `uq_urls_short_code` |
| Hit count update | `UPDATE urls SET hit_count = hit_count + 1 WHERE id = ?` | `pk_urls` |

---

## 8. Phase 2 — Redis Cache Schema

> Not a relational schema; documented here for completeness.

| Key Pattern | Type | Value | TTL |
|-------------|------|-------|-----|
| `url:{shortCode}` | `STRING` | `originalUrl` (plain text) | 86400s (24h) |

**Cache-aside semantics:**
1. On GET `/{shortCode}`: check Redis first
2. Miss → query PostgreSQL → write to Redis with TTL → respond
3. No explicit invalidation needed (URLs are immutable after creation)

---

## 9. Future Schema Extensions (Planned, Not Implemented)

### 9.1 URL Expiry (TTL)

```sql
ALTER TABLE urls ADD COLUMN expires_at TIMESTAMPTZ;
CREATE INDEX idx_urls_expires_at ON urls (expires_at) WHERE expires_at IS NOT NULL;
```

### 9.2 Click Analytics

```sql
CREATE TABLE url_clicks (
    id          BIGSERIAL    PRIMARY KEY,
    url_id      BIGINT       NOT NULL REFERENCES urls(id) ON DELETE CASCADE,
    clicked_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    user_agent  TEXT,
    ip_address  INET,
    referer     TEXT
);
CREATE INDEX idx_url_clicks_url_id ON url_clicks (url_id);
```

### 9.3 API Keys (Authentication)

```sql
CREATE TABLE api_keys (
    id          BIGSERIAL    PRIMARY KEY,
    key_hash    VARCHAR(64)  NOT NULL UNIQUE,  -- SHA-256 of raw key
    label       VARCHAR(100),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    revoked_at  TIMESTAMPTZ
);
```
