package com.iqspark.underwriter.security.authority;

import com.iqspark.underwriter.persistence.DecisionStore;
import com.iqspark.underwriter.persistence.StoredDecision;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Records binding/approval actions and enforces four-eyes / segregation of duties. The approver
 * ledger is in-memory for this baseline slice (a durable, workflow-backed ledger arrives with the
 * event-driven runtime phase). Distinct approvers satisfy dual control — a single person approving
 * twice does not.
 */
@Service
public class BindingService {

    private final DecisionStore decisionStore;
    private final AuthorityService authorityService;
    private final Map<String, Set<String>> approversByReference = new ConcurrentHashMap<>();

    public BindingService(DecisionStore decisionStore, AuthorityService authorityService) {
        this.decisionStore = decisionStore;
        this.authorityService = authorityService;
    }

    /**
     * Submit an approval for a decision.
     *
     * @param action APPROVE (bind the recommendation) or OVERRIDE (bind despite a DECLINE)
     * @throws NoSuchElementException if the decision is not found
     * @throws AccessDeniedException  if the approver lacks authority for this action
     */
    public BindingStatus submitApproval(String reference, String approver, Set<String> roles, String action) {
        StoredDecision decision = decisionStore.find(reference)
                .orElseThrow(() -> new NoSuchElementException("No decision found for reference " + reference));

        boolean isDecline = "DECLINE".equalsIgnoreCase(decision.outcome());
        boolean override = "OVERRIDE".equalsIgnoreCase(action);

        if (isDecline && !override) {
            throw new AccessDeniedException(
                    "Cannot approve a DECLINE; use action=OVERRIDE with senior authority and four-eyes");
        }

        AuthorityCheck check = authorityService.evaluate(roles, decision.coverageAmount(), override);
        if (check.isDenied()) {
            throw new AccessDeniedException(check.reason());
        }

        Set<String> approvers = approversByReference.computeIfAbsent(reference,
                k -> ConcurrentHashMap.newKeySet());
        approvers.add(approver);

        int required = check.requiredApprovals();
        int received = approvers.size();
        String status = received >= required ? BindingStatus.BOUND : BindingStatus.PENDING;
        String message = received >= required
                ? "Binding approved (%s)".formatted(check.reason())
                : "%s — awaiting a second distinct approver".formatted(check.reason());
        return new BindingStatus(reference, status, required, received, message);
    }
}
