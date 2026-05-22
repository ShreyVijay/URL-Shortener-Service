package com.shrey.urlshortener.service;

import com.shrey.urlshortener.repository.ShortUrlRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExpiredUrlCleanupService {

    private final ShortUrlRepository shortUrlRepository;

    @Scheduled(fixedDelayString = "${app.cleanup.expired-urls-ms:3600000}")
    @Transactional
    public void deleteExpiredUrls() {
        int deleted = shortUrlRepository.deleteExpiredUrls(LocalDateTime.now());
        if (deleted > 0) {
            log.info("Expired URL cleanup deleted {} rows", deleted);
        } else {
            log.debug("Expired URL cleanup found no rows to delete");
        }
    }
}
