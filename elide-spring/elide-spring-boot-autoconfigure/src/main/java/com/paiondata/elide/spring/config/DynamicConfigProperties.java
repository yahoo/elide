/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.spring.config;

import lombok.Data;

/**
 * Extra properties for setting up dynamic model config.
 */
@Data
public class DynamicConfigProperties {
    @Data
    public static class ConfigApi {
        /**
         * Enable support for reading and manipulating HJSON configuration through Elide models.
         */
        private boolean enabled = false;
    }

    private ConfigApi configApi = new ConfigApi();

    /**
     * Whether or not dynamic model config is enabled.
     */
    private boolean enabled = false;

    /**
     * The path where the config hjsons are stored.
     */
    private String path = "/";
}
