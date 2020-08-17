/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.standalone.config;

import com.yahoo.elide.async.service.AsyncQueryDAO;

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
     * Whether or not the cleanup is enabled.
     */
    private boolean cleanupEnabled = false;

    /**
     * Default retention of async query and results.
     */
    private int queryCleanupDays = 7;

    /**
     * Which implementation of AsyncQueryDAO to use.
     */
    private AsyncQueryDAO asyncQueryDAO = null;

    /**
     * Default time interval for cancelling async query transactions
     * that are in cancelled status or running beyond max runtime.
     */
    private int queryCancellationIntervalSeconds = 300;

    /**
     * Whether or not the async is enabled.
     */
    private boolean enabled = false;

    /**
     * Settings for the Download API.
     */
    private AsyncDownloadProperties download = new AsyncDownloadProperties();

}
