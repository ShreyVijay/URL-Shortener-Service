package com.shrey.urlshortener.controller;

import com.shrey.urlshortener.dto.ShortenRequest;
import com.shrey.urlshortener.dto.ShortenResponse;
import com.shrey.urlshortener.service.ShortUrlService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/urls")
@RequiredArgsConstructor
public class UrlController {

    private final ShortUrlService shortUrlService;

    /**
     * POST /api/v1/urls
     * Shortens a URL and returns the generated short code.
     * Responds 201 Created on success.
     */
    @PostMapping
    public ResponseEntity<ShortenResponse> shorten(@Valid @RequestBody ShortenRequest request) {
        String shortCode = shortUrlService.createShortUrl(request.originalUrl());

        String shortUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/{shortCode}")
                .buildAndExpand(shortCode)
                .toUriString();

        ShortenResponse response = new ShortenResponse(
                shortCode,
                shortUrl,
                request.originalUrl(),
                LocalDateTime.now()
        );

        URI location = URI.create(shortUrl);
        return ResponseEntity.created(location).body(response);
    }
}
