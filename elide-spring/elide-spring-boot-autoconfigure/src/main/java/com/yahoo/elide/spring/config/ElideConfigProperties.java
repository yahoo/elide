/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.spring.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
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
    private ControllerProperties jsonApi;

    /**
     * Settings for the GraphQL controller.
     */
    private ControllerProperties graphql;

    /**
     * Settings for the Swagger document controller.
     */
    private SwaggerControllerProperties swagger;

    /**
     * Settings for the Async.
     */
    private AsyncProperties async;

    /**
     * Settings for the Dynamic Configuration.
     */
    private DynamicConfigProperties dynamicConfig;

    /**
     * Settings for the Aggregation Store.
     */
    private AggregationStoreProperties aggregationStore;

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
}
