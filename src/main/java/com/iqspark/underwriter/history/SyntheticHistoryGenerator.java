package com.iqspark.underwriter.history;

import com.iqspark.underwriter.domain.model.LineOfBusiness;
import com.iqspark.underwriter.history.model.HistoricalPolicy;
import com.iqspark.underwriter.history.model.Peril;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Deterministically generates a realistic book of business for a given seed, embedding
 * <em>learnable</em> patterns (doc 5 §7) so the case-based core has something real to learn:
 * cities carry theft/claim biases; claim probability rises with vacancy duration, roof age,
 * missing security, long inspection intervals, prior losses and fire-hall distance; the claim
 * peril tracks the dominant driver; and the charged rate tracks technical risk.
 *
 * <p>The book is partitioned by line of business; the vacant-home line carries the richest
 * pattern set, with rental and contents lines generated alongside it.
 */
public class SyntheticHistoryGenerator {

    private record CityProfile(String city, String province, double theftBias, double claimBias) {}

    private static final List<CityProfile> CITIES = List.of(
            new CityProfile("Toronto", "ON", 0.15, 0.12),
            new CityProfile("Ottawa", "ON", 0.12, 0.11),
            new CityProfile("Sudbury", "ON", 0.28, 0.20),
            new CityProfile("Winnipeg", "MB", 0.33, 0.25),
            new CityProfile("Flin Flon", "MB", 0.35, 0.30),
            new CityProfile("Halifax", "NS", 0.10, 0.10),
            new CityProfile("Vancouver", "BC", 0.20, 0.15),
            new CityProfile("Calgary", "AB", 0.14, 0.13),
            new CityProfile("Montreal", "QC", 0.18, 0.14),
            new CityProfile("Saskatoon", "SK", 0.30, 0.22));

    private static final String[] CONSTRUCTIONS = {"Frame", "Masonry"};

