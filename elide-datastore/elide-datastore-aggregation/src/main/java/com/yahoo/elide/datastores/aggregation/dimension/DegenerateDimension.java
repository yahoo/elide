/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.dimension;

import com.yahoo.elide.datastores.aggregation.annotation.CardinalitySize;

import lombok.Getter;

import java.util.Objects;
import java.util.StringJoiner;

/**
 * A {@link Dimension} backed by a table column.
 * <p>
 * {@link DegenerateDimension} is thread-safe and can be accessed by multiple threads.
 */
public class DegenerateDimension extends AbstractDimension {

    private static final long serialVersionUID = 370506626352850507L;

    @Getter
    ColumnType columnType;

    /**
     * Constructor.
     *
     * @param name  The name of the entity representing this {@link Dimension} object
     * @param longName  The a human-readable name (allowing spaces) of this {@link Dimension} object
     * @param description  A short description explaining the meaning of this {@link Dimension}
     * @param dataType  The entity field type of this {@link Dimension}
     * @param cardinality  The estimated cardinality of this {@link Dimension}
     * @param friendlyName  A human displayable column for this {@link Dimension}
     * @param columnType  An object that represents one of the allowed types for a SQL table column backing
     * {@link Dimension}
     *
     * @throws NullPointerException is any one of the arguments is {@code null}
     */
    public DegenerateDimension(
            final String name,
            final String longName,
            final String description,
            final Class<?> dataType,
            final CardinalitySize cardinality,
            final String friendlyName,
            final ColumnType columnType
    ) {
        super(
                Objects.requireNonNull(name, "name"),
                Objects.requireNonNull(longName, "longName"),
                Objects.requireNonNull(description, "description"),
                DefaultDimensionType.DEGENERATE,
                Objects.requireNonNull(dataType, "dataType"),
                Objects.requireNonNull(cardinality, "cardinality"),
                Objects.requireNonNull(friendlyName, "friendlyName")
        );
        this.columnType = Objects.requireNonNull(columnType);
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        if (!super.equals(other)) { return false; }
        final DegenerateDimension that = (DegenerateDimension) other;
        return getColumnType().equals(that.getColumnType());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getColumnType());
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", DegenerateDimension.class.getSimpleName() + "[", "]")
                .add("columnType=" + columnType)
                .add("name='" + name + "'")
                .add("longName='" + longName + "'")
                .add("description='" + description + "'")
                .add("dimensionType=" + dimensionType)
                .add("dataType=" + dataType)
                .add("cardinality=" + cardinality)
                .add("friendlyName='" + friendlyName + "'")
                .toString();
    }
}
