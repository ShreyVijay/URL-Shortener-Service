package com.shrey.urlshortener.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String PRODUCTION_BASE_URL = "https://url-shortener-service.duckdns.org";

    @Bean
    public OpenAPI urlShortenerOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("URL Shortener API")
                        .version("1.0.0")
                        .description("Interactive documentation for shortening URLs, redirecting short codes, and reading analytics.")
                        .contact(new Contact().name("URL Shortener Service")))
                .servers(List.of(new Server()
                        .url(PRODUCTION_BASE_URL)
                        .description("Production HTTPS")));
    }
}
