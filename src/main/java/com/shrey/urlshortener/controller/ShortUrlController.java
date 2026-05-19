package com.shrey.urlshortener.controller;

import com.shrey.urlshortener.dto.ShortenUrlRequest;
import com.shrey.urlshortener.dto.ShortenUrlResponse;
import com.shrey.urlshortener.dto.UrlStatsResponse;
import com.shrey.urlshortener.service.ShortUrlService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class ShortUrlController {

    private final ShortUrlService shortUrlService;

    // Injected from application.properties: app.base-url=http://3.26.1.16
    // Used to build the full short URL returned in the response.
    @Value("${app.base-url}")
    private String baseUrl;

    // POST /api/shorten
    @PostMapping("/api/shorten")
    public ResponseEntity<ShortenUrlResponse> shorten(@Valid @RequestBody ShortenUrlRequest request) {
        String shortCode = shortUrlService.createShortUrl(request.getOriginalUrl());
        String shortUrl = baseUrl + "/" + shortCode;
        return ResponseEntity.ok(new ShortenUrlResponse(shortCode, shortUrl));
    }

    // GET /{shortCode} — redirects to original URL
    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode) {
        String originalUrl = shortUrlService.getOriginalUrl(shortCode);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.LOCATION, originalUrl);
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    // GET /api/stats/{shortCode}
    @GetMapping("/api/stats/{shortCode}")
    public ResponseEntity<UrlStatsResponse> stats(@PathVariable String shortCode) {
        return ResponseEntity.ok(shortUrlService.getStats(shortCode));
    }
}
