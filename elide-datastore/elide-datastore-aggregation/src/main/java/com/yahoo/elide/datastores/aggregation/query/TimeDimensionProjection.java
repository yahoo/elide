/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.query;

import com.yahoo.elide.core.exceptions.InvalidValueException;
import com.yahoo.elide.datastores.aggregation.metadata.models.TimeDimension;
import com.yahoo.elide.datastores.aggregation.time.TimeGrain;

/**
 * Represents a requested time dimension in a query.
 */
public interface TimeDimensionProjection extends DimensionProjection {
    TimeDimension getDimension();
    TimeGrain getProjectedGrain();

    default TimeDimensionProjection toTimeGrain(TimeGrain grain) {
        if(getDimension().getSupportedGrains().stream().noneMatch(g -> g.getGrain().equals(grain))) {
            throw new InvalidValueException(getDimension().getId() + " doesn't support grain " + grain);
        }

        TimeDimensionProjection projection = this;
        return new TimeDimensionProjection() {
            @Override
            public TimeDimension getDimension() {
                return projection.getDimension();
            }

            @Override
            public TimeGrain getProjectedGrain() {
                return grain;
            }

            @Override
            public String getAlias() {
                return projection.getAlias();
            }
        };
    }
}
