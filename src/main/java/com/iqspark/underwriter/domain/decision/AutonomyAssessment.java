package com.iqspark.underwriter.domain.decision;

import java.util.List;

/**
 * The routing outcome for a decision: which {@link AutonomyTier} it falls into, whether it was
 * selected for QA sampling (auto-approvals only), and the reasons behind the tier. Advisory routing
 * metadata — the deterministic outcome and human-in-the-loop guarantees are unchanged.
 */
public record AutonomyAssessment(AutonomyTier tier, boolean qaSampled, List<String> reasons) {}
