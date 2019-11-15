/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.query;

import com.yahoo.elide.core.exceptions.InvalidValueException;
import com.yahoo.elide.datastores.aggregation.metadata.models.Column;
import com.yahoo.elide.datastores.aggregation.metadata.models.TimeDimension;
import com.yahoo.elide.datastores.aggregation.time.TimeGrain;

/**
 * Represents a requested time dimension in a query.
 */
public interface TimeColumnProjection extends ColumnProjection {
    TimeDimension getTimeColumn();
    TimeGrain getGrain();

    /**
     * Convert this projection to a new time grain.
     *
     * @param newGrain new time grain
     * @return a new projection
     */
    default TimeColumnProjection toTimeGrain(TimeGrain newGrain) {
        if (getTimeColumn().getSupportedGrains().stream().noneMatch(g -> g.getGrain().equals(newGrain))) {
            throw new InvalidValueException(getTimeColumn().getId() + " doesn't support grain " + newGrain);
        }

        TimeColumnProjection projection = this;
        return new TimeColumnProjection() {
            @Override
            public TimeDimension getTimeColumn() {
                return projection.getTimeColumn();
            }

            @Override
            public TimeGrain getGrain() {
                return newGrain;
            }

            @Override
            public String getAlias() {
                return projection.getAlias();
            }
        };
    }

    @Override
    default Column getColumn() {
        return getTimeColumn();
    }
}
