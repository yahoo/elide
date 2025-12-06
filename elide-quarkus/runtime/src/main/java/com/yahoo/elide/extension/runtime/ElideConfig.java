/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.extension.runtime;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(prefix = "", name = "elide", phase = ConfigPhase.BUILD_TIME)
public class ElideConfig {

    public static final String JSONAPI_PATH = "/jsonapi";
    public static final String GRAPHQL_PATH = "/graphql";
    public static final String APIDOCS_PATH = "/apiDocs";

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
    @ConfigItem(name = "json-api.path", defaultValue = JSONAPI_PATH)
    public String jsonApiPath;

    /**
     * The base URL path prefix for Elide GraphQL service endpoints.
     * This is appended to the basePath.
     */
    @ConfigItem(name = "graphql.path", defaultValue = GRAPHQL_PATH)
    public String graphqlPath;


    /**
     * The base URL path prefix for the Elide Swagger document.
     * This is appended to the basePath.
     */
    @ConfigItem(name = "api-docs.path", defaultValue = APIDOCS_PATH)
    public String apiDocsPath;
}
