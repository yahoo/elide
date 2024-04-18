/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.spring.config;

import lombok.Data;

/**
 * Settings for a Spring REST controller.
 */
@Data
public class ControllerProperties {

    /**
     * Whether or not the controller is enabled.
     */
    private boolean enabled = false;

    /**
     * The URL path prefix for the controller.
     */
    private String path = "/";
}
