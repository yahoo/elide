/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.swagger.models.media;

import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;

/**
 * Represents a OpenAPI Model that was mapped from a POJO.  Each instance will
 * be bound to a specific POJO class.
 */
public class Resource extends ObjectSchema {
    ObjectSchema attributes;
    ObjectSchema relationships;
    StringSchema idProperty;
    StringSchema typeProperty;

    public Resource() {
        super();

        attributes = new ObjectSchema();
        relationships = new ObjectSchema();
        idProperty = new StringSchema();
        typeProperty = new StringSchema();

        addProperty("type", typeProperty);
        addProperty("id", idProperty);
        addProperty("attributes", attributes);
        addProperty("relationships", relationships);
    }

    public Resource setSecurityDescription(String description) {
        typeProperty.setDescription(description);
        return this;
    }

    public void addAttribute(String attributeName, Schema<?> attribute) {
        attributes.addProperty(attributeName, attribute);
    }

    public void addRelationship(String relationshipName, Relationship relationship) {
        relationships.addProperty(relationshipName, new Data(relationship));
    }

    public ObjectSchema getAttributes() {
        return this.attributes;
    }

    public ObjectSchema getRelationships() {
        return this.relationships;
    }
}
