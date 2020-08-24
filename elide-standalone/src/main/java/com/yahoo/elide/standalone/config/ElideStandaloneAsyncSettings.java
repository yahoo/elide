/*
 * Copyright 2017, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.standalone.config;

import com.yahoo.elide.async.service.AsyncQueryDAO;
import com.yahoo.elide.async.service.ResultStorageEngine;

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
     * @return Default: 60
     */
    default Integer getMaxRunTimeSeconds() {
        return 60;
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
     * @return Default: 10
     */
    default Integer getQueryCancelCheckIntervalSeconds() {
        return 10;
    }

    /**
     * Implementation of AsyncQueryDAO to use.
     *
     * @return AsyncQueryDAO type object.
     */
    default AsyncQueryDAO getQueryDAO() {
        return null;
    }

    /**
     * Enable the support for Download API. If false, the download API feature will be disabled.
     *
     * @return Default: False
     */
    default boolean enableDownload() {
        return false;
    }

    /**
     * API root path specification for Async Download. Namely, this is the mount point of your API.
     * By default it will look something like:
     *   <strong>yourcompany.com/download</strong>
     *
     * @return Default: /download/*
     */
    default String getDownloadApiPathSpec() {
        return "/download/*";
    }

    /**
     * Implementation of ResultStorageEngine to use.
     *
     * @return ResultStorageEngine type object.
     */
    default ResultStorageEngine getResultStorageEngine() {
        return null;
    }
}
