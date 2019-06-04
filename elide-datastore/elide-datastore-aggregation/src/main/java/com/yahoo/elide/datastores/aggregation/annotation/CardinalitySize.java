/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.annotation;

/**
 * A set of constants that indicates how big a dimension is.
 */
public enum CardinalitySize {

    /**
     * Size for a small dimension table.
     * <p>
     * TODO: define size range
     */
    SMALL,

    /**
     * Size for a medium sized dimension table.
     * <p>
     * TODO: define size range
     */
    MEDIUM,

    /**
     * Size for a large dimension table.
     * <p>
     * TODO: define size range
     */
    LARGE
    ;
}
