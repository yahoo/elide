/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.engine.schema;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.filter.FilterPredicate;
import com.yahoo.elide.datastores.aggregation.dimension.Dimension;
import com.yahoo.elide.datastores.aggregation.dimension.TimeDimension;
import com.yahoo.elide.datastores.aggregation.engine.annotation.FromSubquery;
import com.yahoo.elide.datastores.aggregation.engine.annotation.FromTable;
import com.yahoo.elide.datastores.aggregation.engine.annotation.JoinTo;
import com.yahoo.elide.datastores.aggregation.schema.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import javax.persistence.Column;
import javax.persistence.JoinColumn;

/**
 * A subclass of Schema that supports additional metadata to construct the FROM clause of a SQL query.
 */
@EqualsAndHashCode
@ToString
public class SQLSchema extends Schema {

    @Getter
    private boolean isSubquery;

    @Getter
    private String tableDefinition;

    public SQLSchema(Class<?> entityClass, EntityDictionary dictionary) {
        super(entityClass, dictionary);

        isSubquery = false;

        FromTable fromTable = dictionary.getAnnotation(entityClass, FromTable.class);

        if (fromTable != null) {
            tableDefinition = fromTable.name();
        } else {
            FromSubquery fromSubquery = dictionary.getAnnotation(entityClass, FromSubquery.class);

            if (fromSubquery != null) {
                tableDefinition = "(" + fromSubquery.sql() + ")";

                isSubquery = true;
            } else {
                throw new IllegalStateException("Entity is missing FromTable or FromSubquery annotations");
            }
        }
    }

    @Override
    protected Dimension constructDimension(String dimensionField, Class<?> cls, EntityDictionary entityDictionary) {
        Dimension dim = super.constructDimension(dimensionField, cls, entityDictionary);

        JoinTo joinTo = entityDictionary.getAttributeOrRelationAnnotation(cls, JoinTo.class, dimensionField);

        if (joinTo == null) {
            String columnName = getColumnName(entityClass, dimensionField);

            if (dim instanceof TimeDimension) {
                return new SQLTimeDimension(dim, columnName, getAlias());
            }
            return new SQLDimension(dim, columnName, getAlias());
        }

        Path path = new Path(entityClass, entityDictionary, joinTo.path());

        if (dim instanceof TimeDimension) {
            return new SQLTimeDimension(dim, getJoinColumn(path), getJoinTableAlias(path), path);
        }
        return new SQLDimension(dim, getJoinColumn(path), getJoinTableAlias(path), path);
    }

    public static String getColumnName(EntityDictionary entityDictionary, Class<?> clazz, String fieldName) {
        Column[] column = entityDictionary.getAttributeOrRelationAnnotations(clazz, Column.class, fieldName);

        JoinColumn[] joinColumn = entityDictionary.getAttributeOrRelationAnnotations(clazz,
                JoinColumn.class, fieldName);

        if (column == null || column.length == 0) {
            if (joinColumn == null || joinColumn.length == 0) {
                return fieldName;
            } else {
                return joinColumn[0].name();
            }
        } else {
            return column[0].name();
        }
    }

    /**
     * Returns the physical database column name of an entity field.
     * @param clazz The entity which owns the field.
     * @param fieldName The field name to lookup
     * @return
     */
    private String getColumnName(Class<?> clazz, String fieldName) {
        return getColumnName(entityDictionary, clazz, fieldName);
    }

    private String getJoinColumn(Path path) {
        Path.PathElement last = path.lastElement().get();
        Class<?> lastClass = last.getType();

        return getColumnName(lastClass, last.getFieldName());
    }

    private String getJoinTableAlias(Path path) {
        Path.PathElement last = path.lastElement().get();
        Class<?> lastClass = last.getType();

        return FilterPredicate.getTypeAlias(lastClass);
    }
}
