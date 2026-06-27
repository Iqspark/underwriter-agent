package com.iqspark.underwriter.history;

import com.iqspark.underwriter.history.model.AreaRiskStat;
import com.iqspark.underwriter.history.model.HistoricalPolicy;
import com.iqspark.underwriter.history.model.Peril;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Derives per-area claim/theft statistics from the book and the area pricing load. Theft is the
 * dominant vacant-property loss, so an elevated area theft rate both raises a finding and loads
 * the indicative premium.
 */
@Service
public class AreaRiskService {

    private final Map<String, AreaRiskStat> byCity = new HashMap<>();

    public AreaRiskService(HistoricalPolicyRepository repo) {
        build(repo.all());
    }

    private void build(List<HistoricalPolicy> book) {
        Map<String, int[]> counts = new HashMap<>();      // city -> {n, claims, theftClaims}
        Map<String, Double> lossSum = new HashMap<>();
        Map<String, String> display = new HashMap<>();

        for (HistoricalPolicy p : book) {
            if (p.city() == null || p.city().isBlank()) {
                continue;
            }
            String key = key(p.city());
            display.putIfAbsent(key, p.city());
            int[] c = counts.computeIfAbsent(key, k -> new int[3]);
            c[0]++;
            if (p.hadClaim()) {
                c[1]++;
                if (p.dominantPeril() == Peril.THEFT) {
                    c[2]++;
                }
            }
            lossSum.merge(key, p.lossRatio(), Double::sum);
        }

        for (Map.Entry<String, int[]> e : counts.entrySet()) {
            int[] c = e.getValue();
            int n = c[0];
            double overall = n == 0 ? 0 : (double) c[1] / n;
            double theft = n == 0 ? 0 : (double) c[2] / n;
            double avgLoss = n == 0 ? 0 : lossSum.getOrDefault(e.getKey(), 0.0) / n;
            byCity.put(e.getKey(), new AreaRiskStat(
                    display.get(e.getKey()), n,
                    round2(overall), round2(theft), round2(avgLoss)));
        }
    }

    public AreaRiskStat forCity(String city) {
        if (city == null || city.isBlank()) {
            return AreaRiskStat.unknown(city);
        }
        return byCity.getOrDefault(key(city), AreaRiskStat.unknown(city));
    }

    /**
     * Multiplicative pricing load for an area driven by its theft claim rate. Neutral (1.0) for an
     * unknown area; bounded to [0.9, 1.5] so the area signal nudges, not dominates, the price.
     */
    public double theftLoad(String city) {
        AreaRiskStat stat = forCity(city);
        if (stat.sampleSize() == 0) {
            return 1.0;
        }
        double load = 0.9 + stat.theftClaimRate();
        return Math.max(0.9, Math.min(1.5, load));
    }

    private static String key(String city) {
        return city.trim().toLowerCase();
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
