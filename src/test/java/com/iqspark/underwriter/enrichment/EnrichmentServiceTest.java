package com.iqspark.underwriter.enrichment;

import com.iqspark.underwriter.domain.model.Submission;
import com.iqspark.underwriter.support.Submissions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class EnrichmentServiceTest {

    @Test
    void cachesByLocation() {
        AtomicInteger calls = new AtomicInteger();
        EnrichmentProvider counting = new EnrichmentProvider() {
            @Override public String name() { return "counting"; }
            @Override public Enrichment enrich(Submission s) {
                calls.incrementAndGet();
                return new Enrichment(true, "counting", 0.5, 0.5, 0.5, 0.5);
            }
        };
        EnrichmentService service = new EnrichmentService(counting, new EnrichmentProperties());

        service.enrich(Submissions.vacantClean());
        service.enrich(Submissions.vacantClean()); // same location -> cached

        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    void degradesToUnavailableWhenProviderFails() {
        EnrichmentProvider failing = new EnrichmentProvider() {
            @Override public String name() { return "failing"; }
            @Override public Enrichment enrich(Submission s) { throw new RuntimeException("provider down"); }
        };
        EnrichmentService service = new EnrichmentService(failing, new EnrichmentProperties());

        Enrichment result = service.enrich(Submissions.vacantClean());

        assertThat(result.available()).isFalse();
    }
}
