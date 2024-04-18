/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.swagger.models.media;

import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.StringSchema;

/**
 * Represents the schema for the 'included' section of a JSON-API compound document.
 * The 'included' section can contain many different types.  As such, this class is not POJO/type
 * specific and just has the skeleton structure of a resource.
 */
public class IncludedResource extends ObjectSchema {
    public IncludedResource() {
        super();

        /* These will always be empty. */
        ObjectSchema attributes = new ObjectSchema();
        ObjectSchema relationships = new ObjectSchema();

        addProperty("type", new StringSchema());
        addProperty("id", new StringSchema());
        addProperty("attributes", attributes);
        addProperty("relationships", relationships);
    }
}
