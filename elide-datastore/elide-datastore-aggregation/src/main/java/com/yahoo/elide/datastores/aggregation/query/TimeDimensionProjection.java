/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.query;

import com.yahoo.elide.datastores.aggregation.annotation.TimeGrainDefinition;
import java.util.TimeZone;

/**
 * Represents a requested time dimension in a query.
 */
public interface TimeDimensionProjection extends DimensionProjection {

    /**
     * Get the requested time zone.
     * @return requested time zone.
     */
    TimeZone getTimeZone();

    /**
     * Get the requested time grain.
     * @return requested time grain.
     */
    TimeGrainDefinition getTimeGrain();
}
