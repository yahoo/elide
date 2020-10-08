/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.query;

import com.yahoo.elide.datastores.aggregation.metadata.enums.TimeGrain;
import com.yahoo.elide.datastores.aggregation.metadata.models.TimeDimension;
import com.yahoo.elide.request.Argument;

import java.util.Map;
import java.util.TimeZone;

/**
 * Represents a requested time dimension in a query.
 */
public interface TimeDimensionProjection extends ColumnProjection<TimeDimension> {
    /**
     * Get the projected time dimension.
     *
     * @return time dimension
     */
    @Override
    TimeDimension getColumn();

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

        TimeDimensionProjection projection = this;

        return new TimeDimensionProjection() {
            @Override
            public TimeDimension getColumn() {
                return projection.getColumn();
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

            @Override
            public Map<String, Argument> getArguments() {
                return projection.getArguments();
            }

            @Override
            public Queryable getSource() {
                return projection.getColumn().getTable();
            }
        };
    }
}
