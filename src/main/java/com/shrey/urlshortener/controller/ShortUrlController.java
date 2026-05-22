package com.shrey.urlshortener.controller;

import com.shrey.urlshortener.dto.ShortenUrlRequest;
import com.shrey.urlshortener.dto.ShortenUrlResponse;
import com.shrey.urlshortener.dto.UrlAnalyticsResponse;
import com.shrey.urlshortener.dto.UrlStatsResponse;
import com.shrey.urlshortener.service.ShortUrlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@Tag(name = "URL Shortener", description = "Create short URLs, redirect visitors, and inspect click analytics.")
public class ShortUrlController {

    private final ShortUrlService shortUrlService;

    @Value("${app.base-url}")
    private String baseUrl;

    @Operation(
            summary = "Create a short URL",
            description = "Creates a generated short code or uses a validated custom alias. Optionally accepts an expiration timestamp. This endpoint is rate limited to 5 requests per IP per 60 seconds.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Short URL created successfully.",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ShortenUrlResponse.class),
                                    examples = @ExampleObject(value = """
                                            {
                                              "shortCode": "github-docs",
                                              "shortUrl": "https://url-shortener-service.duckdns.org/github-docs"
                                            }
                                            """))),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Validation error, invalid JSON, reserved alias, or non-future expiration timestamp.",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = Map.class),
                                    examples = {
                                            @ExampleObject(
                                                    name = "Field validation",
                                                    value = """
                                                            {
                                                              "originalUrl": "Must be a valid URL (e.g. https://example.com)",
                                                              "expiresAt": "expiresAt must be a future timestamp"
                                                            }
                                                            """),
                                            @ExampleObject(
                                                    name = "Reserved alias",
                                                    value = """
                                                            {
                                                              "error": "customAlias is reserved: api"
                                                            }
                                                            """)
                                    })),
                    @ApiResponse(
                            responseCode = "409",
                            description = "The requested custom alias is already taken.",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = Map.class),
                                    examples = @ExampleObject(value = """
                                            {
                                              "error": "Alias is already taken: github-docs"
                                            }
                                            """))),
                    @ApiResponse(
                            responseCode = "429",
                            description = "Rate limit exceeded for this client IP.",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = Map.class),
                                    examples = @ExampleObject(value = """
                                            {
                                              "error": "Rate limit exceeded"
                                            }
                                            """))),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Unexpected server error.",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class)))
            })
    @PostMapping("/api/shorten")
    public ResponseEntity<ShortenUrlResponse> shorten(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "URL shortening request. Provide only originalUrl for a generated code, or add customAlias and expiresAt for advanced behavior.",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ShortenUrlRequest.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "originalUrl": "https://github.com",
                                      "customAlias": "github-docs",
                                      "expiresAt": "2026-06-01T00:00:00"
                                    }
                                    """)))
            @Valid @RequestBody ShortenUrlRequest request) {
        String shortCode = shortUrlService.createShortUrl(
                request.getOriginalUrl(),
                request.getCustomAlias(),
                request.getExpiresAt());
        String shortUrl = baseUrl + "/" + shortCode;
        return ResponseEntity.ok(new ShortenUrlResponse(shortCode, shortUrl));
    }

    @Operation(
            summary = "Redirect a short code",
            description = "Redirects the visitor to the original URL and records a click. Expired links return 410 Gone instead of redirecting.",
            responses = {
                    @ApiResponse(
                            responseCode = "302",
                            description = "Redirect to the original URL.",
                            headers = @Header(
                                    name = HttpHeaders.LOCATION,
                                    description = "Original destination URL.",
                                    schema = @Schema(type = "string", example = "https://github.com")),
                            content = @Content),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Short code does not exist.",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = Map.class),
                                    examples = @ExampleObject(value = """
                                            {
                                              "error": "Short code not found: missing"
                                            }
                                            """))),
                    @ApiResponse(
                            responseCode = "410",
                            description = "Short code exists but has expired.",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = Map.class),
                                    examples = @ExampleObject(value = """
                                            {
                                              "error": "Short code has expired: github-docs"
                                            }
                                            """)))
            })
    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(
            @Parameter(description = "Short code or custom alias.", example = "github-docs")
            @PathVariable String shortCode) {
        String originalUrl = shortUrlService.getOriginalUrl(shortCode);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.LOCATION, originalUrl);
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    @Operation(
            summary = "Get basic URL stats",
            description = "Returns the destination URL and total recorded redirects for a short code.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Basic stats returned successfully.",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = UrlStatsResponse.class),
                                    examples = @ExampleObject(value = """
                                            {
                                              "shortCode": "github-docs",
                                              "originalUrl": "https://github.com",
                                              "clickCount": 3
                                            }
                                            """))),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Short code does not exist.",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class)))
            })
    @GetMapping("/api/stats/{shortCode}")
    public ResponseEntity<UrlStatsResponse> stats(
            @Parameter(description = "Short code or custom alias.", example = "github-docs")
            @PathVariable String shortCode) {
        return ResponseEntity.ok(shortUrlService.getStats(shortCode));
    }

    @Operation(
            summary = "Get URL analytics",
            description = "Returns detailed analytics including click count, creation time, last access time, configured expiration, and current expired state.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Analytics returned successfully.",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = UrlAnalyticsResponse.class),
                                    examples = @ExampleObject(value = """
                                            {
                                              "shortCode": "github-docs",
                                              "originalUrl": "https://github.com",
                                              "clickCount": 3,
                                              "createdAt": "2026-05-21T12:00:00",
                                              "lastAccessedAt": "2026-05-21T12:05:00",
                                              "expiresAt": "2026-06-01T00:00:00",
                                              "expired": false
                                            }
                                            """))),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Short code does not exist.",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = Map.class),
                                    examples = @ExampleObject(value = """
                                            {
                                              "error": "Short code not found: missing"
                                            }
                                            """)))
            })
    @GetMapping("/api/analytics/{shortCode}")
    public ResponseEntity<UrlAnalyticsResponse> analytics(
            @Parameter(description = "Short code or custom alias.", example = "github-docs")
            @PathVariable String shortCode) {
        return ResponseEntity.ok(shortUrlService.getAnalytics(shortCode));
    }
}
