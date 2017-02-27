/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.swagger.property;

import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.ObjectProperty;
import io.swagger.models.properties.RefProperty;

/**
 * Represents a JSON-API single resource or resource identifier.
 * It is used when a schema is required for swagger.
 */
public class Datum extends ObjectProperty {

    /**
     * Constructs a singular resource (referenced by type).
     *
     * @param definitionName The swagger model to reference in 'data'.
     */
    public Datum(String definitionName) {
        this(definitionName, true);
    }

    /**
     * Constructs a singular resource (referenced by type)
     * @param definitionName The swagger model to reference in 'data'.
     * @param included Whether or not to add the 'included' property to the schema.
     */
    public Datum(String definitionName, boolean included) {
        super();
        property("data", new RefProperty(definitionName));

        if (included) {
            property("included", new ArrayProperty()
                            .description("Included resources")
                            .uniqueItems()
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
        property("data", relationship);
    }
}
