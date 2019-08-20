/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.contrib.testhelpers.jsonapi;

import com.yahoo.elide.contrib.testhelpers.jsonapi.elements.Attribute;
import com.yahoo.elide.contrib.testhelpers.jsonapi.elements.Attributes;
import com.yahoo.elide.contrib.testhelpers.jsonapi.elements.Data;
import com.yahoo.elide.contrib.testhelpers.jsonapi.elements.Document;
import com.yahoo.elide.contrib.testhelpers.jsonapi.elements.Id;
import com.yahoo.elide.contrib.testhelpers.jsonapi.elements.Include;
import com.yahoo.elide.contrib.testhelpers.jsonapi.elements.PatchOperation;
import com.yahoo.elide.contrib.testhelpers.jsonapi.elements.PatchOperationType;
import com.yahoo.elide.contrib.testhelpers.jsonapi.elements.PatchSet;
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
     * @param resource the singular resource
     * @return a data
     */
    public static Data datum(Resource resource) {
        return new Data(resource);
    }

    /**
     * Data data.
     *
     * @param resources the resources
     * @return a data
     */
    public static Data data(Resource... resources) {
        return new Data(resources);
    }

    /**
     * Include data.
     *
     * @param resources the resources
     * @return An include
     */
    public static Include include(Resource... resources) {
        return new Include(resources);
    }

    /**
     * Data relationships.
     *
     * @param links the relationship links
     * @return A top level JSON-API doc
     */
    public static Data data(ResourceLinkage... links) {
        return new Data(links);
    }

    /**
     * Data relationship.
     *
     * @param link the singular relationship link
     * @return A top level JSON-API doc
     */
    public static Data datum(ResourceLinkage link) {
        return new Data(link);
    }

    /**
     * A document with data and includes.
     *
     * @param resources the data resources
     * @param includes the included resources
     * @return A top level JSON-API doc
     */
    public static Document document(Data resources, Include includes) {
        return new Document(resources, includes);
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
     * @param type       the type
     * @param id         the id
     * @param relationships the attributes
     * @return the resource
     */
    public static Resource resource(Type type, Id id, Relationships relationships) {
        return new Resource(id, type, null, relationships);
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
     * Creates a to-many relationship.
     *
     * @param field           the field
     * @param resourceLinkage the resource linkage
     * @return the relation
     */
    public static Relation relation(String field, ResourceLinkage... resourceLinkage) {
        return new Relation(field, resourceLinkage);
    }

    /**
     * Relation relation.
     *
     * @param field           the field
     * @param toOne           whether or not this is a toOne or toMany relationship
     * @param resourceLinkage the resource linkage
     * @return the relation
     */
    public static Relation relation(String field, boolean toOne, ResourceLinkage... resourceLinkage) {
        return new Relation(field, toOne, resourceLinkage);
    }

    /**
     * Relation relation.
     *
     * @param field           the field
     * @return the relation
     */
    public static Relation relation(String field) {
        return new Relation(field);
    }

    /**
     * Relation relation.
     *
     * @param field           the field
     * @param toOne           whether or not this is a toOne or toMany relationship
     * @return the relation
     */
    public static Relation relation(String field, boolean toOne) {
        return new Relation(field, toOne);
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

    /**
     * Patch Set.
     *
     * @param patchOperations the set of patch operation.
     * @return the patch set
     */
    public static PatchSet patchSet(PatchOperation... patchOperations) {
        return new PatchSet(patchOperations);
    }

    /**
     * Patch Operation.
     *
     * @param operation the operation type
     * @param path the operation path
     * @param value the operation value
     * @return
     */
    public static PatchOperation patchOperation(PatchOperationType operation, String path, Resource value) {
        return new PatchOperation(operation, path, value);
    }
}
