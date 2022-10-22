/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.security;

import com.yahoo.elide.core.datastore.DataStoreTransaction;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The request scope interface passed to checks.
 */
public interface RequestScope {
    User getUser();
    String getApiVersion();
    String getRequestHeaderByName(String headerName);
    String getBaseUrlEndPoint();
    Map<String, List<String>> getQueryParams();
    DataStoreTransaction getTransaction();

    /**
     * Sets a metadata property for this request.
     * @param property
     * @param value
     */
    void setMetadataField(String property, Object value);

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
