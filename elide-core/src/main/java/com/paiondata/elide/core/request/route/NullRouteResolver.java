/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.request.route;

import com.paiondata.elide.core.dictionary.EntityDictionary;

import java.util.List;
import java.util.Map;

/**
 * The no-op route resolver.
 */
public class NullRouteResolver implements RouteResolver {

    @Override
    public Route resolve(String mediaType, String baseUrl, String path,
            Map<String, List<String>> headers, Map<String, List<String>> parameters) {
        return Route.builder().apiVersion(EntityDictionary.NO_VERSION).baseUrl(baseUrl).path(path)
                .headers(headers).parameters(parameters).build();
    }
}
