package com.iqspark.underwriter.rules;

import com.iqspark.underwriter.geo.GeoService;
import com.iqspark.underwriter.support.Submissions;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FactExtractorTest {

    private final FactExtractor extractor = new FactExtractor(new GeoService());

    @Test
    void derivesPresenceFlagsAndGeoForACleanVacantFile() {
        Map<String, Object> f = extractor.extract(Submissions.vacantClean());
        assertThat(f.get("missingReference")).isEqualTo(false);
        assertThat(f.get("missingApplicantName")).isEqualTo(false);
        assertThat(f.get("missingLocation")).isEqualTo(false);
        assertThat(f.get("remote")).isEqualTo(false);           // Toronto is within range
        assertThat(f.get("inspectionIntervalHours")).isEqualTo(24);
        assertThat(f.get("vacancyPresent")).isEqualTo(true);
        assertThat(((Number) f.get("perSqft")).longValue()).isEqualTo(375L); // 900000 / 2400
        assertThat(((Number) f.get("monthsVacant")).longValue()).isGreaterThanOrEqualTo(1L);
    }

    @Test
    void flagsRemoteLocation() {
        assertThat(extractor.extract(Submissions.vacantRemoteRisky()).get("remote")).isEqualTo(true);
    }

    @Test
    void derivesRentalFacts() {
        Map<String, Object> f = extractor.extract(Submissions.rentalStrNoEndorsement());
        assertThat(f.get("shortTermRental")).isEqualTo(true);
        assertThat(f.get("shortTermRentalEndorsed")).isEqualTo(false);
        assertThat(f.get("liabilityLimitMissing")).isEqualTo(false);
    }

    @Test
    void derivesContentsFacts() {
        Map<String, Object> f = extractor.extract(Submissions.contents());
        assertThat(f.get("contentsValueMissing")).isEqualTo(false);
        assertThat(f.get("highValueItemsScheduled")).isEqualTo(false);
        assertThat(f.get("noSecurityDevice")).isEqualTo(false); // securityDevice == true
    }

    @Test
    void flagsMissingFieldsOnAnEmptySubmission() {
        Map<String, Object> f = extractor.extract(Submissions.missingMost());
        assertThat(f.get("missingReference")).isEqualTo(true);
        assertThat(f.get("missingApplicantName")).isEqualTo(true);
        assertThat(f.get("missingLocation")).isEqualTo(true);
        assertThat(f.get("missingBuilding")).isEqualTo(true);
    }
}
