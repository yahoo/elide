/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.request.route;

import static com.yahoo.elide.core.dictionary.EntityDictionary.NO_VERSION;

import java.util.List;
import java.util.Map;

/**
 * Resolves a route using the path.
 */
public class PathRouteResolver implements RouteResolver {

    private final String versionPrefix;
    private final ApiVersionValidator apiVersionValidator;

    public PathRouteResolver(String versionPrefix, ApiVersionValidator apiVersionValidator) {
        this.versionPrefix = versionPrefix;
        this.apiVersionValidator = apiVersionValidator;
    }

    @Override
    public Route resolve(String mediaType, String baseUrl, String path,
            Map<String, List<String>> headers, Map<String, List<String>> parameters) {
        String baseRoute = baseUrl;
        String route = path;
        String apiVersion = NO_VERSION;
        int length = 0;
        int find = path.indexOf('/', 0);
        if (find != -1) {
            if (find == 0) {
                route = path.substring(1); // trim leading /
                int endIndex = route.indexOf('/', 1);
                if (endIndex == -1) {
                    apiVersion = NO_VERSION;
                } else {
                    apiVersion = route.substring(0, endIndex);
                }
            } else {
                apiVersion = route.substring(0, find);
            }
            if (!apiVersion.isEmpty() && !this.versionPrefix.isEmpty()) {
                if (apiVersion.startsWith(this.versionPrefix)) {
                    apiVersion = apiVersion.substring(1);
                    length += this.versionPrefix.length();
                } else {
                    apiVersion = NO_VERSION;
                }
            }
            if (!apiVersion.isEmpty()) {
                if (!apiVersionValidator.isValidApiVersion(apiVersion)) { // sanity check version
                    apiVersion = NO_VERSION;
                } else {
                    route = route.substring(length + apiVersion.length() + 1);
                    baseRoute = baseRoute + "/" + this.versionPrefix + apiVersion;
                }
            }
        }
        return Route.builder().apiVersion(apiVersion).baseUrl(baseRoute).path(route).build();
    }
}
