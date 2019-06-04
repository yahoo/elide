/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.dimension;

/**
 * Enumeration of possible {@link ColumnType}s.
 */
public enum DefaultColumnType implements ColumnType {

    PRIMARY_KEY,

    FIELD
    ;
}
