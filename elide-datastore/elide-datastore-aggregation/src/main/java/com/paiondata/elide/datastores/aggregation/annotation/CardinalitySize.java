/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.datastores.aggregation.annotation;

/**
 * A set of constants that indicates how big a dimension is.
 */
public enum CardinalitySize {

    /**
     * Size for a tiny dimension table.
     * <p>
     * TODO: define size range
     */
    TINY,
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
    LARGE,

    /**
     * Size for a huge dimension table.
     * <p>
     * TODO: define size range
     */
    HUGE,

    /**
     * If size indicator is not provided.
     */
    UNKNOWN
    ;
}
