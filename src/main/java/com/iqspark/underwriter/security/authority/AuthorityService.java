package com.iqspark.underwriter.security.authority;

import com.iqspark.underwriter.security.AppRoles;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Underwriting authority limits and four-eyes / segregation-of-duties rules (doc 11 §3.1).
 * Authorization is not just "can access" but "can <em>bind this risk</em>":
 *
 * <ul>
 *   <li>Approving within the user's authority (coverage ≤ limit) needs a single approval.</li>
 *   <li>High-value bindings (≥ four-eyes threshold) need dual control (two distinct approvers).</li>
 *   <li>Overriding a {@code DECLINE} always needs four-eyes and senior authority.</li>
 *   <li>Coverage beyond the user's authority is denied.</li>
 * </ul>
 *
 * Limits are configuration owned by UW leadership; the defaults are illustrative.
 */
@Service
public class AuthorityService {

    private final double underwriterLimit;
    private final double seniorLimit;
    private final double fourEyesThreshold;

    public AuthorityService(
            @Value("${underwriter.authority.underwriter-limit:1000000}") double underwriterLimit,
            @Value("${underwriter.authority.senior-limit:10000000}") double seniorLimit,
            @Value("${underwriter.authority.four-eyes-threshold:5000000}") double fourEyesThreshold) {
        this.underwriterLimit = underwriterLimit;
        this.seniorLimit = seniorLimit;
        this.fourEyesThreshold = fourEyesThreshold;
    }

    /**
     * @param roles    the approver's roles (bare names, no ROLE_ prefix)
     * @param coverage requested coverage / sum insured
     * @param override true when binding despite a {@code DECLINE} (override)
     */
    public AuthorityCheck evaluate(Set<String> roles, double coverage, boolean override) {
        boolean senior = roles.contains(AppRoles.SENIOR_UNDERWRITER);
        double limit = effectiveLimit(roles);

        if (override) {
            if (!senior) {
                return AuthorityCheck.denied("Overriding a DECLINE requires senior underwriting authority");
            }
            if (coverage > seniorLimit) {
                return AuthorityCheck.denied("Coverage exceeds senior authority");
            }
            return AuthorityCheck.fourEyes("Overriding a DECLINE requires four-eyes (two senior approvers)");
        }

        if (limit <= 0) {
            return AuthorityCheck.denied("No binding authority for this role");
        }
        if (coverage > limit) {
            return AuthorityCheck.denied("Coverage exceeds your authority limit");
        }
        if (coverage >= fourEyesThreshold) {
            return AuthorityCheck.fourEyes("High-value binding requires four-eyes (two distinct approvers)");
        }
        return AuthorityCheck.allowed("Within authority");
    }

    private double effectiveLimit(Set<String> roles) {
        double limit = 0.0;
        if (roles.contains(AppRoles.UNDERWRITER)) {
            limit = Math.max(limit, underwriterLimit);
        }
        if (roles.contains(AppRoles.SENIOR_UNDERWRITER)) {
            limit = Math.max(limit, seniorLimit);
        }
        return limit;
    }
}
