/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.request.route;

import static com.yahoo.elide.core.dictionary.EntityDictionary.NO_VERSION;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Resolves a route using a media type parameter.
 */
public class MediaTypeParameterRouteResolver implements RouteResolver {

    private final Function<String, String> resolver;

    public MediaTypeParameterRouteResolver(Function<String, String> resolver) {
        this.resolver = resolver;
    }

    @Override
    public Route resolve(String mediaType, String baseUrl, String path,
            Map<String, List<String>> headers, Map<String, List<String>> parameters) {

        List<String> acceptHeader = headers.get("accept");
        if (acceptHeader != null) {
            for (String accept : acceptHeader) {
                String apiVersion = fromHeader(accept, mediaType);
                if (apiVersion != null) {
                    return Route.builder().apiVersion(apiVersion).baseUrl(baseUrl).path(path).headers(headers)
                            .parameters(parameters).build();
                }
            }
        }
        return Route.builder().apiVersion(NO_VERSION).baseUrl(baseUrl).path(path).headers(headers)
                .parameters(parameters).build();
    }

    protected String fromHeader(String header, String mediaType) {
        if (header.startsWith(mediaType)) {
            String[] parameters = header.split(";");
            for (int x = 1; x < parameters.length; x++) {
                String parameter = parameters[x].trim();
                String apiVersion = fromMediaTypeParameter(parameter);
                if (apiVersion != null && !apiVersion.isBlank()) {
                    return apiVersion;
                }
            }
        }
        return null;
    }

    protected String fromMediaTypeParameter(String parameter) {
        return resolver.apply(parameter);
    }
}
