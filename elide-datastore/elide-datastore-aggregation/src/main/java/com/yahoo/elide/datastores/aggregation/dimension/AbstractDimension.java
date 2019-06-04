/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.dimension;

import com.yahoo.elide.datastores.aggregation.annotation.CardinalitySize;

import lombok.AccessLevel;
import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * Base class of {@link Dimension} with skeleton implementations.
 */
@Data
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class AbstractDimension implements Dimension {

    private static final long serialVersionUID = 6719096405754705293L;

    protected final String name;

    protected final String longName;

    protected final String description;

    protected final DimensionType dimensionType;

    protected final Class<?> dataType;

    protected final CardinalitySize cardinality;

    protected final String friendlyName;
}
