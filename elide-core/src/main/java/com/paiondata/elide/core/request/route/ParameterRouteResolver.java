/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.request.route;

import static com.paiondata.elide.core.dictionary.EntityDictionary.NO_VERSION;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Resolves a route using a parameter.
 */
public class ParameterRouteResolver implements RouteResolver {

    private final String apiVersionParameterName;
    private final ApiVersionValidator apiVersionValidator;

    public ParameterRouteResolver(String apiVersionParameterName, ApiVersionValidator apiVersionValidator) {
        this.apiVersionParameterName = apiVersionParameterName;
        this.apiVersionValidator = apiVersionValidator;
    }

    @Override
    public Route resolve(String mediaType, String baseUrl, String path,
            Map<String, List<String>> headers, Map<String, List<String>> parameters) {
        Map<String, List<String>> result = parameters;

        if (parameters != null && parameters.get(apiVersionParameterName) != null) {
            String apiVersion = parameters.get(apiVersionParameterName).get(0);
            if (this.apiVersionValidator.isValidApiVersion(apiVersion)) {
                result = new LinkedHashMap<>(parameters);
                result.remove(apiVersionParameterName);
                return Route.builder().apiVersion(apiVersion).baseUrl(baseUrl).path(path).headers(headers)
                        .parameters(result).build();
            }
        }
        return Route.builder().apiVersion(NO_VERSION).baseUrl(baseUrl).path(path).headers(headers).parameters(result)
                .build();
    }
}
