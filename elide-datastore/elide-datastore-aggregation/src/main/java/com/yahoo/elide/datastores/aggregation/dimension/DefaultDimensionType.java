/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.dimension;

/**
 * Enumeration of possible {@link Dimension} types.
 */
public enum  DefaultDimensionType implements DimensionType {

    /**
     * A degenerate dimension.
     */
    DEGENERATE,

    /**
     * A dimension backed by a table
     */
    TABLE
    ;
}
