/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.spring.config;

import com.yahoo.elide.core.dictionary.EntityDictionary;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Extra controller properties for the OpenAPI document endpoint.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ApiDocsControllerProperties extends ControllerProperties {
    /**
     * The OpenAPI document version to generate. Either 3.0 or 3.1.
     */
    private String version = "3.0";

    /**
     * The API version that should correspond with the API versions in the Entity Dictionary.
     */
    private String apiVersion = EntityDictionary.NO_VERSION;
}
