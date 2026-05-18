package com.shrey.urlshortener.controller;

import com.shrey.urlshortener.dto.ShortenRequest;
import com.shrey.urlshortener.dto.ShortenResponse;
import com.shrey.urlshortener.dto.UrlInfoResponse;
import com.shrey.urlshortener.service.UrlService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/urls")
@RequiredArgsConstructor
public class UrlController {

    private final UrlService urlService;

    /**
     * POST /api/v1/urls
     * Accepts a long URL (and optional alias), returns a ShortenResponse with the short code.
     * Responds 201 Created on success.
     */
    @PostMapping
    public ResponseEntity<ShortenResponse> shorten(@Valid @RequestBody ShortenRequest request) {
        // TODO (M1-6-2): call urlService.shorten(request), build 201 response
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * GET /api/v1/urls/{shortCode}
     * Returns metadata (originalUrl, hitCount, createdAt) for a given short code.
     */
    @GetMapping("/{shortCode}")
    public ResponseEntity<UrlInfoResponse> getInfo(@PathVariable String shortCode) {
        // TODO (M1-6-3): call urlService.getInfo(shortCode), return 200
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
