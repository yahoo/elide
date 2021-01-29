/*
 * Copyright 2020, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.standalone.config;

import com.yahoo.elide.async.service.dao.AsyncAPIDAO;
import com.yahoo.elide.async.service.storageengine.ResultStorageEngine;

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
     * @return Default: 3600
     */
    default Integer getMaxRunTimeSeconds() {
        return 3600;
    }

    /**
     * Maximum permissible AsyncAfterSeconds value.
     * The Async requests can be configured to execute synchronously before switching to asynchronous mode.
     *
     * @return Default: 10
     */
    default Integer getMaxAsyncAfterSeconds() {
        return 10;
    }

    /**
     * Number of days history to retain for async query executions and results.
     *
     * @return Default: 7
     */
    default Integer getQueryCleanupDays() {
        return 7;
    }

    /**
     * Polling interval to identify async queries that should be canceled.
     *
     * @return Default: 300
     */
    default Integer getQueryCancelCheckIntervalSeconds() {
        return 300;
    }

    /**
     * Implementation of AsyncAPIDAO to use.
     *
     * @return AsyncAPIDAO type object.
     */
    default AsyncAPIDAO getAPIDAO() {
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
     * API root path specification for the export endpoint
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
     * Skip generating Header when exporting in CSV format.
     *
     * @return Default: False
     */
    default boolean skipCSVHeader() {
        return false;
    }
}
