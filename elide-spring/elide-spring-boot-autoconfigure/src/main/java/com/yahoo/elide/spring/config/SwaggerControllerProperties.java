/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.spring.config;

import lombok.Data;

/**
 * Extra controller properties for the Swagger document endpoint.
 */
@Data
public class SwaggerControllerProperties extends ControllerProperties {

    /**
     * Swagger needs a name for the service.
     */
    private String name = "Elide Service";

    /**
     * Swagger needs a version for the service.
     */
    private String version = "1.0";
}
