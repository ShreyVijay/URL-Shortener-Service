package com.shrey.urlshortener.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "short_urls",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_short_urls_short_code", columnNames = "short_code"),
        @UniqueConstraint(name = "uq_short_urls_custom_alias_key", columnNames = "custom_alias_key")
    },
    indexes = {
        @Index(name = "idx_short_urls_short_code",   columnList = "short_code"),
        @Index(name = "idx_short_urls_original_url", columnList = "original_url"),
        @Index(name = "idx_short_urls_expires_at",   columnList = "expires_at")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "originalUrl")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ShortUrl {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    /**
     * nullable = true to support the two-save pattern:
     * 1. save with shortCode=null → database generates the id
     * 2. encode id via Base62 → update shortCode
     * Every committed row is guaranteed to have a non-null shortCode
     * because both saves occur within the same @Transactional method.
     */
    @Column(name = "short_code", nullable = true, unique = true, length = 30)
    private String shortCode;

    @Column(name = "custom_alias_key", unique = true, length = 30)
    private String customAliasKey;

    @Column(name = "original_url", nullable = false, updatable = false,
            columnDefinition = "TEXT")
    private String originalUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "last_accessed_at")
    private LocalDateTime lastAccessedAt;

    @Column(name = "click_count", nullable = false)
    @Builder.Default
    private long clickCount = 0L;

    @PrePersist
    void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }
}
