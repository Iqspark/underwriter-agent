package com.iqspark.underwriter.history;

import com.iqspark.underwriter.history.model.HistoricalPolicy;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SyntheticHistoryGeneratorTest {

    private final SyntheticHistoryGenerator generator = new SyntheticHistoryGenerator();

    @Test
    void generatesRequestedSize() {
        assertThat(generator.generate(500, 42)).hasSize(500);
    }

    @Test
    void isDeterministicForASeed() {
        assertThat(generator.generate(300, 42)).isEqualTo(generator.generate(300, 42));
    }

    @Test
    void differentSeedsProduceDifferentBooks() {
        assertThat(generator.generate(300, 1)).isNotEqualTo(generator.generate(300, 2));
    }

    @Test
    void hasLearnableMixOfOutcomesAndPopulatedCities() {
        List<HistoricalPolicy> book = generator.generate(800, 42);
        long claims = book.stream().filter(HistoricalPolicy::hadClaim).count();
        assertThat(claims).isGreaterThan(0).isLessThan(book.size());
        assertThat(book).allSatisfy(p -> {
            assertThat(p.line()).isNotNull();
            assertThat(p.city()).isNotBlank();
            assertThat(p.lossRatio()).isGreaterThanOrEqualTo(0.0);
        });
    }
}
