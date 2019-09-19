/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.swagger;

import io.swagger.models.Operation;

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
}
