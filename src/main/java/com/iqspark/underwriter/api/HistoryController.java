package com.iqspark.underwriter.api;

import com.iqspark.underwriter.domain.model.Submission;
import com.iqspark.underwriter.history.AreaRiskService;
import com.iqspark.underwriter.history.HistoricalPolicyRepository;
import com.iqspark.underwriter.history.SimilarityEngine;
import com.iqspark.underwriter.history.model.AreaRiskStat;
import com.iqspark.underwriter.history.model.LearnedAssessment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Read access to the learned book: stats, per-area risk, and a comparables preview. */
@RestController
@RequestMapping("/api/underwriting/history")
public class HistoryController {

    private final HistoricalPolicyRepository repository;
    private final AreaRiskService areaRiskService;
    private final SimilarityEngine similarityEngine;

    public HistoryController(HistoricalPolicyRepository repository,
                            AreaRiskService areaRiskService,
                            SimilarityEngine similarityEngine) {
        this.repository = repository;
        this.areaRiskService = areaRiskService;
        this.similarityEngine = similarityEngine;
    }

    @GetMapping("/stats")
    public HistoricalPolicyRepository.BookStats stats() {
        return repository.stats();
    }

    @GetMapping("/areas/{city}")
    public AreaRiskStat area(@PathVariable String city) {
        return areaRiskService.forCity(city);
    }

    @PostMapping("/comparables")
    public LearnedAssessment comparables(@RequestBody Submission submission) {
        return similarityEngine.assess(submission);
    }
}
