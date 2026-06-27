package com.iqspark.underwriter.history;

import com.iqspark.underwriter.history.model.HistoricalPolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Holds the book of business and precomputes {@link FeatureRanges}. Synthetic and deterministic
 * today (seeded), so the book is reproducible for tests; replace the generation here with a reader
 * over real policy + claims data (CSV/JDBC/warehouse) to go production-grade — everything
 * downstream (similarity, area stats, pricing, decisioning) is source-agnostic.
 */
@Repository
public class HistoricalPolicyRepository {

    private final List<HistoricalPolicy> book;
    private final FeatureRanges featureRanges;

    public HistoricalPolicyRepository(
            @Value("${underwriter.history.size:1500}") int size,
            @Value("${underwriter.history.seed:42}") long seed) {
        this.book = new SyntheticHistoryGenerator().generate(size, seed);
        this.featureRanges = FeatureRanges.computeFrom(book);
    }

    public List<HistoricalPolicy> all() {
        return book;
    }

    public FeatureRanges featureRanges() {
        return featureRanges;
    }

    public int size() {
        return book.size();
    }

    public double overallClaimRate() {
        if (book.isEmpty()) {
            return 0.0;
        }
        long claims = book.stream().filter(HistoricalPolicy::hadClaim).count();
        return (double) claims / book.size();
    }

    public BookStats stats() {
        return new BookStats(size(), overallClaimRate());
    }

    public record BookStats(int size, double claimRate) {}
}
