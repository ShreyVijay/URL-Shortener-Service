package com.shrey.urlshortener.controller;

import com.shrey.urlshortener.dto.ShortenUrlRequest;
import com.shrey.urlshortener.dto.ShortenUrlResponse;
import com.shrey.urlshortener.service.ShortUrlService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequiredArgsConstructor
public class ShortUrlController {

    private final ShortUrlService shortUrlService;

    // POST /api/shorten
    // Accepts { "originalUrl": "https://example.com" }
    // Returns { "shortCode": "1c", "shortUrl": "http://localhost:8080/1c" }
    @PostMapping("/api/shorten")
    public ResponseEntity<ShortenUrlResponse> shorten(@Valid @RequestBody ShortenUrlRequest request) {
        String shortCode = shortUrlService.createShortUrl(request.getOriginalUrl());

        // Build the full short URL dynamically from the current request's host and port.
        // Works on localhost:8080 and any other host without hardcoding.
        String shortUrl = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("/{code}")
                .buildAndExpand(shortCode)
                .toUriString();

        return ResponseEntity.ok(new ShortenUrlResponse(shortCode, shortUrl));
    }

    // GET /{shortCode}
    // Resolves the short code and redirects (HTTP 302) to the original URL.
    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode) {
        String originalUrl = shortUrlService.getOriginalUrl(shortCode);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.LOCATION, originalUrl);

        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }
}
