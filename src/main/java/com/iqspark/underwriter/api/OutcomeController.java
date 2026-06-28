package com.iqspark.underwriter.api;

import com.iqspark.underwriter.flywheel.OutcomeRequest;
import com.iqspark.underwriter.flywheel.OutcomeService;
import com.iqspark.underwriter.flywheel.RealizedOutcomeEntity;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Records realized outcomes (claims/loss) from the PAS/claims feed — the data-flywheel input. */
@RestController
@RequestMapping("/api/underwriting/outcomes")
public class OutcomeController {

    private final OutcomeService outcomeService;

    public OutcomeController(OutcomeService outcomeService) {
        this.outcomeService = outcomeService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RealizedOutcomeEntity> record(@RequestBody OutcomeRequest request) {
        return ResponseEntity.accepted().body(outcomeService.record(request));
    }
}
