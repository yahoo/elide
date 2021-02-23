/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.swagger.model;

import com.yahoo.elide.swagger.property.Relationship;

import io.swagger.models.ModelImpl;
import io.swagger.models.properties.RefProperty;

/**
 * Represents a JSON-API singular resource or resource identifier.
 * It is used when a schema is required for swagger.
 */
public class Datum extends ModelImpl {

    public Datum(String definitionName) {
        super();
        property("data", new RefProperty(definitionName));
    }

    /**
     * Constructs a singular resource identifier.
     *
     * @param relationship added as a property of 'data'.
     */
    public Datum(Relationship relationship) {
        super();
        property("data", relationship);
    }
}
