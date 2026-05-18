package com.shrey.urlshortener.service;

import com.shrey.urlshortener.dto.ShortenRequest;
import com.shrey.urlshortener.dto.ShortenResponse;
import com.shrey.urlshortener.dto.UrlInfoResponse;
import com.shrey.urlshortener.exception.AliasAlreadyExistsException;
import com.shrey.urlshortener.exception.ShortCodeNotFoundException;
import com.shrey.urlshortener.repository.UrlRepository;
import com.shrey.urlshortener.util.Base62Encoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UrlServiceImpl implements UrlService {

    private final UrlRepository urlRepository;

    @Override
    @Transactional
    public ShortenResponse shorten(ShortenRequest request) {
        // TODO (M1-5-3): implement shorten logic
        //  1. Check for existing record with same originalUrl → return if found (idempotent)
        //  2. Validate alias uniqueness if alias provided → throw AliasAlreadyExistsException
        //  3. Persist new UrlEntity (shortCode initially null)
        //  4. Compute shortCode = alias ?? Base62Encoder.encode(savedEntity.getId())
        //  5. Update shortCode on entity, save again, return ShortenResponse
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    @Transactional
    public String resolve(String shortCode) {
        // TODO (M1-5-4): implement resolve logic
        //  1. findByShortCode → throw ShortCodeNotFoundException if absent
        //  2. entity.incrementHitCount(), save
        //  3. return entity.getOriginalUrl()
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    @Transactional(readOnly = true)
    public UrlInfoResponse getInfo(String shortCode) {
        // TODO (M1-5-5): implement getInfo logic
        //  1. findByShortCode → throw ShortCodeNotFoundException if absent
        //  2. map entity → UrlInfoResponse
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
