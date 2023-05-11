/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.spring.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import lombok.Data;

/**
 * Configuration settings for Elide.
 */
@Data
@ConfigurationProperties(prefix = "elide")
public class ElideConfigProperties {

    /**
     * Settings for the JSON-API controller.
     */
    @NestedConfigurationProperty
    private JsonApiControllerProperties jsonApi = new JsonApiControllerProperties();

    /**
     * Settings for the GraphQL controller.
     */
    @NestedConfigurationProperty
    private GraphQLControllerProperties graphql = new GraphQLControllerProperties();

    /**
     * Settings for the OpenAPI document controller.
     */
    @NestedConfigurationProperty
    private ApiDocsControllerProperties apiDocs = new ApiDocsControllerProperties();

    /**
     * Settings for the Async.
     */
    @NestedConfigurationProperty
    private AsyncProperties async = new AsyncProperties();

    /**
     * Settings for the Aggregation Store.
     */
    @NestedConfigurationProperty
    private AggregationStoreProperties aggregationStore = new AggregationStoreProperties();

    /**
     * Settings for the JPA Store.
     */
    @NestedConfigurationProperty
    private JpaStoreProperties jpaStore = new JpaStoreProperties();

    /**
     * Settings for the API Versioning Strategy.
     */
    @NestedConfigurationProperty
    private ApiVersioningStrategyProperties apiVersioningStrategy = new ApiVersioningStrategyProperties();

    /**
     * Default pagination size for collections if the client doesn't paginate.
     */
    private int pageSize = 500;

    /**
     * The maximum pagination size a client can request.
     */
    private int maxPageSize = 10000;

    /**
     * The base service URL that clients use in queries.  Elide will reference this name
     * in any callback URLs returned by the service.  If not set, Elide uses the API request to derive the base URL.
     */
    private String baseUrl = "";

    /**
     * Turns on/off verbose error responses.
     */
    private boolean verboseErrors = false;

    /**
     * Remove Authorization headers from RequestScope to prevent accidental logging of security credentials.
     */
    private boolean stripAuthorizationHeaders = true;
}
