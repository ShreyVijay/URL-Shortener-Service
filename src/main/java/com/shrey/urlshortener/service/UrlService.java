package com.shrey.urlshortener.service;

import com.shrey.urlshortener.dto.ShortenRequest;
import com.shrey.urlshortener.dto.ShortenResponse;
import com.shrey.urlshortener.dto.UrlInfoResponse;

public interface UrlService {

    /**
     * Shorten a URL. Idempotent: returns existing short code if the URL
     * has already been shortened. Throws if a requested alias is already taken.
     */
    ShortenResponse shorten(ShortenRequest request);

    /**
     * Resolve a short code to its original URL and increment the hit counter.
     * Throws ShortCodeNotFoundException if the code does not exist.
     */
    String resolve(String shortCode);

    /**
     * Return metadata for a short code without incrementing the hit counter.
     * Throws ShortCodeNotFoundException if the code does not exist.
     */
    UrlInfoResponse getInfo(String shortCode);
}
