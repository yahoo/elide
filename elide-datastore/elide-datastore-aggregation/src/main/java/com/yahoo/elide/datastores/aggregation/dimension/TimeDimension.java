/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.dimension;

import com.yahoo.elide.datastores.aggregation.time.TimeGrain;

import java.util.TimeZone;

/**
 * A dimension backed by a date/time.
 */
public interface TimeDimension extends Dimension {

    /**
     * Get the requested time zone.
     * @return requested time zone.
     */
    TimeZone getTimeZone();

    /**
     * Get the requested time grain.
     * @return requested time grain.
     */
    TimeGrain getTimeGrain();
}
