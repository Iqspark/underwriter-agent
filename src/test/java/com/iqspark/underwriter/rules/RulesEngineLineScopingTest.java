package com.iqspark.underwriter.rules;

import com.iqspark.underwriter.domain.decision.Finding;
import com.iqspark.underwriter.domain.model.Submission;
import com.iqspark.underwriter.geo.GeoService;
import com.iqspark.underwriter.rules.config.RuleConfigLoader;
import com.iqspark.underwriter.support.Submissions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RulesEngineLineScopingTest {

    private final ConfigurableRulesEngine engine =
            new ConfigurableRulesEngine(new RuleConfigLoader(""), new FactExtractor(new GeoService()));

    private List<String> codes(Submission s) {
        return engine.evaluate(s).stream().map(Finding::code).toList();
    }

    @Test
    void vacantOnlyRulesDoNotFireOnRental() {
        List<String> codes = codes(Submissions.rentalStrNoEndorsement());
        assertThat(codes).contains("STR_WITHOUT_ENDORSEMENT");          // rental line
        assertThat(codes).doesNotContain("INSPECTION_INTERVAL_BREACH"); // vacant-home line
        assertThat(codes).doesNotContain("LONG_VACANCY");
    }

    @Test
    void contentsOnlyRulesFireOnContentsLine() {
        List<String> codes = codes(Submissions.contents());
        assertThat(codes).contains("HIGH_VALUE_ITEMS_UNSCHEDULED");     // contents line
        assertThat(codes).doesNotContain("INSPECTION_INTERVAL_BREACH"); // vacant-home line
    }

    @Test
    void sharedRulesApplyAcrossLines() {
        // LOCATION_WITHIN_RANGE is a shared (all-lines) rule — it fires for a resolved rental location.
        assertThat(codes(Submissions.rentalStrNoEndorsement())).contains("LOCATION_WITHIN_RANGE");
    }
}
