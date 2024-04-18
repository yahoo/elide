/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.swagger.models.media;

import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ObjectSchema;

/**
 * Represents a JSON-API single resource or resource identifier.
 * It is used when a schema is required for OpenAPI.
 */
public class Datum extends ObjectSchema {

    /**
     * Constructs a singular resource (referenced by type).
     *
     * @param definitionName The OpenAPI model to reference in 'data'.
     */
    public Datum(String definitionName) {
        this(definitionName, true);
    }

    /**
     * Constructs a singular resource (referenced by type).
     * @param definitionName The OpenAPI model to reference in 'data'.
     * @param included Whether or not to add the 'included' property to the schema.
     */
    @SuppressWarnings("unchecked")
    public Datum(String definitionName, boolean included) {
        super();
        addProperty("data", new ObjectSchema().$ref(definitionName));

        if (included) {
            addProperty("included", new ArraySchema()
                            .description("Included resources")
                            .uniqueItems(true)
                            .items(new IncludedResource())
            );
        }
    }

    /**
     * Constructs a singular resource identifier.
     *
     * @param relationship Added to the property 'data'.
     */
    public Datum(Relationship relationship) {
        super();
        addProperty("data", relationship);
    }
}
