package com.shrey.urlshortener.dto;

import java.time.OffsetDateTime;

public record ShortenResponse(
    String shortCode,
    String shortUrl,
    String originalUrl,
    OffsetDateTime createdAt
) {}
