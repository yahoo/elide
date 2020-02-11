/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.query;

import com.yahoo.elide.core.exceptions.InvalidValueException;
import com.yahoo.elide.datastores.aggregation.metadata.enums.TimeGrain;
import com.yahoo.elide.datastores.aggregation.metadata.models.Column;
import com.yahoo.elide.datastores.aggregation.metadata.models.TimeDimension;

import java.util.TimeZone;

/**
 * Represents a requested time dimension in a query.
 */
public interface TimeDimensionProjection extends ColumnProjection {
    /**
     * Get the projected time dimension.
     *
     * @return time dimension
     */
    TimeDimension getTimeDimension();

    /**
     * The time dimension is the projected column.
     *
     * @return project column
     */
    @Override
    default Column getColumn() {
        return getTimeDimension();
    }

    /**
     * Get the requested time grain.
     *
     * @return time grain
     */
    TimeGrain getGrain();

    /**
     * Get the requested time zone.
     *
     * @return time zone
     */
    TimeZone getTimeZone();

    /**
     * Convert this projection to a new time grain.
     *
     * @param newGrain new time grain
     * @return a new projection
     */
    default TimeDimensionProjection toTimeGrain(TimeGrain newGrain) {
        if (getTimeDimension().getSupportedGrains().stream()
                .noneMatch(supportedGrain -> supportedGrain.getGrain().equals(newGrain))) {
            throw new InvalidValueException(getTimeDimension().getId() + " doesn't support grain " + newGrain);
        }

        TimeDimensionProjection projection = this;
        return new TimeDimensionProjection() {
            @Override
            public TimeDimension getTimeDimension() {
                return projection.getTimeDimension();
            }

            @Override
            public TimeGrain getGrain() {
                return newGrain;
            }

            @Override
            public TimeZone getTimeZone() {
                return projection.getTimeZone();
            }

            @Override
            public String getAlias() {
                return projection.getAlias();
            }
        };
    }
}
