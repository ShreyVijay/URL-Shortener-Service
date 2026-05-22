package com.shrey.urlshortener.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

// Returned by POST /api/shorten
// Contains the short code and the full clickable short URL.
@Data
@AllArgsConstructor
@Schema(description = "Response returned after a short URL is created.")
public class ShortenUrlResponse {

    @Schema(description = "Generated or custom short code.", example = "github-docs")
    private String shortCode;

    @Schema(description = "Fully qualified HTTPS short URL.", example = "https://url-shortener-service.duckdns.org/github-docs")
    private String shortUrl;
}
