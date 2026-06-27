package com.iqspark.underwriter.api;

import com.iqspark.underwriter.agent.DecisionOrchestrator;
import com.iqspark.underwriter.domain.decision.Decision;
import com.iqspark.underwriter.domain.model.Submission;
import com.iqspark.underwriter.extraction.DocumentExtractor;
import com.iqspark.underwriter.persistence.DecisionStore;
import com.iqspark.underwriter.persistence.StoredDecision;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** REST surface for underwriting submissions and documents, plus stored-decision retrieval. */
@RestController
@RequestMapping("/api/underwriting")
public class UnderwritingController {

    private final DecisionOrchestrator orchestrator;
    private final DocumentExtractor documentExtractor;
    private final DecisionStore decisionStore;

    public UnderwritingController(DecisionOrchestrator orchestrator,
                                 DocumentExtractor documentExtractor,
                                 DecisionStore decisionStore) {
        this.orchestrator = orchestrator;
        this.documentExtractor = documentExtractor;
        this.decisionStore = decisionStore;
    }

    @PostMapping(value = "/submissions", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Decision underwrite(@RequestBody Submission submission) {
        return orchestrator.decide(submission);
    }

    @PostMapping(value = "/documents", consumes = MediaType.TEXT_PLAIN_VALUE)
    public Decision underwriteDocument(@RequestBody String rawText) {
        Submission submission = documentExtractor.extract(rawText);
        return orchestrator.decide(submission);
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "service", "underwriter-agent");
    }

    @GetMapping("/decisions/{reference}")
    public ResponseEntity<StoredDecision> decision(@PathVariable String reference) {
        return decisionStore.find(reference)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
