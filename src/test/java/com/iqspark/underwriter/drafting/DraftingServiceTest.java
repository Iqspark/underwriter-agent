package com.iqspark.underwriter.drafting;

import com.iqspark.underwriter.domain.decision.Finding;
import com.iqspark.underwriter.domain.decision.Severity;
import com.iqspark.underwriter.domain.model.Money;
import com.iqspark.underwriter.persistence.StoredDecision;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DraftingServiceTest {

    private final DraftingService service = new DraftingService(null); // offline templates

    @Test
    void draftsAllFourArtifactsOffline() {
        StoredDecision sd = new StoredDecision(
                "DR-1", "VACANT_HOME", "REFER", 9, 500_000, Money.cad(1234.50), "Refer for review.",
                List.of(new Finding("OLD_ROOF", Severity.MEDIUM, "RISK", "Roof age 30 years", "…", "rule")),
                List.of("Obtain updated roof report"), null, Instant.now(), List.of(), true);

        Drafts drafts = service.draft(sd);

        assertThat(drafts.quoteSummary()).contains("DR-1").contains("REFER");
        assertThat(drafts.conditionsSummary()).contains("roof report");
        assertThat(drafts.brokerEmail()).isNotBlank();
        assertThat(drafts.underwriterMemo()).contains("REFER").contains("OLD_ROOF");
    }
}
