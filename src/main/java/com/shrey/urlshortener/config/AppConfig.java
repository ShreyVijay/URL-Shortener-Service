package com.shrey.urlshortener.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Central application configuration.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Expose {@code app.base-url} as an injectable bean</li>
 *   <li>Configure global CORS policy</li>
 *   <li>Wire any future cross-cutting beans (e.g. Redis template, cache manager)</li>
 * </ul>
 */
@Configuration
public class AppConfig {

    @Value("${app.base-url}")
    private String baseUrl;

    /**
     * Returns the configured base URL used to build full short URLs in responses.
     * Example: {@code https://short.ly}
     */
    @Bean
    public String baseUrl() {
        return baseUrl;
    }

    /**
     * Global CORS configuration.
     * Tighten {@code allowedOrigins} in production via environment-specific properties.
     */
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                    .allowedOrigins("*")          // TODO: restrict in production
                    .allowedMethods("GET", "POST")
                    .allowedHeaders("Content-Type", "Accept");
            }
        };
    }
}
