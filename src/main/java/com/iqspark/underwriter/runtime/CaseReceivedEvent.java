package com.iqspark.underwriter.runtime;

/** Published after a case is persisted; handled (async, after commit) to process the case. */
public record CaseReceivedEvent(String caseId) {}
