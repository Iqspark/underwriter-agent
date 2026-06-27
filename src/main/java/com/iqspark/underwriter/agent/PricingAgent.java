package com.iqspark.underwriter.agent;

import com.iqspark.underwriter.domain.model.Money;
import com.iqspark.underwriter.domain.model.Submission;
import com.iqspark.underwriter.history.AreaRiskService;
import com.iqspark.underwriter.history.model.LearnedAssessment;
import org.springframework.stereotype.Component;

/**
 * Prices from the comparable fair rate × area theft load. On a cold-start book it falls back to a
 * base rate plus a rule-derived risk load. The premium is floored at a minimum.
 *
 * <pre>premium = (coverage / 1000) × suggestedRatePerThousand × areaTheftLoad</pre>
 */
@Component
public class PricingAgent implements UnderwritingAgent {

    private static final double BASE_RATE_PER_THOUSAND = 4.5;
    private static final double MIN_PREMIUM = 750.0;
    private static final double RISK_LOAD_PER_POINT = 0.03;

    private final AreaRiskService areaRiskService;

    public PricingAgent(AreaRiskService areaRiskService) {
        this.areaRiskService = areaRiskService;
    }

    @Override
    public int order() {
        return 40;
    }

    @Override
    public void handle(UnderwritingContext context) {
        Submission s = context.submission();
        double coverage = s.requestedCoverage() != null ? s.requestedCoverage().amount() : 0.0;
        String city = s.location() != null ? s.location().city() : null;
        double areaLoad = areaRiskService.theftLoad(city);

        LearnedAssessment learned = context.learnedAssessment();
        double rate;
        String basis;
        if (learned != null && !learned.coldStart() && learned.suggestedRatePerThousand() > 0) {
            rate = learned.suggestedRatePerThousand();
            basis = "comparable fair rate";
        } else {
            rate = BASE_RATE_PER_THOUSAND * (1.0 + context.pricingScore() * RISK_LOAD_PER_POINT);
            basis = "cold-start base rate + risk load";
        }

        double premium = Math.max(MIN_PREMIUM, (coverage / 1000.0) * rate * areaLoad);
        premium = Math.round(premium * 100.0) / 100.0;
        context.setIndicativePremium(Money.cad(premium));

        context.audit("PricingAgent",
                "Indicative premium CAD %.2f (%s %.2f/1000 × area load %.2f on coverage %.0f)"
                        .formatted(premium, basis, rate, areaLoad, coverage));
    }
}
