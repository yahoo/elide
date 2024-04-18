/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.spring.config;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Extra controller properties for the JSON-API endpoint.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class JsonApiControllerProperties extends ControllerProperties {

    @Data
    public static class Links {
        /**
         * Turns on/off JSON-API links in the API.
         */
        private boolean enabled = false;
    }

    private Links links = new Links();
}
