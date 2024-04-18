/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.request.route;

import java.util.List;
import java.util.Map;

/**
 * Strategy for resolving a route.
 */
public interface RouteResolver {
    /**
     * Resolves a route.
     *
     * @param mediaType the acceptable media type for processing
     * @param baseUrl the base url
     * @param path the path
     * @param headers the request headers
     * @param parameters the request parameters
     * @return the route information
     */
    Route resolve(String mediaType, String baseUrl, String path,
            Map<String, List<String>> headers, Map<String, List<String>> parameters);
}
