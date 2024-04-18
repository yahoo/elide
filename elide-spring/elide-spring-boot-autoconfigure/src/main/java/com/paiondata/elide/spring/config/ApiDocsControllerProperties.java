/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.spring.config;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Extra controller properties for the OpenAPI document endpoint.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ApiDocsControllerProperties extends ControllerProperties {
    /**
     * The OpenAPI Specification Version.
     */
    public enum Version {
        OPENAPI_3_0("3.0"),
        OPENAPI_3_1("3.1");

        private final String value;

        Version(String value) {
            this.value = value;
        }

        public String getValue() {
            return this.value;
        }

        public static Version from(String version) {
            if (version.startsWith(OPENAPI_3_1.getValue())) {
                return OPENAPI_3_1;
            } else if (version.startsWith(OPENAPI_3_0.getValue())) {
                return OPENAPI_3_0;
            }
            throw new IllegalArgumentException("Invalid OpenAPI version. Only versions 3.0 and 3.1 are supported.");
        }
    }

    /**
     * The OpenAPI Specification Version to generate. Either openapi_3_0 or openapi_3_1.
     */
    private Version version = Version.OPENAPI_3_0;
}
