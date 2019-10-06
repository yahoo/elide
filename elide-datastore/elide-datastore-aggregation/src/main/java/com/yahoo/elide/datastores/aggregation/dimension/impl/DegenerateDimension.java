/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.dimension.impl;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.datastores.aggregation.annotation.CardinalitySize;
import com.yahoo.elide.datastores.aggregation.annotation.Meta;

import com.yahoo.elide.datastores.aggregation.dimension.ColumnType;
import com.yahoo.elide.datastores.aggregation.dimension.Dimension;
import com.yahoo.elide.datastores.aggregation.dimension.DimensionType;
import com.yahoo.elide.datastores.aggregation.schema.Schema;
import lombok.Getter;

import java.util.Objects;
import java.util.StringJoiner;

/**
 * A {@link Dimension} backed by a table column.
 * <p>
 * {@link DegenerateDimension} is thread-safe and can be accessed by multiple threads.
 */
public class DegenerateDimension extends EntityDimension {

    /**
     * Returns the nature of a SQL column that is backing a specified dimension field in an entity.
     *
     * @param dimensionField  The provided dimension field
     * @param cls  The entity having the {@code dimensionField}
     * @param entityDictionary  Object that helps to find the type info about entity field
     *
     * @return a {@link ColumnType}, such as PK or a regular field
     */
    public static ColumnType parseColumnType(String dimensionField, Class<?> cls, EntityDictionary entityDictionary) {
        if (entityDictionary.getIdFieldName(cls).equals(dimensionField)) {
            return ColumnType.PRIMARY_KEY;
        } else {
            return ColumnType.FIELD;
        }
    }

    private static final long serialVersionUID = 370506626352850507L;

    @Getter
    ColumnType columnType;

    /**
     * Constructor.
     *
     * @param schema The schema this {@link Dimension} belongs to.
     * @param dimensionField  The entity field or relation that this {@link Dimension} represents
     * @param annotation  Provides static meta data about this {@link Dimension}
     * @param fieldType  The Java type for this entity field or relation
     * @param cardinality  The estimated cardinality of this {@link Dimension} in SQL table
     * @param friendlyName  A human-readable name representing this {@link Dimension}
     * @param columnType  The type of the SQL column mapping this {@link Dimension}
     *
     * @throws NullPointerException any argument, except for {@code annotation}, is {@code null}
     */
    public DegenerateDimension(
            Schema schema,
            String dimensionField,
            Meta annotation,
            Class<?> fieldType,
            CardinalitySize cardinality,
            String friendlyName,
            ColumnType columnType
    ) {
        super(
                schema,
                dimensionField,
                annotation,
                fieldType,
                DimensionType.DEGENERATE,
                Objects.requireNonNull(cardinality, "cardinality"),
                Objects.requireNonNull(friendlyName, "friendlyName")
        );
        this.columnType = Objects.requireNonNull(columnType, "columnType");
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

    /**
     * Returns the string representation of this {@link Dimension}.
     * <p>
     * The string consists of values of all fields in the format
     * "DegenerateDimension[columnType=XXX, name='XXX', longName='XXX', description='XXX', dimensionType=XXX,
     * dataType=XXX, cardinality=XXX, friendlyName=XXX]", where values can be programmatically fetched via getters.
     * <p>
     * {@code dataType} are printed in its {@link Class#getSimpleName() simple name}.
     * <p>
     * Note that there is a single space separating each value pair.
     *
     * @return serialized {@link DegenerateDimension}
     */
    @Override
    public String toString() {
        return new StringJoiner(", ", DegenerateDimension.class.getSimpleName() + "[", "]")
                .add("columnType=" + getColumnType())
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
