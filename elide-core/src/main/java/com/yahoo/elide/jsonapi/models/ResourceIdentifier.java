/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.jsonapi.models;

import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.exceptions.ForbiddenAccessException;
import com.yahoo.elide.core.exceptions.InvalidObjectIdentifierException;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Model representing JSON API Linkage.
 */
public class ResourceIdentifier {
    private final String type;
    private final String id;

    public ResourceIdentifier(@JsonProperty("type") String type, @JsonProperty("id") String id) {
        this.type = type;
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public String getId() {
        return id;
    }

    public PersistentResource toPersistentResource(RequestScope requestScope)
        throws ForbiddenAccessException, InvalidObjectIdentifierException {
        Class<?> cls = requestScope.getDictionary().getEntityClass(type);
        return PersistentResource.loadRecord(cls, id, requestScope);
    }

    public Resource castToResource() {
        return new Resource(type, id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ResourceIdentifier that = (ResourceIdentifier) o;

        return new EqualsBuilder()
            .append(type, that.type)
            .append(id, that.id)
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
            .append(type)
            .append(id)
            .toHashCode();
    }
}
