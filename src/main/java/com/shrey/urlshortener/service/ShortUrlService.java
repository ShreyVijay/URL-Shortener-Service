package com.shrey.urlshortener.service;

import com.shrey.urlshortener.dto.UrlAnalyticsResponse;
import com.shrey.urlshortener.dto.UrlStatsResponse;

public interface ShortUrlService {

    /**
     * Shortens the given URL and returns its unique short code.
     * Idempotent: the same URL always returns the same short code.
     */
    default String createShortUrl(String originalUrl) {
        return createShortUrl(originalUrl, null);
    }

    /**
     * Shortens the given URL using a custom alias when provided.
     * Without an alias, the generated Base62 flow remains idempotent by URL.
     */
    String createShortUrl(String originalUrl, String customAlias);

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
