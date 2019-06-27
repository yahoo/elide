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

import java.util.Objects;

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

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof AbstractDimension)) {
            return false;
        }

        final AbstractDimension that = (AbstractDimension) other;
        return getName().equals(that.getName())
                && getLongName().equals(that.getLongName())
                && getDescription().equals(that.getDescription())
                && getDimensionType().equals(that.getDimensionType())
                && getDataType().equals(that.getDataType())
                && getCardinality() == that.getCardinality()
                && getFriendlyName().equals(that.getFriendlyName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                getName(),
                getLongName(),
                getDescription(),
                getDimensionType(),
                getDataType(),
                getCardinality(),
                getFriendlyName()
        );
    }
}