    public List<HistoricalPolicy> generate(int size, long seed) {
        Random rnd = new Random(seed);
        List<HistoricalPolicy> book = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            book.add(generateOne(i, rnd));
        }
        return List.copyOf(book);
    }

    private HistoricalPolicy generateOne(int i, Random rnd) {
        LineOfBusiness line = pickLine(rnd);
        CityProfile cp = CITIES.get(rnd.nextInt(CITIES.size()));
        String id = "HP-%05d".formatted(i);

        return switch (line) {
            case RENTAL -> generateRental(id, cp, rnd);
            case CONTENTS -> generateContents(id, cp, rnd);
            default -> generateVacantHome(id, cp, rnd);
        };
    }

    private LineOfBusiness pickLine(Random rnd) {
        double r = rnd.nextDouble();
        if (r < 0.60) return LineOfBusiness.VACANT_HOME;
        if (r < 0.85) return LineOfBusiness.RENTAL;
        return LineOfBusiness.CONTENTS;
    }

    private HistoricalPolicy generateVacantHome(String id, CityProfile cp, Random rnd) {
        int roofAge = 3 + rnd.nextInt(38);                 // 3..40
        int vacancyMonths = 1 + rnd.nextInt(36);           // 1..36
        int inspection = pickInspectionInterval(rnd);      // 24..168
        int fireHallKm = 1 + rnd.nextInt(30);              // 1..30
        boolean security = rnd.nextDouble() < 0.55;
        boolean alarm = security && rnd.nextDouble() < 0.6;
        int priorLoss = samplePriorLoss(rnd);
        int units = 1 + (rnd.nextDouble() < 0.15 ? 1 : 0);
        int sqft = 800 + rnd.nextInt(3200);
        int yearBuilt = 1900 + rnd.nextInt(121);           // 1900..2020
        double coverage = 200_000 + rnd.nextInt(1_000_001);
        String construction = CONSTRUCTIONS[rnd.nextInt(CONSTRUCTIONS.length)];

        double p = cp.claimBias()
                + (vacancyMonths / 36.0) * 0.25
                + Math.max(0, roofAge - 15) / 25.0 * 0.15
                + (security ? 0.0 : 0.12)
                + Math.max(0, inspection - 72) / 96.0 * 0.15
                + priorLoss * 0.08
                + Math.max(0, fireHallKm - 13) / 20.0 * 0.10
                + cp.theftBias() * 0.20;
        p = clamp(p, 0.02, 0.95);

        boolean claim = rnd.nextDouble() < p;
        Peril peril = claim ? dominantPeril(cp, security, roofAge, fireHallKm, vacancyMonths) : Peril.NONE;
        double lossRatio = sampleLossRatio(claim, peril, rnd);
        double rate = 3.0 + p * 9.0 + rnd.nextDouble();

        return new HistoricalPolicy(id, LineOfBusiness.VACANT_HOME, cp.city(), cp.province(),
                construction, "Detached Home", units, sqft, yearBuilt, roofAge, vacancyMonths,
                inspection, fireHallKm, security, alarm, priorLoss, coverage,
                claim, lossRatio, rate, peril);
    }

    private HistoricalPolicy generateRental(String id, CityProfile cp, Random rnd) {
        int roofAge = 3 + rnd.nextInt(30);
        int fireHallKm = 1 + rnd.nextInt(20);
        int priorLoss = samplePriorLoss(rnd);
        int units = 2 + rnd.nextInt(8);
        int sqft = 2000 + rnd.nextInt(8000);
        int yearBuilt = 1920 + rnd.nextInt(101);
        double coverage = 400_000 + rnd.nextInt(1_100_001);
        String construction = CONSTRUCTIONS[rnd.nextInt(CONSTRUCTIONS.length)];
        boolean shortTerm = rnd.nextDouble() < 0.25;

        double p = cp.claimBias()
                + (shortTerm ? 0.10 : 0.0)
                + priorLoss * 0.08
                + units / 40.0;
        p = clamp(p, 0.02, 0.9);

        boolean claim = rnd.nextDouble() < p;
        Peril peril = claim ? (rnd.nextDouble() < 0.5 ? Peril.WATER : Peril.OTHER) : Peril.NONE;
        double lossRatio = sampleLossRatio(claim, peril, rnd);
        double rate = 2.5 + p * 8.0 + rnd.nextDouble();

        return new HistoricalPolicy(id, LineOfBusiness.RENTAL, cp.city(), cp.province(),
                construction, "Apartment", units, sqft, yearBuilt, roofAge, 0,
                0, fireHallKm, true, false, priorLoss, coverage,
                claim, lossRatio, rate, peril);
    }

    private HistoricalPolicy generateContents(String id, CityProfile cp, Random rnd) {
        int priorLoss = samplePriorLoss(rnd);
        double coverage = 20_000 + rnd.nextInt(130_001);
        boolean security = rnd.nextDouble() < 0.6;

        double p = cp.claimBias()
                + cp.theftBias() * 0.30
                + (security ? 0.0 : 0.10)
                + priorLoss * 0.07;
        p = clamp(p, 0.02, 0.9);

        boolean claim = rnd.nextDouble() < p;
        Peril peril = claim ? (rnd.nextDouble() < 0.7 ? Peril.THEFT : Peril.OTHER) : Peril.NONE;
        double lossRatio = sampleLossRatio(claim, peril, rnd);
        double rate = 4.0 + p * 10.0 + rnd.nextDouble();

        return new HistoricalPolicy(id, LineOfBusiness.CONTENTS, cp.city(), cp.province(),
                "", "", 0, 0, 0, 0, 0,
                0, 0, security, false, priorLoss, coverage,
                claim, lossRatio, rate, peril);
    }

    private Peril dominantPeril(CityProfile cp, boolean security, int roofAge, int fireHallKm, int vacancyMonths) {
        double theft = (security ? 0.0 : 1.0) + cp.theftBias() * 2.0;
        double water = Math.max(0, roofAge - 20) / 20.0 * 1.5;
        double fire = Math.max(0, fireHallKm - 13) / 20.0 * 1.5;
        double vandalism = vacancyMonths / 36.0 * 1.5;
        double other = 0.3;

        Peril best = Peril.OTHER;
        double bestVal = other;
        if (theft > bestVal) { bestVal = theft; best = Peril.THEFT; }
        if (water > bestVal) { bestVal = water; best = Peril.WATER; }
        if (fire > bestVal) { bestVal = fire; best = Peril.FIRE; }
        if (vandalism > bestVal) { best = Peril.VANDALISM; }
        return best;
    }

    private int pickInspectionInterval(Random rnd) {
        double r = rnd.nextDouble();
        if (r < 0.45) return 24;
        if (r < 0.70) return 48;
        if (r < 0.85) return 72;
        if (r < 0.95) return 96;
        return 168;
    }

    private int samplePriorLoss(Random rnd) {
        double r = rnd.nextDouble();
        if (r < 0.80) return 0;
        if (r < 0.92) return 1;
        if (r < 0.98) return 2;
        return 3;
    }

    private double sampleLossRatio(boolean claim, Peril peril, Random rnd) {
        if (!claim) {
            return round2(rnd.nextDouble() * 0.3);
        }
        double base = 0.6 + rnd.nextDouble() * 1.2;          // 0.6..1.8
        if (peril == Peril.FIRE) {
            base += 0.3;
        }
        return round2(base);
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
