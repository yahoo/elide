/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.standalone.config;

import com.yahoo.elide.async.service.ResultStorageEngine;

import lombok.Data;

/**
 * Extra properties for setting up async query download support.
 */
@Data
public class AsyncDownloadProperties {

    /**
     * API root path specification for the download endpoint.
     */
    private String pathSpec = "/download/*";

    /**
     * Which implementation of ResultStorageEngine to use.
     */
    private ResultStorageEngine resultStorageEngine = null;

    /**
     * Whether or not the controller is enabled.
     */
    private boolean enabled = false;

}
