/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.request.route;

import com.paiondata.elide.core.dictionary.EntityDictionary;

import lombok.Builder;
import lombok.Data;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Represents a route which contains the base url, path, headers, parameters and
 * api version of the request.
 * <p>
 * Use the static factory {@link #builder()} method to prepare an instance.
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

    /**
     * Returns a builder with the current values.
     *
     * @return the builder to mutate
     */
    public RouteBuilder mutate() {
        return builder().baseUrl(this.baseUrl).path(this.path).apiVersion(this.apiVersion).parameters(this.parameters)
                .headers(this.headers);
    }

    /**
     * A mutable builder for creating a {@link Route}.
     */
    public static class RouteBuilder {
        private String baseUrl = "";
        private String path = "";
        private String apiVersion = EntityDictionary.NO_VERSION;
        private Map<String, List<String>> parameters = Collections.emptyMap();
        private Map<String, List<String>> headers = Collections.emptyMap();
    }
}
