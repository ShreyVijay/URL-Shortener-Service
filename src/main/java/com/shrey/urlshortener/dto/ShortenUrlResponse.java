package com.shrey.urlshortener.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

// Returned by POST /api/shorten
// Contains the short code and the full clickable short URL.
@Data
@AllArgsConstructor
public class ShortenUrlResponse {

    private String shortCode;
    private String shortUrl;
}
