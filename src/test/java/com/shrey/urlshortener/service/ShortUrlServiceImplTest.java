package com.shrey.urlshortener.service;

import com.shrey.urlshortener.entity.ShortUrl;
import com.shrey.urlshortener.exception.AliasAlreadyExistsException;
import com.shrey.urlshortener.exception.UrlExpiredException;
import com.shrey.urlshortener.repository.ShortUrlRepository;
import com.shrey.urlshortener.util.Base62Encoder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShortUrlServiceImplTest {

    @Mock
    private ShortUrlRepository shortUrlRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private ShortUrlServiceImpl shortUrlService;

    @Test
    void createsCustomAliasWhenAvailable() {
        when(shortUrlRepository.existsByShortCodeIgnoreCase("github")).thenReturn(false);
        when(shortUrlRepository.saveAndFlush(any(ShortUrl.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String shortCode = shortUrlService.createShortUrl("https://github.com", "github");

        assertThat(shortCode).isEqualTo("github");

        ArgumentCaptor<ShortUrl> captor = ArgumentCaptor.forClass(ShortUrl.class);
        verify(shortUrlRepository).saveAndFlush(captor.capture());
        ShortUrl saved = captor.getValue();
        assertThat(saved.getShortCode()).isEqualTo("github");
        assertThat(saved.getCustomAliasKey()).isEqualTo("github");
        assertThat(saved.getOriginalUrl()).isEqualTo("https://github.com");
    }

    @Test
    void customAliasDoesNotReuseExistingUrlMapping() {
        when(shortUrlRepository.existsByShortCodeIgnoreCase("docs_2026")).thenReturn(false);
        when(shortUrlRepository.saveAndFlush(any(ShortUrl.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String shortCode = shortUrlService.createShortUrl("https://example.com", "docs_2026");

        assertThat(shortCode).isEqualTo("docs_2026");
        verify(shortUrlRepository, never()).findByOriginalUrl("https://example.com");
    }

    @Test
    void createsGeneratedCodeWhenAliasIsNotProvided() {
        ShortUrl savedWithoutCode = ShortUrl.builder()
                .originalUrl("https://example.com")
                .build();
        savedWithoutCode.setId(125L);

        when(shortUrlRepository.findByOriginalUrl("https://example.com")).thenReturn(Optional.empty());
        when(shortUrlRepository.saveAndFlush(any(ShortUrl.class))).thenReturn(savedWithoutCode);
        when(shortUrlRepository.existsByShortCode(Base62Encoder.encode(125L))).thenReturn(false);
        when(shortUrlRepository.save(any(ShortUrl.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String shortCode = shortUrlService.createShortUrl("https://example.com", null);

        assertThat(shortCode).isEqualTo(Base62Encoder.encode(125L));
        assertThat(savedWithoutCode.getShortCode()).isEqualTo(shortCode);
    }

    @Test
    void createsGeneratedCodeWithExpirationWhenProvided() {
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(1);
        ShortUrl savedWithoutCode = ShortUrl.builder()
                .originalUrl("https://example.com")
                .expiresAt(expiresAt)
                .build();
        savedWithoutCode.setId(126L);

        when(shortUrlRepository.findByOriginalUrl("https://example.com")).thenReturn(Optional.empty());
        when(shortUrlRepository.saveAndFlush(any(ShortUrl.class))).thenReturn(savedWithoutCode);
        when(shortUrlRepository.existsByShortCode(Base62Encoder.encode(126L))).thenReturn(false);
        when(shortUrlRepository.save(any(ShortUrl.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String shortCode = shortUrlService.createShortUrl("https://example.com", null, expiresAt);

        assertThat(shortCode).isEqualTo(Base62Encoder.encode(126L));
        assertThat(savedWithoutCode.getExpiresAt()).isEqualTo(expiresAt);
    }

    @Test
    void createsCustomAliasWithExpirationWhenProvided() {
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(1);
        when(shortUrlRepository.existsByShortCodeIgnoreCase("github")).thenReturn(false);
        when(shortUrlRepository.saveAndFlush(any(ShortUrl.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String shortCode = shortUrlService.createShortUrl("https://github.com", "github", expiresAt);

        assertThat(shortCode).isEqualTo("github");
        ArgumentCaptor<ShortUrl> captor = ArgumentCaptor.forClass(ShortUrl.class);
        verify(shortUrlRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getExpiresAt()).isEqualTo(expiresAt);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "ab", "has space", "bad!", "this-alias-is-more-than-thirty-chars"})
    void rejectsInvalidAliasFormat(String customAlias) {
        assertThatThrownBy(() -> shortUrlService.createShortUrl("https://example.com", customAlias))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("customAlias must be 3-30 characters");

        verify(shortUrlRepository, never()).saveAndFlush(any(ShortUrl.class));
    }

    @Test
    void rejectsPastExpiration() {
        LocalDateTime expiresAt = LocalDateTime.now().minusMinutes(1);

        assertThatThrownBy(() -> shortUrlService.createShortUrl("https://example.com", null, expiresAt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expiresAt must be a future timestamp");

        verify(shortUrlRepository, never()).saveAndFlush(any(ShortUrl.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"api", "ACTUATOR", "Analytics"})
    void rejectsReservedAliases(String customAlias) {
        assertThatThrownBy(() -> shortUrlService.createShortUrl("https://example.com", customAlias))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("customAlias is reserved");

        verify(shortUrlRepository, never()).saveAndFlush(any(ShortUrl.class));
    }

    @Test
    void rejectsCaseInsensitiveAliasConflicts() {
        when(shortUrlRepository.existsByShortCodeIgnoreCase("GitHub")).thenReturn(true);

        assertThatThrownBy(() -> shortUrlService.createShortUrl("https://github.com", "GitHub"))
                .isInstanceOf(AliasAlreadyExistsException.class)
                .hasMessageContaining("GitHub");

        verify(shortUrlRepository, never()).saveAndFlush(any(ShortUrl.class));
    }

    @Test
    void convertsAliasRaceToConflict() {
        when(shortUrlRepository.existsByShortCodeIgnoreCase("github")).thenReturn(false);
        when(shortUrlRepository.saveAndFlush(any(ShortUrl.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate alias"));

        assertThatThrownBy(() -> shortUrlService.createShortUrl("https://github.com", "github"))
                .isInstanceOf(AliasAlreadyExistsException.class)
                .hasMessageContaining("github");
    }

    @Test
    void expiredRedirectThrowsGoneAndDoesNotRecordAccess() {
        ShortUrl expired = ShortUrl.builder()
                .shortCode("old")
                .originalUrl("https://example.com")
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .build();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("old")).thenReturn(null);
        when(shortUrlRepository.findByShortCode("old")).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> shortUrlService.getOriginalUrl("old"))
                .isInstanceOf(UrlExpiredException.class)
                .hasMessageContaining("old");

        verify(redisTemplate).delete("old");
        verify(shortUrlRepository, never()).recordAccessByShortCode(any(), any());
    }

    @Test
    void activeExpiringRedirectCachesWithTimeToLive() {
        ShortUrl active = ShortUrl.builder()
                .shortCode("soon")
                .originalUrl("https://example.com")
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("soon")).thenReturn(null);
        when(shortUrlRepository.findByShortCode("soon")).thenReturn(Optional.of(active));

        String originalUrl = shortUrlService.getOriginalUrl("soon");

        assertThat(originalUrl).isEqualTo("https://example.com");
        verify(valueOperations).set(eq("soon"), eq("https://example.com"), any(Duration.class));
        verify(shortUrlRepository).recordAccessByShortCode(any(), any());
    }
}
