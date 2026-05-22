package com.shrey.urlshortener.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@Schema(description = "Detailed analytics for a short URL.")
public class UrlAnalyticsResponse {
    @Schema(description = "Short code being analyzed.", example = "github-docs")
    private String shortCode;

    @Schema(description = "Destination URL.", example = "https://github.com")
    private String originalUrl;

    @Schema(description = "Number of successful redirects recorded for this short code.", example = "3")
    private long clickCount;

    @Schema(description = "Timestamp when the short URL was created.", example = "2026-05-21T12:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "Most recent successful redirect timestamp.", example = "2026-05-21T12:05:00", nullable = true)
    private LocalDateTime lastAccessedAt;

    @Schema(description = "Expiration timestamp, if one was configured.", example = "2026-06-01T00:00:00", nullable = true)
    private LocalDateTime expiresAt;

    @Schema(description = "Whether the short URL is currently expired.", example = "false")
    private boolean expired;
}
