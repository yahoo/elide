/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.standalone.config;

import com.yahoo.elide.async.service.dao.AsyncApiDao;
import com.yahoo.elide.async.service.storageengine.ResultStorageEngine;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * interface for configuring the Async configuration of standalone application.
 */
public interface ElideStandaloneAsyncSettings {
    /* Elide Async settings */

    /**
     * Enable the support for Async querying feature. If false, the async feature will be disabled.
     *
     * @return Default: False
     */
    default boolean enabled() {
        return false;
    }

    /**
     * Enable the support for cleaning up Async query history. If false, the async cleanup feature will be disabled.
     *
     * @return Default: False
     */
    default boolean enableCleanup() {
        return false;
    }

    /**
     * Thread Size for Async queries to run in parallel.
     *
     * @return Default: 5
     */
    default Integer getThreadSize() {
        return 5;
    }

    /**
     * Maximum Query Run time for Async Queries to mark as TIMEDOUT.
     *
     * @return Default: 3600s
     */
    default Duration getQueryMaxRunTime() {
        return Duration.ofSeconds(3600L);
    }

    /**
     * Maximum permissible AsyncAfterSeconds value.
     * The Async requests can be configured to execute synchronously before switching to asynchronous mode.
     *
     * @return Default: 10s
     */
    default Duration getMaxAsyncAfter() {
        return Duration.ofSeconds(10L);
    }

    /**
     * Number of days history to retain for async query executions and results.
     *
     * @return Default: 7d
     */
    default Duration getQueryRetentionDuration() {
        return Duration.ofDays(7L);
    }

    /**
     * Polling interval to identify async queries that should be canceled.
     *
     * @return Default: 300s
     */
    default Duration getQueryCancellationCheckInterval() {
        return Duration.ofSeconds(300L);
    }

    /**
     * Implementation of AsyncApiDao to use.
     *
     * @return AsyncApiDao type object.
     */
    default AsyncApiDao getAsyncApiDao() {
        return null;
    }

    /**
     * Implementation of ResultStorageEngine to use.
     *
     * @return ResultStorageEngine type object.
     */
    default ResultStorageEngine getResultStorageEngine() {
        return null;
    }

    /**
     * API root path specification for the export endpoint.
     *
     * @return Default: /export
     */
    default String getExportApiPathSpec() {
        return "/export/*";
    }

    /**
     * Enable the Export endpoint. If false, the endpoint and export support will be disabled.
     *
     * @return Default: False
     */
    default boolean enableExport() {
        return false;
    }

    /**
     * Enable the addition of extensions to Export attachments.
     * If false, the attachments will be downloaded without extensions.
     *
     * @return Default: False
     */
    default boolean appendFileExtension() {
        return false;
    }

    /**
     * Generating Header when exporting in CSV format. Set to false to skip.
     *
     * @return Default: True
     */
    default boolean csvWriteHeader() {
        return true;
    }

    /**
     * Storage engine destination.
     *
     * @return Default: /tmp
     */
    default String getStorageDestination() {
        return "/tmp";
    }

    /**
     * Export async response timeout.
     *
     * @return Default: 30s
     */
    default Duration getExportAsyncResponseTimeout() {
        return Duration.ofSeconds(30L);
    }

    /**
     * Executor for running export resource asynchronously.
     *
     * @return Default: null
     */
    default ExecutorService getExportAsyncResponseExecutor() {
        return enableExport() ? Executors.newFixedThreadPool(getThreadSize() == null ? 6 : getThreadSize()) : null;
    }
}
