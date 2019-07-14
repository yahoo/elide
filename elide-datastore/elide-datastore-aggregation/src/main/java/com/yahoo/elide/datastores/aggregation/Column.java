/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation;

import com.yahoo.elide.datastores.aggregation.annotation.Meta;
import com.yahoo.elide.datastores.aggregation.dimension.Dimension;
import com.yahoo.elide.datastores.aggregation.metric.Metric;

import lombok.Getter;

import java.util.Objects;

/**
 * Base class that offers common components between {@link Metric} and {@link Dimension}.
 */
public abstract class Column {

    @Getter
    protected final String name;
    @Getter
    protected final String longName;
    @Getter
    protected final String description;
    @Getter
    protected final Class<?> dataType;
    @Getter
    protected final Schema schema;

    /**
     * Constructor.
     *
     * @param field  The entity field or relation that this {@link Column} represents
     * @param annotation  Provides static meta data about this {@link Column}
     * @param fieldType  The Java type for this entity field or relation
     *
     * @throws NullPointerException if {@code field} or {@code fieldType} is {@code null}
     */
    public Column(Schema schema, String field, Meta annotation, Class<?> fieldType) {
        this.name = Objects.requireNonNull(field, "field");
        this.longName = annotation == null || annotation.longName().isEmpty() ? field : annotation.longName();
        this.description = annotation == null || annotation.description().isEmpty() ? field : annotation.description();
        this.dataType = Objects.requireNonNull(fieldType, "fieldType");
        this.schema = schema;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        final Column column = (Column) other;

        return getName().equals(column.getName())
                && getLongName().equals(column.getLongName())
                && getDescription().equals(column.getDescription())
                && getDataType().equals(column.getDataType());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getLongName(), getDescription(), getDataType());
    }
}
