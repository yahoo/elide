/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.standalone.config;

import lombok.Data;

/**
 * Extra properties for setting up async query download support.
 */
@Data
public class AsyncDownloadProperties {

    /**
     * The URL path prefix for the controller.
     */
    private String path = "/download";

    /**
     * Whether or not to use the default implementation of ResultStorageEngine.
     * If false, the user will provide custom implementation of ResultStorageEngine.
     */
    private boolean defaultResultStorageEngine = true;

    /**
     * Whether or not the controller is enabled.
     */
    private boolean enabled = false;

}
