package com.shrey.urlshortener.controller;

import com.shrey.urlshortener.dto.ShortenUrlRequest;
import com.shrey.urlshortener.service.ShortUrlService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class ShortUrlController {

    private final ShortUrlService shortUrlService;

    // POST /api/shorten
    // Body: { "originalUrl": "https://example.com" }
    // Returns: the generated short code as a plain string
    @PostMapping("/api/shorten")
    public ResponseEntity<String> shorten(@Valid @RequestBody ShortenUrlRequest request) {
        String shortCode = shortUrlService.createShortUrl(request.getOriginalUrl());
        return ResponseEntity.ok(shortCode);
    }

    // GET /{shortCode}
    // Resolves the short code and redirects to the original URL (HTTP 302)
    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode) {
        String originalUrl = shortUrlService.getOriginalUrl(shortCode);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.LOCATION, originalUrl);

        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }
}
