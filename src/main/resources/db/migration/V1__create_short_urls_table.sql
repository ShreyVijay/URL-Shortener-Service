-- V1__create_short_urls_table.sql
-- Creates the core URL mapping table.
-- short_code is nullable on INSERT to support the two-save pattern:
--   1. INSERT with short_code = NULL to obtain the auto-generated id
--   2. UPDATE short_code = Base62(id) within the same transaction
-- Every committed row is guaranteed to have a non-null short_code.

CREATE TABLE IF NOT EXISTS short_urls (
    id           BIGSERIAL     NOT NULL,
    short_code   VARCHAR(20),
    original_url TEXT          NOT NULL,
    click_count  BIGINT        NOT NULL DEFAULT 0,
    created_at   TIMESTAMP     NOT NULL DEFAULT NOW(),
    expires_at   TIMESTAMP     NULL,

    CONSTRAINT pk_short_urls PRIMARY KEY (id),
    CONSTRAINT uq_short_urls_short_code UNIQUE (short_code)
);

CREATE INDEX IF NOT EXISTS idx_short_urls_short_code
    ON short_urls (short_code);

CREATE INDEX IF NOT EXISTS idx_short_urls_original_url
    ON short_urls (original_url);

-- Partial index: only index rows that actually have an expiry date set
CREATE INDEX IF NOT EXISTS idx_short_urls_expires_at
    ON short_urls (expires_at)
    WHERE expires_at IS NOT NULL;

COMMENT ON TABLE  short_urls IS 'Shortened URL mappings';
COMMENT ON COLUMN short_urls.id           IS 'Auto-increment PK; used as Base62 encoding seed';
COMMENT ON COLUMN short_urls.short_code   IS 'Base62 short code or custom alias; set after first INSERT';
COMMENT ON COLUMN short_urls.original_url IS 'Full destination URL';
COMMENT ON COLUMN short_urls.click_count  IS 'Total redirect count';
COMMENT ON COLUMN short_urls.created_at   IS 'UTC creation timestamp; never updated';
COMMENT ON COLUMN short_urls.expires_at   IS 'Optional UTC expiry; NULL = never expires';
