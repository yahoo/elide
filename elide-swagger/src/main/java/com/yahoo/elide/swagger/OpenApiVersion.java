/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.swagger;

/**
 * The OpenAPI version.
 */
public enum OpenApiVersion {
    OPENAPI_3_0("3.0"),
    OPENAPI_3_1("3.1");

    private final String version;

    OpenApiVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return this.version;
    }

    public static OpenApiVersion from(String version) {
        if (version.startsWith("3.1")) {
            return OPENAPI_3_1;
        } else if (version.startsWith("3.0")) {
            return OPENAPI_3_0;
        }
        throw new IllegalArgumentException("Invalid OpenAPI version. Only versions 3.0 and 3.1 are supported.");
    }
}
