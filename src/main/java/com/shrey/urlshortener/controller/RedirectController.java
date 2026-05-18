package com.shrey.urlshortener.controller;

import com.shrey.urlshortener.service.ShortUrlService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.net.URI;

@Controller
@RequiredArgsConstructor
public class RedirectController {

    private final ShortUrlService shortUrlService;

    /**
     * GET /{shortCode}
     * Resolves a short code to its original URL and issues an HTTP 302 redirect.
     * Increments the click counter on every successful redirect.
     */
    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode) {
        String originalUrl = shortUrlService.getOriginalUrl(shortCode);
        return ResponseEntity.status(302)
                .location(URI.create(originalUrl))
                .build();
    }
}
