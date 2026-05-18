package com.shrey.urlshortener.dto;

import java.time.OffsetDateTime;

public record UrlInfoResponse(
    String shortCode,
    String originalUrl,
    long hitCount,
    OffsetDateTime createdAt
) {}
