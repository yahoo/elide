/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.query;

import com.yahoo.elide.datastores.aggregation.metadata.enums.TimeGrain;
import com.yahoo.elide.datastores.aggregation.metadata.models.TimeDimension;

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
}
