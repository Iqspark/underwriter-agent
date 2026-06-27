package com.iqspark.underwriter.security.authority;

/** The state of a binding/approval after an approval is submitted. */
public record BindingStatus(
        String reference,
        String status,            // BOUND | PENDING_SECOND_APPROVAL
        int requiredApprovals,
        int receivedApprovals,
        String message
) {
    public static final String BOUND = "BOUND";
    public static final String PENDING = "PENDING_SECOND_APPROVAL";
}
