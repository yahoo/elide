/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.dimension;

import com.yahoo.elide.datastores.aggregation.Schema;
import com.yahoo.elide.datastores.aggregation.annotation.CardinalitySize;
import com.yahoo.elide.datastores.aggregation.annotation.Meta;
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

    /**
     * Constructor.
     *
     * @param dimensionField  The entity field or relation that this {@link Dimension} represents
     * @param annotation  Provides static meta data about this {@link Dimension}
     * @param fieldType  The Java type for this entity field or relation
     * @param cardinality  The estimated cardinality of this {@link Dimension} in SQL table
     * @param friendlyName  A human-readable name representing this {@link Dimension}
     * @param timeZone  The timezone describing the data of this {@link Dimension}
     * @param timeGrain  The finest unit into which this temporal {@link Dimension} column can be divided
     *
     * @throws NullPointerException any argument, except for {@code annotation}, is {@code null}
     */
    public TimeDimension(
            Schema schema,
            String dimensionField,
            Meta annotation,
            Class<?> fieldType,
            CardinalitySize cardinality,
            String friendlyName,
            TimeZone timeZone,
            TimeGrain timeGrain
    ) {
        super(schema, dimensionField, annotation, fieldType, cardinality, friendlyName, ColumnType.TEMPORAL);
        this.timeZone = Objects.requireNonNull(timeZone, "timeZone");
        this.timeGrain = Objects.requireNonNull(timeGrain, "timeGrain");
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
