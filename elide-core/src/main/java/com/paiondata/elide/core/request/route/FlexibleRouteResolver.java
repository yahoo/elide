/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.request.route;

import java.util.List;
import java.util.Map;

/**
 * The flexible route resolver allows multiple ways to derive the requested version.
 *
 * <li>Header: ApiVersion, Accept-Version
 * <li>Path: Prefixed with 'v'
 * <li>Query Parameter: For 'v'
 * <li>Media Type Profile: After the last /
 */
public class FlexibleRouteResolver implements RouteResolver {

    private final DelegatingRouteResolver routeResolver;

    public FlexibleRouteResolver(ApiVersionValidator apiVersionValidator, UriPrefixSupplier uriPrefixSupplier) {
        this.routeResolver = new DelegatingRouteResolver(new HeaderRouteResolver("Accept-Version", "ApiVersion"),
                new PathRouteResolver("v", apiVersionValidator), new ParameterRouteResolver("v", apiVersionValidator),
                new MediaTypeProfileRouteResolver("v", apiVersionValidator, uriPrefixSupplier));
    }

    @Override
    public Route resolve(String mediaType, String baseUrl, String path,
            Map<String, List<String>> headers, Map<String, List<String>> parameters) {
        return this.routeResolver.resolve(mediaType, baseUrl, path, headers, parameters);
    }
}
