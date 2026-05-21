package com.shrey.urlshortener.service;

import com.shrey.urlshortener.dto.UrlAnalyticsResponse;
import com.shrey.urlshortener.dto.UrlStatsResponse;
import com.shrey.urlshortener.entity.ShortUrl;
import com.shrey.urlshortener.exception.AliasAlreadyExistsException;
import com.shrey.urlshortener.exception.ShortCodeNotFoundException;
import com.shrey.urlshortener.repository.ShortUrlRepository;
import com.shrey.urlshortener.util.Base62Encoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShortUrlServiceImpl implements ShortUrlService {

    private static final Pattern CUSTOM_ALIAS_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{3,30}$");
    private static final Set<String> RESERVED_ALIASES = Set.of("api", "actuator", "analytics");

    private final ShortUrlRepository shortUrlRepository;
    private final StringRedisTemplate redisTemplate;

    @Override
    @Transactional
    public String createShortUrl(String originalUrl, String customAlias) {
        if (originalUrl == null || originalUrl.isBlank()) {
            throw new IllegalArgumentException("originalUrl must not be blank");
        }

        if (customAlias != null) {
            return createCustomAlias(originalUrl, customAlias);
        }

        // Idempotency: if the URL was already shortened, return the existing code.
        return shortUrlRepository.findByOriginalUrl(originalUrl)
                .map(ShortUrl::getShortCode)
                .orElseGet(() -> createAndPersist(originalUrl));
    }

    private String createCustomAlias(String originalUrl, String customAlias) {
        validateCustomAlias(customAlias);

        String aliasKey = customAlias.toLowerCase(Locale.ROOT);
        if (shortUrlRepository.existsByShortCodeIgnoreCase(customAlias)) {
            throw new AliasAlreadyExistsException(customAlias);
        }

        ShortUrl entity = ShortUrl.builder()
                .shortCode(customAlias)
                .customAliasKey(aliasKey)
                .originalUrl(originalUrl)
                .build();

        try {
            shortUrlRepository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException ex) {
            throw new AliasAlreadyExistsException(customAlias);
        }

        log.info("Custom short URL created: shortCode={} originalUrl={}", customAlias, originalUrl);
        return customAlias;
    }

    private void validateCustomAlias(String customAlias) {
        if (!CUSTOM_ALIAS_PATTERN.matcher(customAlias).matches()) {
            throw new IllegalArgumentException(
                    "customAlias must be 3-30 characters and contain only letters, numbers, hyphen, or underscore");
        }

        if (RESERVED_ALIASES.contains(customAlias.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("customAlias is reserved: " + customAlias);
        }
    }

    /**
     * Two-save pattern:
     * 1. Persist the entity without a shortCode to let the database generate the id.
     * 2. Encode that id with Base62 and write the shortCode back in the same transaction.
     *
     * saveAndFlush() on step 1 forces Hibernate to execute the INSERT immediately,
     * so the database-assigned id is available before step 2 runs.
     */
    private String createAndPersist(String originalUrl) {
        // Step 1: first save — get the database-generated id
        ShortUrl entity = ShortUrl.builder()
                .originalUrl(originalUrl)
                .build();
        ShortUrl saved = shortUrlRepository.saveAndFlush(entity);

        // Step 2: encode id → shortCode, then update the same row
        String shortCode = generateAvailableShortCode(saved.getId());
        saved.setShortCode(shortCode);
        shortUrlRepository.save(saved);

        log.info("Short URL created: shortCode={} originalUrl={}", shortCode, originalUrl);
        return shortCode;
    }

    private String generateAvailableShortCode(long seed) {
        long candidate = seed;
        String shortCode = Base62Encoder.encode(candidate);
        while (shortUrlRepository.existsByShortCode(shortCode)) {
            candidate++;
            shortCode = Base62Encoder.encode(candidate);
        }
        return shortCode;
    }

    @Override
    @Transactional
    public String getOriginalUrl(String shortCode) {
        // 1. Check Redis first
        String cached = redisTemplate.opsForValue().get(shortCode);
        if (cached != null) {
            System.out.println("CACHE HIT  \u2192 shortCode=" + shortCode);
            recordAccess(shortCode);
            return cached;
        }

        // 2. Cache miss — query PostgreSQL
        System.out.println("CACHE MISS \u2192 shortCode=" + shortCode + " (querying DB)");
        ShortUrl entity = shortUrlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ShortCodeNotFoundException("Short code not found: " + shortCode));

        if (entity.isExpired()) {
            throw new ShortCodeNotFoundException("Short code has expired: " + shortCode);
        }

        // 3. Store in Redis for future requests
        redisTemplate.opsForValue().set(shortCode, entity.getOriginalUrl());

        // 4. Record access in DB
        recordAccess(shortCode);

        return entity.getOriginalUrl();
    }

    private void recordAccess(String shortCode) {
        shortUrlRepository.recordAccessByShortCode(shortCode, LocalDateTime.now());
    }

    @Override
    @Transactional(readOnly = true)
    public UrlStatsResponse getStats(String shortCode) {
        ShortUrl entity = shortUrlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ShortCodeNotFoundException("Short code not found: " + shortCode));

        return new UrlStatsResponse(entity.getShortCode(), entity.getOriginalUrl(), entity.getClickCount());
    }

    @Override
    @Transactional(readOnly = true)
    public UrlAnalyticsResponse getAnalytics(String shortCode) {
        log.info("Analytics requested for shortCode={}", shortCode);
        ShortUrl entity = shortUrlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ShortCodeNotFoundException("Short code not found: " + shortCode));

        return new UrlAnalyticsResponse(
                entity.getShortCode(),
                entity.getOriginalUrl(),
                entity.getClickCount(),
                entity.getCreatedAt(),
                entity.getLastAccessedAt(),
                entity.getExpiresAt()
        );
    }
}
