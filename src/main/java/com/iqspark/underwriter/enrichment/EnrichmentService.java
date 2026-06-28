package com.iqspark.underwriter.enrichment;

import com.iqspark.underwriter.domain.model.Submission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Calls the enrichment provider behind a cache and a safety net: a provider failure degrades to
 * {@link Enrichment#unavailable()} (book-only signals) rather than failing the submission
 * (degrade-to-floor, doc 12). Responses are cached by location to bound cost/latency (doc 14).
 */
@Service
public class EnrichmentService {

    private static final Logger log = LoggerFactory.getLogger(EnrichmentService.class);

    private final EnrichmentProvider provider;
    private final EnrichmentProperties properties;
    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public EnrichmentService(EnrichmentProvider provider, EnrichmentProperties properties) {
        this.provider = provider;
        this.properties = properties;
    }

    public Enrichment enrich(Submission submission) {
        String key = cacheKey(submission);
        if (key != null) {
            CacheEntry cached = cache.get(key);
            if (cached != null && !cached.isExpired(properties.getCacheTtlSeconds())) {
                return cached.value();
            }
        }
        Enrichment result;
        try {
            result = provider.enrich(submission);
        } catch (Exception e) {
            log.warn("Enrichment provider '{}' failed; degrading to book-only: {}",
                    provider.name(), e.getMessage());
            result = Enrichment.unavailable();
        }
        if (key != null && result.available()) {
            cache.put(key, new CacheEntry(result, Instant.now()));
        }
        return result;
    }

    private static String cacheKey(Submission s) {
        if (s.location() == null) {
            return null;
        }
        return "%s|%s|%s|%s".formatted(
                s.location().city(), s.location().province(),
                s.location().latitude(), s.location().longitude());
    }

    private record CacheEntry(Enrichment value, Instant at) {
        boolean isExpired(long ttlSeconds) {
            return Instant.now().isAfter(at.plus(Duration.ofSeconds(ttlSeconds)));
        }
    }
}
