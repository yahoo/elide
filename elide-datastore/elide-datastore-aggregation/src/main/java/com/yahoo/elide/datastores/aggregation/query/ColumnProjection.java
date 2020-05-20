/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.query;

import com.yahoo.elide.datastores.aggregation.metadata.models.Column;
import com.yahoo.elide.request.Argument;

import java.io.Serializable;
import java.util.Map;

/**
 * Represents a projected column as an alias in a query.
 * @param <T> Column type of the projection.
 */
public interface ColumnProjection<T extends Column> extends Serializable {
    /**
     * Get the projected column.
     *
     * @return column
     */
    T getColumn();

    /**
     * Get the projection alias.
     *
     * @return alias
     */
    String getAlias();

    /**
     * Get all arguments provided for this metric function.
     *
     * @return request arguments
     */
    Map<String, Argument> getArguments();

    // force implementations to define equals/hashCode
    boolean equals(Object other);
    int hashCode();
}
