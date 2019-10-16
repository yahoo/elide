/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.schema.dimension;

/**
 * An object that represents one of the allowed types for a SQL table column.
 */
public enum  ColumnType {

    /**
     * A primary key column.
     */
    PRIMARY_KEY,

    /**
     * A time column.
     */
    TEMPORAL,

    /**
     * All other columns.
     */
    FIELD
    ;
}
