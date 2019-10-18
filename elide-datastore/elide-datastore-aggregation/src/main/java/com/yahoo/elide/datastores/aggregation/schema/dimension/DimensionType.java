/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.schema.dimension;

/**
 * An object that represents one of the allowed types for {@link DimensionColumn} type in Elide.
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
