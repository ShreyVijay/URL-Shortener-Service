package com.shrey.urlshortener.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Future;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request body for creating a short URL. Custom aliases and expiration timestamps are optional.")
public class ShortenUrlRequest {

    @NotBlank(message = "URL must not be blank")
    @URL(message = "Must be a valid URL (e.g. https://example.com)")
    @Schema(
            description = "Destination URL to redirect users to.",
            example = "https://github.com",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String originalUrl;

    @Pattern(
            regexp = "^[A-Za-z0-9_-]{3,30}$",
            message = "customAlias must be 3-30 characters and contain only letters, numbers, hyphen, or underscore"
    )
    @Schema(
            description = "Optional custom short code. Must be 3-30 characters using letters, numbers, hyphen, or underscore.",
            example = "github-docs",
            nullable = true)
    private String customAlias;

    @Future(message = "expiresAt must be a future timestamp")
    @Schema(
            description = "Optional ISO-8601 local date-time after which the short URL returns 410 Gone.",
            example = "2026-06-01T00:00:00",
            nullable = true)
    private LocalDateTime expiresAt;
}
