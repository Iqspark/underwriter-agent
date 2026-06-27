package com.iqspark.underwriter.extraction;

import com.iqspark.underwriter.domain.model.Submission;

/** Turns a raw document (text today; multimodal in the target state) into a {@link Submission}. */
public interface DocumentExtractor {

    /** Extract a submission from raw text. Never invents data — absent keys are left null. */
    Submission extract(String rawText);
}
