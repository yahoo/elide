/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.dimension;

import com.yahoo.elide.core.EntityDictionary;

import lombok.Getter;

import java.util.Objects;
import java.util.StringJoiner;

import javax.persistence.Id;

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
     *
     * @return a {@link ColumnType}, such as PK or a regular field
     */
    public static ColumnType parseColumnType(String dimensionField, Class<?> cls, EntityDictionary entityDictionary) {
        if (entityDictionary.attributeOrRelationAnnotationExists(cls, dimensionField, Id.class)) {
            return ColumnType.PRIMARY_KEY;
        } else {
            return ColumnType.FIELD;
        }
    }

    private static final long serialVersionUID = 370506626352850507L;

    @Getter
    ColumnType columnType;

    public DegenerateDimension(String dimensionField, Class<?> cls, EntityDictionary entityDictionary) {
        this(dimensionField, cls, entityDictionary, parseColumnType(dimensionField, cls, entityDictionary));
    }

    protected DegenerateDimension(
            String dimensionField,
            Class<?> cls,
            EntityDictionary entityDictionary,
            ColumnType columnType
    ) {
        super(dimensionField, cls, entityDictionary, DimensionType.DEGENERATE);
        this.columnType = columnType;
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
