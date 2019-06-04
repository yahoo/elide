/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.dimension;

import com.yahoo.elide.datastores.aggregation.annotation.CardinalitySize;

import java.util.Objects;
import java.util.StringJoiner;

/**
 * A dimension backed by a table.
 * <p>
 * {@link EntityDimension} is thread-safe and can be accessed by multiple threads.
 */
public class EntityDimension extends AbstractDimension {

    private static final long serialVersionUID = 4383532465610358102L;

    /**
     * Constructor.
     *
     * @param name  The name of the entity representing this {@link Dimension} object
     * @param longName  The a human-readable name (allowing spaces) of this {@link Dimension} object
     * @param description  A short description explaining the meaning of this {@link Dimension}
     * @param dataType  The entity field type of this {@link Dimension}
     * @param cardinality  The estimated cardinality of this {@link Dimension}
     * @param friendlyName  A human displayable column for this {@link Dimension}
     *
     * @throws NullPointerException is any one of the arguments is {@code null}
     */
    public EntityDimension(
            final String name,
            final String longName,
            final String description,
            final Class<?> dataType,
            final CardinalitySize cardinality,
            final String friendlyName
    ) {
        super(
                Objects.requireNonNull(name, "name"),
                Objects.requireNonNull(longName, "longName"),
                Objects.requireNonNull(description, "description"),
                DefaultDimensionType.TABLE,
                Objects.requireNonNull(dataType, "dataType"),
                Objects.requireNonNull(cardinality, "cardinality"),
                Objects.requireNonNull(friendlyName, "friendlyName")
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

        final EntityDimension that = (EntityDimension) other;
        return getName().equals(that.getName())
                && getLongName().equals(that.getLongName())
                && getDescription().equals(that.getDescription())
                && getDimensionType().equals(that.getDimensionType())
                && getDataType().equals(that.getDataType())
                && getCardinality().equals(that.getCardinality())
                && getFriendlyName().equals(that.getFriendlyName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                getName(),
                getLongName(),
                getDescription(),
                getDimensionType(),
                getDataType(),
                getCardinality(),
                getFriendlyName()
        );
    }

    /**
     * Returns the string representation of this {@link Dimension}.
     * <p>
     * The string consists of values of all fields in the format
     * "EntityDimension[name='XXX', longName='XXX', description='XXX', dimensionType=XXX, dataType=XXX, cardinality=XXX,
     * friendlyName=XXX]", where values can be programmatically fetched via getters.
     * <p>
     * {@code dataType} are printed in its {@link Class#getSimpleName() simple name}.
     * <p>
     * Note that there is a single space separating each value pair.
     *
     * @return serialized {@link EntityDimension}
     */
    @Override
    public String toString() {
        return new StringJoiner(", ", EntityDimension.class.getSimpleName() + "[", "]")
                .add("name='" + getName() + "'")
                .add("longName='" + getLongName() + "'")
                .add("description='" + getDescription() + "'")
                .add("dimensionType=" + getDimensionType())
                .add("dataType=" + getDataType().getSimpleName())
                .add("cardinality=" + getCardinality())
                .add("friendlyName='" + getFriendlyName() + "'")
                .toString();
    }
}
