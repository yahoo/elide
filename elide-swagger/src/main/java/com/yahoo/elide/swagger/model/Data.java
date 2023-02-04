/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.swagger.model;

import com.yahoo.elide.swagger.property.Relationship;

import io.swagger.models.ModelImpl;
import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.RefProperty;

/**
 * Represents a JSON-API collection of resources or resource identifiers.
 * It is used when a schema is required for swagger.
 */
public class Data extends ModelImpl {
    public Data(String definitionName) {
        super();
        property("data", new ArrayProperty().items(new RefProperty(definitionName)));
    }

    /**
     * Used to construct a collection of resource identifiers.
     * @param relationship is added as a property of 'data'.
     */
    public Data(Relationship relationship) {
        super();
        property("data", new ArrayProperty().items(relationship));
    }
}
