/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.swagger.model;

/**
 * Represents a Swagger Model that was mapped from a POJO.  Each instance will
 * be bound to a specific POJO class.
 */
public class Resource extends ModelImpl {

    ObjectProperty attributes;
    ObjectProperty relationships;
    StringProperty idProperty;
    StringProperty typeProperty;

    public Resource() {
        super();

        attributes = new ObjectProperty();
        relationships = new ObjectProperty();
        idProperty = new StringProperty();
        typeProperty = new StringProperty();

        property("type", typeProperty);
        property("id", idProperty);
        property("attributes", attributes);
        property("relationships", relationships);
    }

    public Resource setSecurityDescription(String description) {
        typeProperty.setDescription(description);
        return this;
    }

    public void addAttribute(String attributeName, Property attribute) {
        attributes.property(attributeName, attribute);
    }

    public void addRelationship(String relationshipName, Property relationship) {
        relationships.property(relationshipName, relationship);
    }
}
