/*
 * Copyright 2022, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.spring.config;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Extra controller properties for the GraphQL endpoint.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class GraphQLControllerProperties extends ControllerProperties {

    /**
     * Turns on/off Apollo federation schema
     */
    boolean enableFederation = false;
}
