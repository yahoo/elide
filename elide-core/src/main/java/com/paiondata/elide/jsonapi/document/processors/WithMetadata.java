/*
 * Copyright 2022, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.jsonapi.document.processors;

import java.util.Optional;
import java.util.Set;

/**
 * The class carries metadata fields.
 */
public interface WithMetadata {

    /**
     * Sets a metadata property for this request.
     * @param property
     * @param value
     */
    default void setMetadataField(String property, Object value) {
        //noop
    }

    /**
     * Retrieves a metadata property from this request.
     * @param property
     * @return An optional metadata property.
     */
    Optional<Object> getMetadataField(String property);

    /**
     * Return the set of metadata fields that have been set.
     * @return metadata fields that have been set.
     */
    Set<String> getMetadataFields();
}
