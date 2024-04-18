/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.request.route;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Collections;

/**
 * Tests for path route resolver.
 */
class PathRouteResolverTest {

    enum Input {
        VERSION_LEADING_SLASH_BASE("https://elide.io/", "v", "/v1/post", "1", "https://elide.io/v1", "post"),
        VERSION_LEADING_SLASH_NULL_BASE(null, "v", "/v1/post", "1", "/v1", "post"),
        VERSION_LEADING_SLASH("", "v", "/v1/post", "1", "/v1", "post"),
        VERSION_LEADING_SLASH_NO_PATH("", "v", "/v1", "1", "/v1", ""),
        VERSION_WITHOUT_LEADING_SLASH("", "v", "/v1/post", "1", "/v1", "post"),
        VERSION_LEADING_SLASH_NO_PREFIX("", "", "1/post", "1", "/1", "post"),
        VERSION_WIHOUT_LEADING_SLASH_NO_PREFIX("", "", "/1/post", "1", "/1", "post"),

        NO_VERSION_LEADING_SLASH_BASE("https://elide.io/", "v", "/post", "", "https://elide.io/", "post"),
        NO_VERSION_LEADING_SLASH_NULL_BASE(null, "v", "/post", "", "/", "post"),
        NO_VERSION_LEADING_SLASH("", "v", "/post", "", "/", "post"),
        NO_VERSION_LEADING_SLASH_NO_PATH("", "v", "/", "", "/", ""),
        NO_VERSION_WITHOUT_LEADING_SLASH("", "v", "post", "", "/", "post"),
        NO_VERSION_LEADING_SLASH_NO_PREFIX("", "", "/post", "", "/", "post"),
        NO_VERSION_WIHOUT_LEADING_SLASH_NO_PREFIX("", "", "post", "", "/", "post"),
        NO_VERSION_LEADING_SLASH_NO_PREFIX_NEST("", "", "/post/1/author", "", "/", "post/1/author"),
        NO_VERSION_WIHOUT_LEADING_SLASH_NO_PREFIX_NEST("", "", "post/1/author", "", "/", "post/1/author");

        String baseUrl;
        String versionPrefix;
        String path;
        String apiVersion;
        String baseRoute;
        String route;

        Input(String baseUrl, String versionPrefix, String path, String apiVersion, String baseRoute, String route) {
            this.baseUrl = baseUrl;
            this.versionPrefix = versionPrefix;
            this.path = path;
            this.apiVersion = apiVersion;
            this.baseRoute = baseRoute;
            this.route = route;
        }
    }

    @ParameterizedTest
    @EnumSource(Input.class)
    void testInputs(Input input) {
        PathRouteResolver routeResolver = new PathRouteResolver(input.versionPrefix, new BasicApiVersionValidator());
        Route route = routeResolver.resolve("", input.baseUrl, input.path, Collections.emptyMap(), Collections.emptyMap());
        assertEquals(input.apiVersion, route.getApiVersion());
        assertEquals(input.route, route.getPath());
        assertEquals(input.baseRoute, route.getBaseUrl());
    }
}
