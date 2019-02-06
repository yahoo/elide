/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.contrib.testhelpers.jsonapi;

import com.yahoo.elide.contrib.testhelpers.jsonapi.elements.Attribute;
import com.yahoo.elide.contrib.testhelpers.jsonapi.elements.Attributes;
import com.yahoo.elide.contrib.testhelpers.jsonapi.elements.Data;
import com.yahoo.elide.contrib.testhelpers.jsonapi.elements.Id;
import com.yahoo.elide.contrib.testhelpers.jsonapi.elements.Relation;
import com.yahoo.elide.contrib.testhelpers.jsonapi.elements.Relationships;
import com.yahoo.elide.contrib.testhelpers.jsonapi.elements.Resource;
import com.yahoo.elide.contrib.testhelpers.jsonapi.elements.ResourceLinkage;
import com.yahoo.elide.contrib.testhelpers.jsonapi.elements.Type;

/**
 * Adds helper functions for creating Json API style data.
 * <p>
 * Example:
 * data(
 * resource(
 * type("users"),
 * id("the-user-id")
 * )
 * )
 * <p>
 * Creates Json in the form:
 * "data": {
 * "type": "user",
 * "id":   "the-user-id"
 * }
 */
public class JsonApiDSL {

    /**
     * Data data.
     *
     * @param resources the resources
     * @return data
     */
    public static Data data(Resource... resources) {
        return new Data(resources);
    }

    /**
     * Resource resource.
     *
     * @param type          the type
     * @param id            the id
     * @param attributes    the attributes
     * @param relationships the relationships
     * @return the resource
     */
    public static Resource resource(Type type, Id id, Attributes attributes, Relationships relationships) {
        return new Resource(id, type, attributes, relationships);
    }

    /**
     * Resource resource.
     *
     * @param type       the type
     * @param id         the id
     * @param attributes the attributes
     * @return the resource
     */
    public static Resource resource(Type type, Id id, Attributes attributes) {
        return new Resource(id, type, attributes, null);
    }

    /**
     * Resource resource.
     *
     * @param type the type
     * @param id   the id
     * @return the resource
     */
    public static Resource resource(Type type, Id id) {
        return new Resource(id, type, null, null);
    }

    /**
     * Resource resource.
     *
     * @param type       the type
     * @param attributes the attributes
     * @return the resource
     */
    public static Resource resource(Type type, Attributes attributes) {
        return new Resource(id(null), type, attributes, null);
    }

    /**
     * Resource resource.
     *
     * @param type          the type
     * @param attributes    the attributes
     * @param relationships the relationships
     * @return the resource
     */
    public static Resource resource(Type type, Attributes attributes, Relationships relationships) {
        return new Resource(id(null), type, attributes, relationships);
    }

    /**
     * Type type.
     *
     * @param value the value
     * @return the type
     */
    public static Type type(String value) {
        return new Type(value);
    }

    /**
     * Id id.
     *
     * @param id the id
     * @return the id
     */
    public static Id id(Object id) {
        return new Id(id);
    }

    /**
     * Attributes attributes.
     *
     * @param attrs the attrs
     * @return the attributes
     */
    public static Attributes attributes(Attribute... attrs) {
        return new Attributes(attrs);
    }

    /**
     * Attr attribute.
     *
     * @param key   the key
     * @param value the value
     * @return the attribute
     */
    public static Attribute attr(String key, Object value) {
        return new Attribute(key, value);
    }

    /**
     * Relationships relationships.
     *
     * @param relationships the relationships
     * @return the relationships
     */
    public static Relationships relationships(Relation... relationships) {
        return new Relationships(relationships);
    }

    /**
     * Relation relation.
     *
     * @param field           the field
     * @param resourceLinkage the resource linkage
     * @return the relation
     */
    public static Relation relation(String field, ResourceLinkage... resourceLinkage) {
        return new Relation(field, resourceLinkage);
    }

    /**
     * Linkage resource linkage.
     *
     * @param type the type
     * @param id   the id
     * @return the resource linkage
     */
    public static ResourceLinkage linkage(Type type, Id id) {
        return new ResourceLinkage(id, type);
    }
}
