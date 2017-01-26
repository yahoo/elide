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
 * Represents a JSON-API collection of resources or resource identifiers.
 * It is used when a property is required for swagger.
 */
public class Data extends ObjectProperty {

    /**
     * Used to construct a collection of resources (referenced by the resource type).
     * @param definitionName The swagger model to reference in 'data'
     */
    public Data(String definitionName) {
        super();
        property("data", new ArrayProperty().items(new RefProperty(definitionName)));
        property("included", new ArrayProperty()
            .description("Included resources")
            .uniqueItems()
            .items(new IncludedResource())
        );
    }

    /**
     * Used to construct a collection of resource identifiers.
     * @param relationship Added to the property 'data'
     */
    public Data(Relationship relationship) {
        super();
        property("data", new ArrayProperty().items(relationship));
    }
}
