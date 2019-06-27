/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.dimension;

import com.yahoo.elide.datastores.aggregation.annotation.CardinalitySize;
import com.yahoo.elide.datastores.aggregation.time.TimeGrain;

import lombok.Getter;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
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
    private final Set<TimeGrain> timeGrains;

    /**
     * Constructor.
     *
     * @param name  The name of the entity representing this {@link Dimension} object
     * @param longName  The a human-readable name (allowing spaces) of this {@link Dimension} object
     * @param description  A short description explaining the meaning of this {@link Dimension}
     * @param dataType  The entity field type of this {@link Dimension}
     * @param cardinality  The estimated cardinality of this {@link Dimension}
     * @param friendlyName  A human displayable column for this {@link Dimension}
     * @param timeZone  The default timezone of time data stored in this temporal column
     * @param timeGrains  All supported units into which this temporal column can be divided
     *
     * @throws NullPointerException is any one of the arguments is {@code null}
     */
    public TimeDimension(
            final String name,
            final String longName,
            final String description,
            final Class<?> dataType,
            final CardinalitySize cardinality,
            final String friendlyName,
            final TimeZone timeZone,
            final Set<TimeGrain> timeGrains
    ) {
        super(name, longName, description, dataType, cardinality, friendlyName, DefaultColumnType.TEMPORAL);
        this.timeZone = Objects.requireNonNull(timeZone, "timeZone");
        this.timeGrains = Collections.unmodifiableSet(
                new HashSet<>(Objects.requireNonNull(timeGrains, "timeGrains"))
        );
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
                && getTimeGrains().equals(that.getTimeGrains());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getTimeZone(), getTimeGrains());
    }

    /**
     * Returns the string representation of this {@link Dimension}.
     * <p>
     * The string consists of values of all fields in the format
     * "EntityDimension[timeZone=XXX, timeGrains=XXX, columnType=XXX, name='XXX', longName='XXX', description='XXX',
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
                .add("timeGrains=" + getTimeGrains())
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
