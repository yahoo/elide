/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.request.route;

import static com.paiondata.elide.core.dictionary.EntityDictionary.NO_VERSION;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Resolves a route using a header.
 */
public class HeaderRouteResolver implements RouteResolver {

    private final String[] apiVersionHeaderNames;

    public HeaderRouteResolver(String... apiVersionHeaderNames) {
        this.apiVersionHeaderNames = apiVersionHeaderNames;
        for (int x = 0; x < apiVersionHeaderNames.length; x++) {
            this.apiVersionHeaderNames[x] = this.apiVersionHeaderNames[x].toLowerCase(Locale.ENGLISH);
        }
    }

    @Override
    public Route resolve(String mediaType, String baseUrl, String path,
            Map<String, List<String>> headers, Map<String, List<String>> parameters) {
        for (String apiVersionHeaderName : this.apiVersionHeaderNames) {
            if (headers != null && headers.get(apiVersionHeaderName) != null) {
                String apiVersion = headers.get(apiVersionHeaderName).get(0);
                if (apiVersion != null) {
                    return Route.builder().apiVersion(apiVersion).baseUrl(baseUrl).path(path).headers(headers)
                            .parameters(parameters).build();
                }
            }
        }
        return Route.builder().apiVersion(NO_VERSION).baseUrl(baseUrl).path(path).headers(headers)
                .parameters(parameters).build();
    }
}
