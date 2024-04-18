/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.test.jsonapi;

import com.paiondata.elide.test.jsonapi.elements.AtomicOperation;
import com.paiondata.elide.test.jsonapi.elements.AtomicOperationCode;
import com.paiondata.elide.test.jsonapi.elements.AtomicOperations;
import com.paiondata.elide.test.jsonapi.elements.Attribute;
import com.paiondata.elide.test.jsonapi.elements.Attributes;
import com.paiondata.elide.test.jsonapi.elements.Data;
import com.paiondata.elide.test.jsonapi.elements.Document;
import com.paiondata.elide.test.jsonapi.elements.Id;
import com.paiondata.elide.test.jsonapi.elements.Include;
import com.paiondata.elide.test.jsonapi.elements.Lid;
import com.paiondata.elide.test.jsonapi.elements.Links;
import com.paiondata.elide.test.jsonapi.elements.PatchOperation;
import com.paiondata.elide.test.jsonapi.elements.PatchOperationType;
import com.paiondata.elide.test.jsonapi.elements.PatchSet;
import com.paiondata.elide.test.jsonapi.elements.Ref;
import com.paiondata.elide.test.jsonapi.elements.Relation;
import com.paiondata.elide.test.jsonapi.elements.Relationship;
import com.paiondata.elide.test.jsonapi.elements.Relationships;
import com.paiondata.elide.test.jsonapi.elements.Resource;
import com.paiondata.elide.test.jsonapi.elements.ResourceLinkage;
import com.paiondata.elide.test.jsonapi.elements.Type;

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
     * @return a data
     */
    public static Data data(Resource... resources) {
        return new Data(resources);
    }

    /**
     * Data data.
     *
     * @param resources the resources
     * @return a data
     */
    public static Data datum(Resource resources) {
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
        return new Resource(id, type, attributes, null, relationships);
    }

    /**
     * Resource resource.
     *
     * @param type          the type
     * @param lid           the lid
     * @param attributes    the attributes
     * @param relationships the relationships
     * @return the resource
     */
    public static Resource resource(Type type, Lid lid, Attributes attributes, Relationships relationships) {
        return new Resource(lid, type, attributes, null, relationships);
    }

    /**
     * Resource resource.
     *
     * @param type          the type
     * @param id            the id
     * @param attributes    the attributes
     * @param link          the link
     * @param relationships the relationships
     * @return the resource
     */
    public static Resource resource(Type type, Id id, Attributes attributes, Links link, Relationships relationships) {
        return new Resource(id, type, attributes, link, relationships);
    }

    /**
     * Resource resource.
     *
     * @param type          the type
     * @param id            the id
     * @param attributes    the attributes
     * @param link          the link
     * @return the resource
     */
    public static Resource resource(Type type, Id id, Attributes attributes, Links link) {
        return new Resource(id, type, attributes, link, null);
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
        return new Resource(id, type, attributes, null, null);
    }

    /**
     * Resource resource.
     *
     * @param type       the type
     * @param lid        the lid
     * @param attributes the attributes
     * @return the resource
     */
    public static Resource resource(Type type, Lid lid, Attributes attributes) {
        return new Resource(lid, type, attributes, null, null);
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
        return new Resource(id, type, null, null, relationships);
    }

    /**
     * Resource resource.
     *
     * @param type the type
     * @param id   the id
     * @return the resource
     */
    public static Resource resource(Type type, Id id) {
        return new Resource(id, type, null, null, null);
    }

    /**
     * Resource resource.
     *
     * @param type the type
     * @param lid   the lid
     * @return the resource
     */
    public static Resource resource(Type type, Lid lid) {
        return new Resource(lid, type, null, null, null);
    }

    /**
     * Resource resource.
     *
     * @param type       the type
     * @param attributes the attributes
     * @return the resource
     */
    public static Resource resource(Type type, Attributes attributes) {
        return new Resource(id(null), type, attributes, null, null);
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
        return new Resource(id(null), type, attributes, null, relationships);
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
     * Local Id id.
     *
     * @param lid the lid
     * @return the lid
     */
    public static Lid lid(Object lid) {
        return new Lid(lid);
    }

    /**
     * Relationship relationship.
     *
     * @param value the value
     * @return the relationship
     */
    public static Relationship relationship(String value) {
        return new Relationship(value);
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
     * Links links.
     *
     * @param attrs the attrs
     * @return the attributes
     */
    public static Links links(Attribute... attrs) {
        return new Links(attrs);
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
        return new Relation(field, toOne, null, resourceLinkage);
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
     * @param links           the links
     * @return the relation
     */
    public static Relation relation(String field, Links links) {
        return new Relation(field, links);
    }

    /**
     * Relation relation.
     *
     * @param field           the field
     * @param toOne           whether or not this is a toOne or toMany relationship
     * @param links           the links
     * @return the relation
     */
    public static Relation relation(String field, boolean toOne, Links links) {
        return new Relation(field, toOne, links);
    }

    /**
     * Relation relation.
     *
     * @param field           the field
     * @param toOne           whether or not this is a toOne or toMany relationship
     * @param links           the links
     * @param resourceLinkage the resource linkage
     * @return the relation
     */
    public static Relation relation(String field, boolean toOne, Links links, ResourceLinkage... resourceLinkage) {
        return new Relation(field, toOne, links, resourceLinkage);
    }

    /**
     * Relation relation.
     *
     * @param field           the field
     * @param links           the links
     * @param resourceLinkage the resource linkage
     * @return the relation
     */
    public static Relation relation(String field, Links links, ResourceLinkage... resourceLinkage) {
        return new Relation(field, links, resourceLinkage);
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
     * @return the patch operation
     */
    public static PatchOperation patchOperation(PatchOperationType operation, String path, Resource value) {
        return new PatchOperation(operation, path, value);
    }

    public static AtomicOperations atomicOperations(AtomicOperation... atomicOperations) {
        return new AtomicOperations(atomicOperations);
    }

    public static AtomicOperation atomicOperation(AtomicOperationCode operation, Data data) {
        return new AtomicOperation(operation, data);
    }

    public static AtomicOperation atomicOperation(AtomicOperationCode operation, String href, Data data) {
        return new AtomicOperation(operation, href, data);
    }

    public static AtomicOperation atomicOperation(AtomicOperationCode operation, Ref ref, Data data) {
        return new AtomicOperation(operation, ref, data);
    }

    public static AtomicOperation atomicOperation(AtomicOperationCode operation, Ref ref) {
        return new AtomicOperation(operation, ref, null);
    }

    public static Ref ref(Type type, Id id) {
        return new Ref(type, id);
    }

    public static Ref ref(Type type, Id id, Relationship relationship) {
        return new Ref(type, id, relationship);
    }

    public static Ref ref(Type type, Lid lid) {
        return new Ref(type, lid);
    }

    public static Ref ref(Type type, Lid lid, Relationship relationship) {
        return new Ref(type, lid, relationship);
    }
}
