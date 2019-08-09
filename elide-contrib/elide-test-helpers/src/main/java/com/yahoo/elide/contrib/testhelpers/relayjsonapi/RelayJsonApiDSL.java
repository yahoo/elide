/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.testhelpers.relayjsonapi;

import com.yahoo.elide.contrib.testhelpers.jsonapi.elements.Attribute;
import com.yahoo.elide.contrib.testhelpers.jsonapi.elements.Attributes;
import com.yahoo.elide.contrib.testhelpers.relayjsonapi.elements.Data;
import com.yahoo.elide.contrib.testhelpers.relayjsonapi.elements.Edges;
import com.yahoo.elide.contrib.testhelpers.relayjsonapi.elements.Node;
import com.yahoo.elide.contrib.testhelpers.relayjsonapi.elements.Resource;

import java.util.Arrays;
import java.util.Collections;

/**
 * Adds helper functions for creating Relay's JSON style GraphQL response data.
 */
public final class RelayJsonApiDSL {

    /**
     * Constructor.
     * <p>
     * Suppress default constructor for noninstantiability.
     */
    private RelayJsonApiDSL() {
        throw new AssertionError();
    }

    /**
     * Creates a complete GraphQL response.
     *
     * @param resource  The object representing the response JSON data of an entity
     *
     * @return a serializable GraphQL response object
     */
    public static Data datum(Resource resource) {
        return new Data(resource);
    }

    /**
     * Creates an object representing the response JSON data of an entity.
     *
     * @param resourceName  The entity name
     * @param edges  All data returned about the entity
     *
     * @return a serializable response entity
     */
    public static Resource resource(String resourceName, Edges edges) {
        return new Resource(resourceName, edges);
    }

    /**
     * Creates a key-value pair representing the hydrated field of an returned entity.
     *
     * @param name  The key
     * @param value  The value
     *
     * @return an entity field with value
     */
    public static Attribute attribute(String name, Object value) {
        return new Attribute(name, value);
    }

    /**
     * Creates a single {@link Attribute} group.
     *
     * @param attribute  The single attribute
     *
     * @return the single {@link Attribute} group
     */
    public static Attributes attribute(Attribute attribute) {
        return new Attributes(attribute);
    }

    /**
     * Creates a {@link Node} instance.
     *
     * @param attributes  All fields in this {@link Node} instance
     *
     * @return a {@link Node} instance
     */
    public static Node node(Attribute... attributes) {
        return new Node(new Attributes(attributes));
    }

    /**
     * Creates {@link Edges} instance.
     *
     * @param nodes  All {@link Node}s wrapped inside this {@link Edges} instance
     *
     * @return an {@link Edges} instance
     */
    public static Edges edges(Node... nodes) {
        return new Edges(Arrays.asList(nodes));
    }

    /**
     * Creates an empty {@link Edges} instance.
     *
     * @return a {@link Edges} instance with no returned data about an entity
     */
    public static Edges edges() {
        return new Edges(Collections.emptyList());
    }
}
