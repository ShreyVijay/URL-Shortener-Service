package com.shrey.urlshortener.controller;

import com.shrey.urlshortener.service.UrlService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.net.URI;

@Controller
@RequiredArgsConstructor
public class RedirectController {

    private final UrlService urlService;

    /**
     * GET /{shortCode}
     * Resolves a short code to its original URL and issues an HTTP 302 redirect.
     * Increments the hit counter on every successful redirect.
     */
    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode) {
        // TODO (M1-6-5): call urlService.resolve(shortCode), build 302 Location response
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
