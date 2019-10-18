/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.schema.dimension;

import com.yahoo.elide.datastores.aggregation.annotation.TimeGrainDefinition;
import com.yahoo.elide.datastores.aggregation.query.TimeDimensionProjection;
import com.google.common.base.Preconditions;

import java.util.Set;
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
    Set<TimeGrainDefinition> getSupportedGrains();

    default TimeDimensionProjection toProjectedDimension(final TimeGrainDefinition requestedGrain) {
        Preconditions.checkArgument(getSupportedGrains().contains(requestedGrain));

        TimeZone requestedTimeZone = getTimeZone();
        String name = getName();

        return new TimeDimensionProjection() {
            @Override
            public TimeZone getTimeZone() {
                return requestedTimeZone;
            }

            @Override
            public TimeGrainDefinition getTimeGrain() {
                return requestedGrain;
            }

            @Override
            public String getName() {
                return name;
            }
        };
    }
}
