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
public class AsyncProperties extends ControllerProperties {

    /**
     * Default thread pool size.
     */
    private int threadPoolSize = 6;

    /**
     * Default max query run time.
     */
    private int maxRunTimeMinutes = 60;

    /**
     * Whether or not the cleanup is enabled.
     */
    private boolean cleanupEnabled = false;

    /**
     * Default retention of async query and results.
     */
    private int queryCleanupDays = 7;

    /**
     * Whether or not to use the default implementation of AsyncQueryDAO.
     * If false, the user will provide custom implementation of AsyncQueryDAO.
     */
    private boolean defaultAsyncQueryDAO = true;

}
