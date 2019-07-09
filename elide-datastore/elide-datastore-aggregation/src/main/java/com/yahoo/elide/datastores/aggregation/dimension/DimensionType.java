/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.dimension;

/**
 * An object that represents one of the allowed types for {@link Dimension} type in Elide.
 */
public enum  DimensionType {

    /**
     * A degenerate dimension.
     */
    DEGENERATE,

    /**
     * A dimension backed by a table.
     */
    TABLE
    ;
}
