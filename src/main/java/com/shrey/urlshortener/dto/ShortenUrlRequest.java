package com.shrey.urlshortener.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShortenUrlRequest {

    @NotBlank(message = "URL must not be blank")
    @URL(message = "Must be a valid URL (e.g. https://example.com)")
    private String originalUrl;
}
