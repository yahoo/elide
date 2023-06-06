/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.request.route;

import lombok.Builder;
import lombok.Data;

import java.util.Collections;
import java.util.List;
import java.util.Map;

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

    /**
     * The parameters of the resolved route.
     */
    private final Map<String, List<String>> parameters;

    /**
     * The headers of the resolved route.
     */
    private final Map<String, List<String>> headers;

    public static class RouteBuilder {
        private String baseUrl = "";
        private String path = "";
        private String apiVersion = "";
        private Map<String, List<String>> parameters = Collections.emptyMap();
        private Map<String, List<String>> headers = Collections.emptyMap();
    }
}
