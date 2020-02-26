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
    private int maxRunTime = 60;

    /**
     * Default number of hosts running Elide.
     */
    private int numberOfHosts = 1;

}
