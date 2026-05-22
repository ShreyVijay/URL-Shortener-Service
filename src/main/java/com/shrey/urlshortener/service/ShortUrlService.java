package com.shrey.urlshortener.service;

import com.shrey.urlshortener.dto.UrlAnalyticsResponse;
import com.shrey.urlshortener.dto.UrlStatsResponse;

import java.time.LocalDateTime;

public interface ShortUrlService {

    /**
     * Shortens the given URL and returns its unique short code.
     * Idempotent: the same URL always returns the same short code.
     */
    default String createShortUrl(String originalUrl) {
        return createShortUrl(originalUrl, null, null);
    }

    /**
     * Shortens the given URL using a custom alias when provided.
     * Without an alias, the generated Base62 flow remains idempotent by URL.
     */
    default String createShortUrl(String originalUrl, String customAlias) {
        return createShortUrl(originalUrl, customAlias, null);
    }

    /**
     * Shortens the given URL using a custom alias and optional expiration timestamp.
     */
    String createShortUrl(String originalUrl, String customAlias, LocalDateTime expiresAt);

    /**
     * Resolves a short code to its original URL and records the click.
     * Throws ShortCodeNotFoundException if the code is unknown or expired.
     */
    String getOriginalUrl(String shortCode);

    /**
     * Returns stats (shortCode, originalUrl, clickCount) for a given short code.
     * Throws ShortCodeNotFoundException if the code does not exist.
     */
    UrlStatsResponse getStats(String shortCode);

    UrlAnalyticsResponse getAnalytics(String shortCode);
}
