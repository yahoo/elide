/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.request.route;

import static com.paiondata.elide.core.dictionary.EntityDictionary.NO_VERSION;

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
        String baseRoute = baseUrl == null ? "" : baseUrl;
        String route = path;
        String apiVersion = NO_VERSION;

        String apiVersionString = "";
        int versionStart = -1;
        int versionEnd = -1;
        int pathStart = -1;
        int pathEnd = -1;

        int find = path.indexOf('/', 0);
        if (find != -1) {
            if (find == 0) {
                // "/.."
                versionStart = 1;
                int findEnd = path.indexOf('/', 1);
                if (findEnd != -1) {
                    // "/v2/.."
                    versionEnd = findEnd;
                    pathStart = findEnd;
                    pathEnd = path.length();
                } else {
                    // "/v2"
                    versionEnd = path.length();
                }
            } else {
                // "v2/.."
                versionStart = 0;
                versionEnd = find;
                pathStart = find;
                pathEnd = path.length();
            }
        } else {
            // "v2"
            versionStart = 0;
            versionEnd = path.length();
        }

        if (versionStart != -1 && versionEnd != -1) {
            apiVersion = path.substring(versionStart, versionEnd);
            apiVersionString = apiVersion;
        }

        if (!apiVersion.isEmpty() && !this.versionPrefix.isEmpty()) {
            if (apiVersion.startsWith(this.versionPrefix)) {
                apiVersion = apiVersion.substring(this.versionPrefix.length());
            }
        }

        if (!apiVersion.isEmpty()) {
            if (!apiVersionValidator.isValidApiVersion(apiVersion)) { // sanity check version
                apiVersion = NO_VERSION;
                pathStart = 0;
                pathEnd = path.length();
                apiVersionString = "";
            }
        }

        if (pathStart != -1 && pathEnd != -1) {
            route = path.substring(pathStart, pathEnd);
        } else {
            route = "";
        }

        if (route.length() > 0 && route.charAt(0) == '/') {
            route = route.substring(1);
        }

        if (baseRoute.length() > 0 && baseRoute.charAt(baseRoute.length() - 1) == '/') {
            baseRoute = baseRoute + apiVersionString;
        } else {
            baseRoute = baseRoute + "/" + apiVersionString;
        }

        return Route.builder().apiVersion(apiVersion).baseUrl(baseRoute).path(route).headers(headers)
                .parameters(parameters).build();
    }
}
