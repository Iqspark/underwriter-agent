package com.iqspark.underwriter.agent;

import com.iqspark.underwriter.domain.model.Submission;
import org.springframework.stereotype.Component;

/** Acknowledges and logs the submission. The extraction seam sits upstream (controller). */
@Component
public class IntakeAgent implements UnderwritingAgent {

    @Override
    public int order() {
        return 10;
    }

    @Override
    public void handle(UnderwritingContext context) {
        Submission s = context.submission();
        String ref = s.reference() != null ? s.reference() : "(no reference)";
        String where = "unknown location";
        if (s.location() != null) {
            where = "%s, %s".formatted(
                    s.location().city() != null ? s.location().city() : "?",
                    s.location().province() != null ? s.location().province() : "?");
        }
        context.audit("IntakeAgent", "Ingested submission %s (%s) at %s"
                .formatted(ref, s.effectiveLine(), where));
    }
}
