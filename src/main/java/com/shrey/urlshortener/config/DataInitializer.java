package com.shrey.urlshortener.config;

import com.shrey.urlshortener.service.ShortUrlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Temporary startup smoke test.
 *
 * Calls {@link ShortUrlService#createShortUrl(String)} with a known URL
 * and prints the generated short code to confirm the full
 * service → repository → database → Base62 encoding pipeline is working.
 *
 * REMOVE before merging to main / deploying to production.
 * Active only on the "dev" profile to prevent accidental runs in prod.
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final ShortUrlService shortUrlService;

    @Override
    public void run(String... args) {
        String testUrl = "https://google.com";

        String shortCode = shortUrlService.createShortUrl(testUrl);

        System.out.println("--------------------------------------------------");
        System.out.println("  DataInitializer smoke test");
        System.out.println("  Input URL  : " + testUrl);
        System.out.println("  Short code : " + shortCode);
        System.out.println("--------------------------------------------------");

        log.info("DataInitializer completed: shortCode={}", shortCode);
    }
}
