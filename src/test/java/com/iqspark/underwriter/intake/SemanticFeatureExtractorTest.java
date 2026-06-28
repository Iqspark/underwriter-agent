package com.iqspark.underwriter.intake;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SemanticFeatureExtractorTest {

    private final SemanticFeatureExtractor extractor = new SemanticFeatureExtractor(null); // offline heuristic

    @Test
    void extractsHeuristicFeaturesFromNotes() {
        SemanticFeatures f = extractor.extract(
                "Inspection: signs of deferred maintenance and rot; mold in the basement. "
                        + "Overall poor condition.");
        assertThat(f.present()).isTrue();
        assertThat(f.deferredMaintenancePresent()).isTrue();
        assertThat(f.hazards()).contains("mold");
        assertThat(f.inspectorSentiment()).isEqualTo("NEGATIVE");
        assertThat(f.source()).isEqualTo("heuristic");
    }

    @Test
    void blankTextYieldsEmpty() {
        assertThat(extractor.extract("  ").present()).isFalse();
        assertThat(extractor.extract(null).present()).isFalse();
    }
}
