/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.query;

import com.yahoo.elide.datastores.aggregation.schema.Schema;
import com.yahoo.elide.datastores.aggregation.schema.dimension.DimensionColumn;

import java.io.Serializable;

/**
 * Represents a selected dimension in a Query.
 */
public interface ProjectedDimension extends Serializable {

    /**
     * Returns the name of the entity representing this {@link ProjectedDimension} object as a {@link String}.
     *
     * @return the name of the entity or interface representing this {@link ProjectedDimension}.
     */
    String getName();

    /**
     * Given a schema, converts this requested dimension into a schema column.
     * @param schema The provided schema
     * @return A dimension column.
     */
    default DimensionColumn toDimensionColumn(Schema schema) {
        return schema.getDimension(getName());
    }
}
