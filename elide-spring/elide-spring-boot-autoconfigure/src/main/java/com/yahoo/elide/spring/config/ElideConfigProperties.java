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
     * Default pagination size for collections if the client doesn't paginate.
     */
    private int pageSize = 500;

    /**
     * The maximum pagination size a client can request.
     */
    private int maxPageSize = 10000;
}
