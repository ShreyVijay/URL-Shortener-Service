package com.shrey.urlshortener.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Future;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShortenUrlRequest {

    @NotBlank(message = "URL must not be blank")
    @URL(message = "Must be a valid URL (e.g. https://example.com)")
    private String originalUrl;

    @Pattern(
            regexp = "^[A-Za-z0-9_-]{3,30}$",
            message = "customAlias must be 3-30 characters and contain only letters, numbers, hyphen, or underscore"
    )
    private String customAlias;

    @Future(message = "expiresAt must be a future timestamp")
    private LocalDateTime expiresAt;
}
