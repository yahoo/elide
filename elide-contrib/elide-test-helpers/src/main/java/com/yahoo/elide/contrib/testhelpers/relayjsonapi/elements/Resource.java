/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.testhelpers.relayjsonapi.elements;

import com.yahoo.elide.contrib.testhelpers.jsonapi.elements.Attribute;

import java.util.Collections;

/**
 * {@link Resource} represents all instances of an entity in a GraphQL JSON response.
 */
public class Resource extends Attribute {

    /**
     * Constructor.
     *
     * @param resourceName  The entity name
     * @param edges  All instances of the entity wrapped in {@link Edges}-{@link Node nodes}.
     */
    public Resource(String resourceName, Edges edges) {
        super(resourceName, Collections.singletonMap("edges", edges));
    }
}
