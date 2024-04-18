/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.spring.config;

import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.boot.convert.DurationUnit;

import lombok.Data;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

/**
 * Extra properties for setting up async query support.
 */
@Data
public class AsyncProperties {
    @Data
    public static class Cleanup {
        /**
         * Whether or not the cleanup is enabled.
         */
        private boolean enabled = false;

        /**
         * Retention period of async query and results before being cleaned up.
         */
        @DurationUnit(ChronoUnit.DAYS)
        private Duration queryRetentionDuration = Duration.ofDays(7L);

        /**
         * Polling interval to identify async queries that should be canceled.
         */
        @DurationUnit(ChronoUnit.SECONDS)
        private Duration queryCancellationCheckInterval = Duration.ofSeconds(300L);

        /**
         * Maximum query run time.
         */
        @DurationUnit(ChronoUnit.SECONDS)
        private Duration queryMaxRunTime = Duration.ofSeconds(3600L);
    }

    private Cleanup cleanup = new Cleanup();

    /**
     * Whether or not the async feature is enabled.
     */
    private boolean enabled = false;

    /**
     * Default thread pool size.
     */
    private int threadPoolSize = 5;

    /**
     * Default maximum permissible time to wait synchronously for the query to
     * complete before switching to asynchronous mode.
     */
    @DurationUnit(ChronoUnit.SECONDS)
    private Duration maxAsyncAfter = Duration.ofSeconds(10L);

    /**
     * Settings for the export controller.
     */
    @NestedConfigurationProperty
    private ExportControllerProperties export;
}
