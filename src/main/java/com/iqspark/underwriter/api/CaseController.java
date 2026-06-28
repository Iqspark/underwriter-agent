package com.iqspark.underwriter.api;

import com.iqspark.underwriter.domain.model.Submission;
import com.iqspark.underwriter.runtime.CaseService;
import com.iqspark.underwriter.runtime.CaseView;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Async intake (the event-driven path): {@code POST /cases} accepts a submission as a durable case
 * and returns {@code 202 Accepted} with the case id; the client polls {@code GET /cases/{id}} for the
 * status and, once processed, the decision. The synchronous {@code /submissions} fast-path remains.
 */
@RestController
@RequestMapping("/api/underwriting/cases")
public class CaseController {

    private final CaseService caseService;

    public CaseController(CaseService caseService) {
        this.caseService = caseService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CaseView> submit(@RequestBody Submission submission) {
        return ResponseEntity.accepted().body(caseService.submit(submission));
    }

    @GetMapping("/{caseId}")
    public ResponseEntity<CaseView> get(@PathVariable String caseId) {
        return caseService.find(caseId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
