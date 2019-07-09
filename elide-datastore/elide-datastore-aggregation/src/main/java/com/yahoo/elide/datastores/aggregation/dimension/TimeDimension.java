/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.dimension;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.datastores.aggregation.time.TimeGrain;

import lombok.Getter;

import java.util.Objects;
import java.util.StringJoiner;
import java.util.TimeZone;

/**
 * A {@link Dimension} backed by a temporal column.
 * <p>
 * {@link TimeDimension} is thread-safe and can be accessed by multiple threads.
 */
public class TimeDimension extends DegenerateDimension {

    private static final long serialVersionUID = -8864179100604940730L;

    @Getter
    private final TimeZone timeZone;
    @Getter
    private final TimeGrain timeGrain;

    public TimeDimension(
            String dimensionField,
            Class<?> cls,
            EntityDictionary entityDictionary,
            TimeZone timeZone,
            TimeGrain timeGrain
    ) {
        super(dimensionField, cls, entityDictionary, ColumnType.TEMPORAL);
        this.timeZone = Objects.requireNonNull(timeZone, "timeZone");
        this.timeGrain = timeGrain;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        if (!super.equals(other)) {
            return false;
        }

        final TimeDimension that = (TimeDimension) other;

        return getTimeZone().equals(that.getTimeZone())
                && getTimeGrain().equals(that.getTimeGrain());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getTimeZone(), getTimeGrain());
    }

    /**
     * Returns the string representation of this {@link Dimension}.
     * <p>
     * The string consists of values of all fields in the format
     * "EntityDimension[timeZone=XXX, timeGrain=XXX, columnType=XXX, name='XXX', longName='XXX', description='XXX',
     * dimensionType=XXX, dataType=XXX, cardinality=XXX, friendlyName=XXX, columnType=XXX]", where values can be
     * programmatically fetched via getters.
     * <p>
     * {@code dataType} are printed in its {@link Class#getSimpleName() simple name}. {@code timeZone} is printed in its
     * {@link TimeZone#getDisplayName() display name}.
     * <p>
     * Note that there is a single space separating each value pair.
     *
     * @return serialized {@link EntityDimension}
     */
    @Override
    public String toString() {
        return new StringJoiner(", ", TimeDimension.class.getSimpleName() + "[", "]")
                .add("timeZone=" + getTimeZone().getDisplayName())
                .add("timeGrain=" + getTimeGrain())
                .add("columnType=" + getColumnType())
                .add("name='" + getName() + "'")
                .add("longName='" + getLongName() + "'")
                .add("description='" + getDescription() + "'")
                .add("dimensionType=" + getDimensionType())
                .add("dataType=" + getDataType())
                .add("cardinality=" + getCardinality())
                .add("friendlyName='" + getFriendlyName() + "'")
                .toString();
    }
}
