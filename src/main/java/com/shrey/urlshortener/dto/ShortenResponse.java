package com.shrey.urlshortener.dto;

import java.time.LocalDateTime;

public record ShortenResponse(
    String shortCode,
    String shortUrl,
    String originalUrl,
    LocalDateTime createdAt
) {}
