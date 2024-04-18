/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.datastores.aggregation.query;

import com.paiondata.elide.datastores.aggregation.metadata.enums.TimeGrain;

import java.util.TimeZone;

/**
 * Represents a requested time dimension in a query.
 */
public interface TimeDimensionProjection extends ColumnProjection {

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
