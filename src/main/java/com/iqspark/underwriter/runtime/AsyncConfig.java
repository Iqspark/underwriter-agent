package com.iqspark.underwriter.runtime;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Enables the lean async runtime: @Async event handling + the @Scheduled outbox relay. */
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {
}
