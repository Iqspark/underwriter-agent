package com.iqspark.underwriter.extraction;

import com.iqspark.underwriter.domain.model.LineOfBusiness;
import com.iqspark.underwriter.domain.model.Submission;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KeyValueDocumentExtractorTest {

    private final KeyValueDocumentExtractor extractor = new KeyValueDocumentExtractor();

    @Test
    void parsesRecognisedKeysIntoASubmission() {
        String doc = """
                reference: TRM-1
                applicant: Acme Inc.
                city: Toronto
                province: ON
                roof_age: 24
                inspection_interval_hours: 96
                utilities_on: yes
                water_shut_off: no
                security_system: false
                requested_coverage: 450000
                notes: deferred maintenance noted
                """;
        Submission s = extractor.extract(doc);

        assertThat(s.reference()).isEqualTo("TRM-1");
        assertThat(s.applicant().name()).isEqualTo("Acme Inc.");
        assertThat(s.location().city()).isEqualTo("Toronto");
        assertThat(s.location().province()).isEqualTo("ON");
        assertThat(s.building().roofAgeYears()).isEqualTo(24);
        assertThat(s.vacancy().inspectionIntervalHours()).isEqualTo(96);
        assertThat(s.vacancy().utilitiesOn()).isTrue();
        assertThat(s.vacancy().waterShutOff()).isFalse();
        assertThat(s.vacancy().securitySystem()).isFalse();
        assertThat(s.requestedCoverage().amount()).isEqualTo(450_000.0);
        assertThat(s.notes()).contains("deferred maintenance");
        assertThat(s.effectiveLine()).isEqualTo(LineOfBusiness.VACANT_HOME);
    }

    @Test
    void honoursDeclaredLineOfBusiness() {
        assertThat(extractor.extract("line_of_business: RENTAL").effectiveLine())
                .isEqualTo(LineOfBusiness.RENTAL);
    }

    @Test
    void absentKeysLeaveNullsForCompletenessRules() {
        Submission s = extractor.extract("city: Toronto");
        assertThat(s.reference()).isNull();
        assertThat(s.applicant()).isNull();   // no applicant keys present
        assertThat(s.building()).isNull();     // no building keys present
        assertThat(s.location().city()).isEqualTo("Toronto");
    }

    @Test
    void neverInventsDataFromUnknownKeys() {
        Submission s = extractor.extract("totally_unknown: value\nanother: thing");
        assertThat(s.reference()).isNull();
        assertThat(s.location()).isNull();
    }
}
