/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.swagger.property;

import io.swagger.models.properties.ObjectProperty;
import io.swagger.models.properties.StringProperty;

/**
 * Represents the schema for the 'included' section of a JSON-API compound document.
 * The 'included' section can contain many different types.  As such, this class is not POJO/type
 * specific and just has the skeleton structure of a resource.
 */
public class IncludedResource extends ObjectProperty {
    public IncludedResource() {
        super();

        /* These will always be empty. */
        ObjectProperty attributes = new ObjectProperty();
        ObjectProperty relationships = new ObjectProperty();

        property("type", new StringProperty());
        property("id", new StringProperty());
        property("attributes", attributes);
        property("relationships", relationships);
    }
}
