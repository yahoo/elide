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
     * The OpenAPI document version to generate. Either 3.0 or 3.1.
     */
    private String version = "3.0";

    /**
     * Information about the API.
     */
    private Info info = new Info();

    @Data
    public static class Info {
        /**
         * The title of the API.
         */
        private String title = "Elide Service";

        /**
         * The version of the API.
         */
        private String version = "";

        /**
         * The description of the API.
         */
        private String description = "";
    }
}
