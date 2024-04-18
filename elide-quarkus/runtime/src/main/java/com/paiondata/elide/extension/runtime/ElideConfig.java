/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.extension.runtime;

import static com.paiondata.elide.extension.runtime.ElideResourceBuilder.GRAPHQL_BASE;
import static com.paiondata.elide.extension.runtime.ElideResourceBuilder.JSONAPI_BASE;
import static com.paiondata.elide.extension.runtime.ElideResourceBuilder.SWAGGER_BASE;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "elide", phase = ConfigPhase.BUILD_TIME)
public class ElideConfig {

    /**
     * Default page size if client doesn't request any.
     */
    @ConfigItem(defaultValue = "100")
    public int defaultPageSize;

    /**
     * Maximum page size that can be requested by a client.
     */
    @ConfigItem(defaultValue = "10000")
    public int defaultMaxPageSize;

    /**
     * Turns on verbose errors in HTTP responses.
     */
    @ConfigItem(defaultValue = "true")
    public boolean verboseErrors;

    /**
     * The base URL path prefix for Elide JSON-API service endpoints.
     * This is appended to the basePath.
     */
    @ConfigItem(defaultValue = JSONAPI_BASE)
    public String baseJsonapi;

    /**
     * The base URL path prefix for Elide GraphQL service endpoints.
     * This is appended to the basePath.
     */
    @ConfigItem(defaultValue = GRAPHQL_BASE)
    public String baseGraphql;

    /**
     * The base URL path prefix for the Elide Swagger document.
     * This is appended to the basePath.
     */
    @ConfigItem(defaultValue = SWAGGER_BASE)
    public String baseSwagger;
}
