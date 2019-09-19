/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.swagger;

import io.swagger.models.Operation;
import io.swagger.models.parameters.Parameter;

import java.util.Optional;

/**
 * Operation that consumes and produces JSON API mime type.
 */
public class JsonApiOperation extends Operation {
    public static final String JSON_API_MIME = "application/vnd.api+json";

    public JsonApiOperation() {
        super();
        consumes(JSON_API_MIME);
        produces(JSON_API_MIME);
    }

    /**
     * Adds a parameter only if it exists.
     * @param parameter The parameter to add.
     * @return The operation under construction.
     */
    public Operation parameter(Optional<Parameter> parameter) {
        if (parameter.isPresent()) {
            super.parameter(parameter.get());
        }
        return this;
    }
}
