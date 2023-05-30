/*
 * Copyright 2022, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.spring.config;

import org.springframework.boot.context.properties.NestedConfigurationProperty;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Extra controller properties for the GraphQL endpoint.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class GraphQLControllerProperties extends ControllerProperties {

    @Data
    public static class Federation {
        /**
         * Turns on/off Apollo federation schema.
         */
        private boolean enabled = false;
    }

    /**
     * Settings for subscriptions.
     */
    @NestedConfigurationProperty
    private SubscriptionProperties subscription = new SubscriptionProperties();

    private Federation federation = new Federation();
}
