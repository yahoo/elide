/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.spring.config;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Extra controller properties for the JSON-API endpoint.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class JsonApiControllerProperties extends ControllerProperties {

    /**
     * Turns on/off JSON-API links in the API.
     */
    boolean enableLinks = false;
}
