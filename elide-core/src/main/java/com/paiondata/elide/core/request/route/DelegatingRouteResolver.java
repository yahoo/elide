/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.request.route;

import static com.paiondata.elide.core.dictionary.EntityDictionary.NO_VERSION;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * A route resolver that delegates to other implementations.
 */
public class DelegatingRouteResolver implements RouteResolver {

    private final List<RouteResolver> routeResolvers;

    public DelegatingRouteResolver(RouteResolver... routeResolver) {
        this(Arrays.asList(routeResolver));
    }

    public DelegatingRouteResolver(List<RouteResolver> routeResolvers) {
        this.routeResolvers = routeResolvers;
    }

    @Override
    public Route resolve(String mediaType, String baseUrl, String path,
            Map<String, List<String>> headers, Map<String, List<String>> parameters) {
        for (RouteResolver routeResolver : this.routeResolvers) {
            Route route = routeResolver.resolve(mediaType, baseUrl, path, headers, parameters);
            if (!NO_VERSION.equals(route.getApiVersion())) {
                return route;
            }
        }
        return Route.builder().apiVersion(NO_VERSION).baseUrl(baseUrl).path(path).headers(headers)
                .parameters(parameters).build();
    }
}
