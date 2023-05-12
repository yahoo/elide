/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.request.route;

import lombok.Builder;
import lombok.Data;

/**
 * Represents a route with the API Version.
 */
@Data
@Builder
public class Route {
    /**
     * The baseUrl of the resolved route.
     */
    private final String baseUrl;

    /**
     * The path of the resolved route.
     */
    private final String path;

    /**
     * The API Version of the resolved route.
     */
    private final String apiVersion;
}
