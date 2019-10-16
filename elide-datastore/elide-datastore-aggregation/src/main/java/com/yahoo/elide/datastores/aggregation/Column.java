/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation;

import com.yahoo.elide.datastores.aggregation.annotation.Meta;
import com.yahoo.elide.datastores.aggregation.dimension.Dimension;
import com.yahoo.elide.datastores.aggregation.metric.Metric;

import com.yahoo.elide.datastores.aggregation.schema.Schema;
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
    @Getter
    protected final String columnName;

    /**
     * Constructor.
     *
     * @param schema The schema this {@link Column} belongs to.
     * @param field  The entity field or relation that this {@link Column} represents
     * @param annotation  Provides static meta data about this {@link Column}
     * @param fieldType  The Java type for this entity field or relation
     *
     * @throws NullPointerException if {@code field} or {@code fieldType} is {@code null}
     */
    public Column(Schema schema, String field, Meta annotation, Class<?> fieldType) {
        this(schema, field, annotation, fieldType, field);
    }

    public Column(Schema schema, String field, Meta annotation, Class<?> fieldType, String columnName) {
        this.name = Objects.requireNonNull(field, "field");
        this.longName = annotation == null || annotation.longName().isEmpty() ? field : annotation.longName();
        this.description = annotation == null || annotation.description().isEmpty() ? field : annotation.description();
        this.dataType = Objects.requireNonNull(fieldType, "fieldType");
        this.schema = schema;
        this.columnName = Objects.requireNonNull(columnName, "columnName");
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
