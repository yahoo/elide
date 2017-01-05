/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.swagger.property;

import io.swagger.models.properties.ObjectProperty;
import io.swagger.models.properties.StringProperty;

/**
 * Represents a JSON-API resource identifier.
 */
public class Relationship extends ObjectProperty {

    /**
     * Constructs a singular resource identifier
     * @param relationshipType the type of resource
     */
    public Relationship(String relationshipType) {
        super();
        property("type", new StringProperty()._enum(relationshipType));
        property("id", new StringProperty());
    }
}
