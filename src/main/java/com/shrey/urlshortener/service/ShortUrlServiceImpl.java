package com.shrey.urlshortener.service;

import com.shrey.urlshortener.entity.ShortUrl;
import com.shrey.urlshortener.exception.ShortCodeNotFoundException;
import com.shrey.urlshortener.repository.ShortUrlRepository;
import com.shrey.urlshortener.util.Base62Encoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShortUrlServiceImpl implements ShortUrlService {

    private final ShortUrlRepository shortUrlRepository;
    private final StringRedisTemplate redisTemplate;

    @Override
    @Transactional
    public String createShortUrl(String originalUrl) {
        if (originalUrl == null || originalUrl.isBlank()) {
            throw new IllegalArgumentException("originalUrl must not be blank");
        }

        // Idempotency: if the URL was already shortened, return the existing code.
        return shortUrlRepository.findByOriginalUrl(originalUrl)
                .map(ShortUrl::getShortCode)
                .orElseGet(() -> createAndPersist(originalUrl));
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
        String shortCode = Base62Encoder.encode(saved.getId());
        saved.setShortCode(shortCode);
        shortUrlRepository.save(saved);

        log.info("Short URL created: shortCode={} originalUrl={}", shortCode, originalUrl);
        return shortCode;
    }

    @Override
    @Transactional
    public String getOriginalUrl(String shortCode) {
        // 1. Check Redis first
        String cached = redisTemplate.opsForValue().get(shortCode);
        if (cached != null) {
            System.out.println("CACHE HIT  → shortCode=" + shortCode);
            return cached;
        }

        // 2. Cache miss — query PostgreSQL
        System.out.println("CACHE MISS → shortCode=" + shortCode + " (querying DB)");
        ShortUrl entity = shortUrlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ShortCodeNotFoundException("Short code not found: " + shortCode));

        if (entity.isExpired()) {
            throw new ShortCodeNotFoundException("Short code has expired: " + shortCode);
        }

        // 3. Store in Redis for next time
        redisTemplate.opsForValue().set(shortCode, entity.getOriginalUrl());

        // 4. Increment click count in DB
        shortUrlRepository.incrementClickCount(entity.getId());

        return entity.getOriginalUrl();
    }
}
