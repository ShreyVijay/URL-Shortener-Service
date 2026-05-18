package com.shrey.urlshortener.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public record ShortenRequest(

    @NotBlank(message = "Original URL must not be blank")
    @URL(message = "Must be a valid HTTP or HTTPS URL")
    String originalUrl,

    @Size(max = 20, message = "Alias must be 20 characters or fewer")
    String alias   // optional — null means auto-generate via Base62
) {}
