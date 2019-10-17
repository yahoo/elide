/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.schema.dimension;

import com.yahoo.elide.datastores.aggregation.query.ProjectedDimension;

/**
 * An object that represents one of the allowed types for {@link ProjectedDimension} type in Elide.
 */
public enum  DimensionType {

    /**
     * A dimension backed by a column.
     */
    DEGENERATE,

    /**
     * A dimension backed by a table.
     */
    ENTITY
    ;
}
