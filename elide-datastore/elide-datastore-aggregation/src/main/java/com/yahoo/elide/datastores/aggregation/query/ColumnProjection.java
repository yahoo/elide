/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.query;

import com.yahoo.elide.core.exceptions.InvalidValueException;
import com.yahoo.elide.datastores.aggregation.metadata.models.Column;
import com.yahoo.elide.datastores.aggregation.metadata.models.Dimension;
import com.yahoo.elide.datastores.aggregation.metadata.models.TimeDimension;
import com.yahoo.elide.datastores.aggregation.time.TimeGrain;

import java.io.Serializable;

/**
 * Represents a projected column as an alias in a query.
 */
public interface ColumnProjection extends Serializable {

    /**
     * Get the projected column.
     *
     * @return column
     */
    Column getColumn();

    /**
     * Get the projection alias.
     *
     * @return alias
     */
    String getAlias();

    /**
     * Project a dimension as alias.
     *
     * @param dimension dimension column
     * @param alias alias
     * @return a projection represents that "dimension AS alias"
     */
    static ColumnProjection toProjection(Dimension dimension, String alias) {
        return new ColumnProjection() {
            @Override
            public Dimension getColumn() {
                return dimension;
            }

            @Override
            public String getAlias() {
                return alias;
            }
        };
    }

    /**
     * Project a time dimension as alias with specific time grain.
     *
     * @param dimension time dimension column
     * @param grain projected time grain
     * @param alias alias
     * @return a projection represents that "grain(dimension) AS alias"
     */
    static TimeDimensionProjection toProjection(TimeDimension dimension, TimeGrain grain, String alias) {
        if (dimension.getSupportedGrains().stream().anyMatch(g -> g.getGrain().equals(grain))) {
            return new TimeDimensionProjection() {
                @Override
                public TimeGrain getGrain() {
                    return grain;
                }

                @Override
                public TimeDimension getTimeDimension() {
                    return dimension;
                }

                @Override
                public String getAlias() {
                    return alias;
                }
            };
        }
        throw new InvalidValueException(dimension.getId() + " doesn't support grain " + grain);
    }
}
