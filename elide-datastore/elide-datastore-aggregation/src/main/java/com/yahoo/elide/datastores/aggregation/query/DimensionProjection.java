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
    Dimension getDimension();

    String getAlias();

    static DimensionProjection toProjection(Dimension dimension, String alias) {
        return new DimensionProjection() {
            @Override
            public Dimension getDimension() {
                return dimension;
            }

            @Override
            public String getAlias() {
                return alias;
            }
        };
    }

    static TimeDimensionProjection toProjection(TimeDimension dimension, TimeGrain grain, String alias) {
        if (dimension.getSupportedGrains().stream().anyMatch(g -> g.getGrain().equals(grain))) {
            return new TimeDimensionProjection() {
                @Override
                public TimeGrain getProjectedGrain() {
                    return grain;
                }

                @Override
                public TimeDimension getDimension() {
                    return dimension;
                }

                @Override
                public String getAlias() {
                    return alias;
                }
            };
        }
        return null;
    }
}
