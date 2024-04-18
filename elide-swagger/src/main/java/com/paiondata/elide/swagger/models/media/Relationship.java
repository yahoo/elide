/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.swagger.models.media;

import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.StringSchema;

/**
 * Represents a JSON-API resource identifier.
 */
public class Relationship extends ObjectSchema {

    /**
     * Constructs a singular resource identifier.
     * @param relationshipType the type of resource.
     */
    public Relationship(String relationshipType) {
        super();
        addProperty("type", new StringSchema().addEnumItem(relationshipType));
        addProperty("id", new StringSchema());
    }
}
