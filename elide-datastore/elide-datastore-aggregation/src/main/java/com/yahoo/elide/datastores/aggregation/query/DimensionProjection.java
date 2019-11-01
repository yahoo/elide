/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.query;

import com.yahoo.elide.datastores.aggregation.metadata.models.Dimension;
import com.yahoo.elide.datastores.aggregation.metadata.models.TimeDimension;
import com.yahoo.elide.datastores.aggregation.time.TimeGrain;

import java.io.Serializable;

/**
 * Represents a selected dimension in a Query.
 */
public interface DimensionProjection extends Serializable {

    /**
     * Returns the name of the entity representing this {@link DimensionProjection} object as a {@link String}.
     *
     * @return the name of the entity or interface representing this {@link DimensionProjection}.
     */
    String getName();

    static DimensionProjection toDimensionProjection(Dimension dimension) {
        return (DimensionProjection) dimension::getName;
    }

    static TimeDimensionProjection toDimensionProjection(TimeDimension dimension, TimeGrain grain) {
        if (dimension.getSupportedGrains().contains(grain)) {
            return new TimeDimensionProjection() {
                @Override
                public String getName() {
                    return dimension.getName();
                }
            };
        }
        return null;
    }
}
