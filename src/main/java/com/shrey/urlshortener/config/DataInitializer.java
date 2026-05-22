package com.shrey.urlshortener.config;

import com.shrey.urlshortener.service.ShortUrlService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

// Runs automatically on startup when profile = dev.
// Used to quickly verify the service + DB connection are working.
// Delete this class once you have a working controller to test via Postman.
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final ShortUrlService shortUrlService;

    @Override
    public void run(String... args) {
        String shortCode = shortUrlService.createShortUrl("https://google.com");
        System.out.println("Test short code created: " + shortCode);
    }
}
