package com.iqspark.underwriter.security.authority;

/** Outcome of an authority check for a binding/approval action. */
public record AuthorityCheck(Decision decision, String reason) {

    public enum Decision { ALLOWED_SINGLE, REQUIRES_FOUR_EYES, DENIED }

    public static AuthorityCheck allowed(String reason) {
        return new AuthorityCheck(Decision.ALLOWED_SINGLE, reason);
    }

    public static AuthorityCheck fourEyes(String reason) {
        return new AuthorityCheck(Decision.REQUIRES_FOUR_EYES, reason);
    }

    public static AuthorityCheck denied(String reason) {
        return new AuthorityCheck(Decision.DENIED, reason);
    }

    public boolean isDenied() {
        return decision == Decision.DENIED;
    }

    public int requiredApprovals() {
        return decision == Decision.REQUIRES_FOUR_EYES ? 2 : 1;
    }
}
