/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.schema.dimension;

import com.yahoo.elide.datastores.aggregation.annotation.Grain;
import com.yahoo.elide.datastores.aggregation.query.ProjectedTimeDimension;

import java.util.TimeZone;

/**
 * Represents a time dimension column in a schema.
 */
public interface TimeDimensionColumn extends DimensionColumn {
    /**
     * Get the requested time zone.
     * @return requested time zone.
     */
    TimeZone getTimeZone();

    /**
     * Get the requested time grain.
     * @return requested time grain.
     */
    Grain[] getSupportedGrains();

    public default ProjectedTimeDimension toRequestedTimeDimension(final Grain requestedGrain) {
        TimeZone requestedTimeZone = getTimeZone();
        String name = getName();

        return new ProjectedTimeDimension() {
            @Override
            public TimeZone getTimeZone() {
                return requestedTimeZone;
            }

            @Override
            public Grain getTimeGrain() {
                return requestedGrain;
            }

            @Override
            public String getName() {
                return name;
            }
        };
    }
}
