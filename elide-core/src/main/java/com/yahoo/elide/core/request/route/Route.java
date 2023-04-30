/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.request.route;

import lombok.Builder;
import lombok.Data;

/**
 * Represents a route with the api version.
 */
@Data
@Builder
public class Route {
    private final String baseUrl;
    private final String path;
    private final String apiVersion;
}
