/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.security;

import com.yahoo.elide.core.type.Type;

import java.util.Optional;
import java.util.Set;

/**
 * The persistent resource interface passed to change specs.
 * @param <T> resource type
 */
public interface PersistentResource<T> {

    boolean matchesId(String id);

    Optional<String> getUUID();
    String getId();
    String getTypeName();

    /**
     * Sets a metadata property for this resource.
     * @param property
     * @param value
     */
    void setMetadataField(String property, Object value);

    /**
     * Retrieves a metadata property from this resource.
     * @param property
     * @return An optional metadata property.
     */
    Optional<Object> getMetadataField(String property);

    /**
     * Return the set of metadata fields that have been set.
     * @return metadata fields that have been set.
     */
    Set<String> getMetadataFields();

    T getObject();
    Type<T> getResourceType();
    RequestScope getRequestScope();
}
