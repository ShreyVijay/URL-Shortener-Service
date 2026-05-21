package com.shrey.urlshortener.service;

import com.shrey.urlshortener.entity.ShortUrl;
import com.shrey.urlshortener.exception.AliasAlreadyExistsException;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShortUrlServiceImplTest {

    @Mock
    private ShortUrlRepository shortUrlRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

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

    @ParameterizedTest
    @ValueSource(strings = {"", "ab", "has space", "bad!", "this-alias-is-more-than-thirty-chars"})
    void rejectsInvalidAliasFormat(String customAlias) {
        assertThatThrownBy(() -> shortUrlService.createShortUrl("https://example.com", customAlias))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("customAlias must be 3-30 characters");

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
}
