/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.spring.config;

import lombok.Data;

/**
 * Extra properties for setting up async query support.
 */
@Data
public class AsyncProperties {

    /**
     * Default thread pool size.
     */
    private int threadPoolSize = 5;

    /**
     * Default max query run time.
     */
    private int maxRunTimeSeconds = 3600;

    /**
     * Default maximum permissible AsyncAfterSeconds value.
     * The Async requests can be configured to execute synchronously before switching to asynchronous mode.
     */
    private int maxAsyncAfterSeconds = 10;

    /**
     * Whether or not the cleanup is enabled.
     */
    private boolean cleanupEnabled = false;

    /**
     * Default retention of async query and results.
     */
    private int queryCleanupDays = 7;

    /**
     * Polling interval to identify async queries that should be canceled.
     */
    private int queryCancellationIntervalSeconds = 300;

    /**
     * Whether or not to use the default implementation of AsyncAPIDAO.
     * If false, the user will provide custom implementation of AsyncAPIDAO.
     */
    private boolean defaultAsyncAPIDAO = true;

    /**
     * Whether or not the async feature is enabled.
     */
    private boolean enabled = false;

    /**
     * Settings for the export controller.
     */
    private ExportControllerProperties export;
}
