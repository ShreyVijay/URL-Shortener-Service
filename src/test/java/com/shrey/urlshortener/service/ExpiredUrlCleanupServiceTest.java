package com.shrey.urlshortener.service;

import com.shrey.urlshortener.repository.ShortUrlRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpiredUrlCleanupServiceTest {

    @Mock
    private ShortUrlRepository shortUrlRepository;

    @InjectMocks
    private ExpiredUrlCleanupService cleanupService;

    @Test
    void cleanupDeletesExpiredUrls() {
        when(shortUrlRepository.deleteExpiredUrls(any(LocalDateTime.class))).thenReturn(3);

        cleanupService.deleteExpiredUrls();

        verify(shortUrlRepository).deleteExpiredUrls(any(LocalDateTime.class));
    }
}
