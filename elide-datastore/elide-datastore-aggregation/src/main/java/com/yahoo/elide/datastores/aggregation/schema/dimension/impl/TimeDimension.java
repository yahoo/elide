/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.schema.dimension.impl;

import com.yahoo.elide.datastores.aggregation.annotation.CardinalitySize;
import com.yahoo.elide.datastores.aggregation.annotation.Grain;
import com.yahoo.elide.datastores.aggregation.annotation.Meta;
import com.yahoo.elide.datastores.aggregation.query.ProjectedDimension;
import com.yahoo.elide.datastores.aggregation.schema.Schema;
import com.yahoo.elide.datastores.aggregation.schema.dimension.ColumnType;
import com.yahoo.elide.datastores.aggregation.schema.dimension.TimeDimensionColumn;

import lombok.Getter;

import java.util.Arrays;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.TimeZone;
import java.util.stream.Collectors;

/**
 * A {@link ProjectedDimension} backed by a temporal column.
 * <p>
 * {@link TimeDimension} is thread-safe and can be accessed by multiple threads.
 */
public class TimeDimension extends DegenerateDimension implements TimeDimensionColumn {

    private static final long serialVersionUID = -8864179100604940730L;

    @Getter
    private final TimeZone timeZone;
    @Getter
    private Grain[] supportedGrains;

    /**
     * Constructor.
     *
     * @param schema The schema this dimension belongs to
     * @param dimensionField  The entity field or relation that this {@link ProjectedDimension} represents
     * @param annotation  Provides static meta data about this {@link ProjectedDimension}
     * @param fieldType  The Java type for this entity field or relation
     * @param cardinality  The estimated cardinality of this {@link ProjectedDimension} in SQL table
     * @param friendlyName  A human-readable name representing this {@link ProjectedDimension}
     * @param timeZone  The timezone describing the data of this {@link ProjectedDimension}
     * @param supportedGrains A list of supported time grains.
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
            Grain[] supportedGrains
    ) {
        super(schema, dimensionField, annotation, fieldType, cardinality, friendlyName, ColumnType.TEMPORAL);
        this.timeZone = Objects.requireNonNull(timeZone, "timeZone");
        this.supportedGrains = supportedGrains;
    }

    @Override
    public Grain[] getSupportedGrains() {
        return supportedGrains;
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
                && getSupportedGrains().equals(that.getSupportedGrains());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getTimeZone(), getSupportedGrains());
    }

    /**
     * Returns the string representation of this {@link ProjectedDimension}.
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
                .add("timeGrains=" + Arrays.stream(getSupportedGrains()).map(Grain::grain).collect(Collectors.toSet()))
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
