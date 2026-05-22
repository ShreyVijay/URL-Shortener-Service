package com.shrey.urlshortener.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "Basic click statistics for a short URL.")
public class UrlStatsResponse {
    @Schema(description = "Short code being reported.", example = "github-docs")
    private String shortCode;

    @Schema(description = "Destination URL.", example = "https://github.com")
    private String originalUrl;

    @Schema(description = "Number of successful redirects recorded for this short code.", example = "3")
    private long clickCount;
}
