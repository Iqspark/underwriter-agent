package com.iqspark.underwriter.rules;

import com.iqspark.underwriter.domain.decision.Finding;
import com.iqspark.underwriter.domain.decision.Severity;
import com.iqspark.underwriter.geo.GeoService;
import com.iqspark.underwriter.rules.config.RuleConfigLoader;
import com.iqspark.underwriter.support.Submissions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigurableRulesEngineTest {

    private final ConfigurableRulesEngine engine =
            new ConfigurableRulesEngine(new RuleConfigLoader(""), new FactExtractor(new GeoService()));

    private List<String> codes(List<Finding> findings) {
        return findings.stream().map(Finding::code).toList();
    }

    @Test
    void loadsRulePacks() {
        assertThat(new RuleConfigLoader("").rules()).isNotEmpty();
    }

    @Test
    void cleanFileHasNoKnockout() {
        List<Finding> findings = engine.evaluate(Submissions.vacantClean());
        assertThat(findings).noneMatch(f -> f.severity() == Severity.KNOCKOUT);
    }

    @Test
    void inspectionBreachIsAKnockout() {
        List<Finding> findings = engine.evaluate(Submissions.vacantKnockout());
        assertThat(findings).anyMatch(f ->
                f.code().equals("INSPECTION_INTERVAL_BREACH") && f.severity() == Severity.KNOCKOUT);
    }

    @Test
    void missingFieldsAreFlagged() {
        assertThat(codes(engine.evaluate(Submissions.missingMost()))).contains("MISSING_FIELD");
    }

    @Test
    void shortTermRentalWithoutEndorsementIsAKnockout() {
        assertThat(codes(engine.evaluate(Submissions.rentalStrNoEndorsement())))
                .contains("STR_WITHOUT_ENDORSEMENT");
    }

    @Test
    void contentsHighValueItemsFlagged() {
        assertThat(codes(engine.evaluate(Submissions.contents())))
                .contains("HIGH_VALUE_ITEMS_UNSCHEDULED");
    }

    @Test
    void interpolatesFactsIntoMessages() {
        List<Finding> findings = engine.evaluate(Submissions.vacantKnockout());
        Finding breach = findings.stream()
                .filter(f -> f.code().equals("INSPECTION_INTERVAL_BREACH"))
                .findFirst().orElseThrow();
        assertThat(breach.message()).contains("168");
    }
}
