package com.iqspark.underwriter.agent;

import com.iqspark.underwriter.domain.decision.Finding;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/** Inspects findings for condition-precedent knockouts and records a compliance clearance/breach. */
@Component
public class ComplianceAgent implements UnderwritingAgent {

    @Override
    public int order() {
        return 30;
    }

    @Override
    public void handle(UnderwritingContext context) {
        List<Finding> knockouts = context.findings().stream()
                .filter(Finding::isKnockout)
                .toList();

        if (knockouts.isEmpty()) {
            context.audit("ComplianceAgent",
                    "Compliance clearance: no condition-precedent breaches detected.");
        } else {
            String breaches = knockouts.stream().map(Finding::code).collect(Collectors.joining(", "));
            context.audit("ComplianceAgent",
                    "Condition-precedent breach(es) detected: " + breaches + " — forces DECLINE unless cured.");
        }
    }
}
