/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.swagger.models.media;

import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ObjectSchema;

/**
 * Represents a JSON-API collection of resources or resource identifiers.
 * It is used when a property is required for OpenAPI.
 */
public class Data extends ObjectSchema {

    /**
     * Used to construct a collection of resources (referenced by the resource type).
     * @param definitionName The swagger model to reference in 'data'
     */
    public Data(String definitionName) {
        this(definitionName, true);
    }

    /**
     * Used to construct a collection of resources (referenced by the resource type).
     * @param definitionName The swagger model to reference in 'data'
     * @param included Whether or not to add the 'included' property to the schema.
     */
    @SuppressWarnings("unchecked")
    public Data(String definitionName, boolean included) {
        super();
        addProperty("data", new ArraySchema().items(new ObjectSchema().$ref(definitionName)));

        if (included) {
            addProperty("included", new ArraySchema()
                            .description("Included resources")
                            .uniqueItems(true)
                            .items(new IncludedResource())
            );
        }
    }

    /**
     * Used to construct a collection of resource identifiers.
     * @param relationship Added to the property 'data'
     */
    public Data(Relationship relationship) {
        super();
        addProperty("data", new ArraySchema().items(relationship));
    }
}
