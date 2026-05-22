package com.shrey.urlshortener.repository;

import com.shrey.urlshortener.entity.ShortUrl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface ShortUrlRepository extends JpaRepository<ShortUrl, Long> {

    Optional<ShortUrl> findByShortCode(String shortCode);

    boolean existsByShortCodeIgnoreCase(String shortCode);

    Optional<ShortUrl> findByOriginalUrl(String originalUrl);

    boolean existsByShortCode(String shortCode);

    @Modifying
    @Query("UPDATE ShortUrl s SET s.clickCount = s.clickCount + 1, s.lastAccessedAt = :accessedAt WHERE s.shortCode = :shortCode")
    void recordAccessByShortCode(@Param("shortCode") String shortCode, @Param("accessedAt") LocalDateTime accessedAt);

    @Modifying
    @Query("DELETE FROM ShortUrl s WHERE s.expiresAt IS NOT NULL AND s.expiresAt <= :now")
    int deleteExpiredUrls(@Param("now") LocalDateTime now);
}
