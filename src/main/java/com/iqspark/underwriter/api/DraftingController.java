package com.iqspark.underwriter.api;

import com.iqspark.underwriter.drafting.Drafts;
import com.iqspark.underwriter.drafting.DraftingService;
import com.iqspark.underwriter.persistence.DecisionStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * On-demand drafting for a stored decision: quote, conditions, broker email, underwriter memo.
 * Authorized by the existing {@code GET /decisions/**} rule (underwriters/auditors).
 */
@RestController
@RequestMapping("/api/underwriting/decisions")
public class DraftingController {

    private final DecisionStore decisionStore;
    private final DraftingService draftingService;

    public DraftingController(DecisionStore decisionStore, DraftingService draftingService) {
        this.decisionStore = decisionStore;
        this.draftingService = draftingService;
    }

    @GetMapping("/{reference}/drafts")
    public ResponseEntity<Drafts> drafts(@PathVariable String reference) {
        return decisionStore.find(reference)
                .map(stored -> ResponseEntity.ok(draftingService.draft(stored)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
