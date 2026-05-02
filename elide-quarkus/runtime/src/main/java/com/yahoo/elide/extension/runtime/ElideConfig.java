/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.extension.runtime;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
@ConfigMapping(prefix = "elide")
public interface ElideConfig {
    public static final String JSONAPI_PATH = "/jsonapi";
    public static final String GRAPHQL_PATH = "/graphql";
    public static final String APIDOCS_PATH = "/apiDocs";

    interface JsonApiConfig {
        /**
         * The base URL path prefix for Elide JSON-API service endpoints.
         * This is appended to the basePath.
         */
        @WithDefault(JSONAPI_PATH)
        String path();
    }

    interface GraphqlConfig {
        /**
         * The base URL path prefix for Elide GraphQL service endpoints.
         * This is appended to the basePath.
         */
        @WithDefault(GRAPHQL_PATH)
        String path();
    }

    interface ApiDocsConfig {
        /**
         * The base URL path prefix for the Elide Swagger document.
         * This is appended to the basePath.
         */
        @WithDefault(APIDOCS_PATH)
        String path();
    }

    /**
     * JSON-API configuration.
     *
     * @return the configuration
     */
    JsonApiConfig jsonApi();

    /**
     * GraphQL configuration.
     *
     * @return the configuration
     */
    GraphqlConfig graphql();

    /**
     * API Docs configuration.
     *
     * @return the configuration
     */
    ApiDocsConfig apiDocs();

    /**
     * Default page size if client doesn't request any.
     */
    @WithDefault("100")
    int defaultPageSize();

    /**
     * Maximum page size that can be requested by a client.
     */
    @WithDefault("10000")
    int defaultMaxPageSize();

    /**
     * Turns on verbose errors in HTTP responses.
     */
    @WithDefault("true")
    boolean verboseErrors();
}
