/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.spring.config;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Extra controller properties for the OpenAPI document endpoint.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ApiDocsControllerProperties extends ControllerProperties {
    /**
     * The OpenAPI version to generate.
     */
    private String version = "3.0";

    private Info info = new Info();

    @Data
    public static class Info {
        /**
         * OpenAPI needs a title for the service.
         */
        private String title = "Elide Service";

        /**
         * The API version.
         */
        private String version = "";

        /**
         * The description.
         */
        private String description = "";
    }
}
