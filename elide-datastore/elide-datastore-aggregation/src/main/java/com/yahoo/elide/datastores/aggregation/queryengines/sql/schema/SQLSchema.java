/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql.schema;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.filter.FilterPredicate;
import com.yahoo.elide.datastores.aggregation.annotation.Meta;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromSubquery;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromTable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.JoinTo;
import com.yahoo.elide.datastores.aggregation.schema.Schema;
import com.yahoo.elide.datastores.aggregation.schema.dimension.DimensionColumn;
import com.yahoo.elide.datastores.aggregation.schema.dimension.TimeDimensionColumn;
import com.yahoo.elide.datastores.aggregation.schema.metric.Aggregation;
import com.yahoo.elide.datastores.aggregation.schema.metric.Metric;

import org.hibernate.annotations.Subselect;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.List;
import javax.persistence.Column;
import javax.persistence.JoinColumn;
import javax.persistence.Table;

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
    protected DimensionColumn constructDimension(String dimensionField,
                                                 Class<?> cls, EntityDictionary entityDictionary) {
        DimensionColumn dim = super.constructDimension(dimensionField, cls, entityDictionary);

        JoinTo joinTo = entityDictionary.getAttributeOrRelationAnnotation(cls, JoinTo.class, dimensionField);

        if (joinTo == null) {
            String columnName = getColumnName(entityClass, dimensionField);

            if (dim instanceof TimeDimensionColumn) {
               return new SQLTimeDimensionColumn((TimeDimensionColumn) dim, columnName, getAlias());
            }
            return new SQLDimensionColumn(dim, columnName, getAlias());
        }

        Path path = new Path(entityClass, entityDictionary, joinTo.path());

        if (dim instanceof TimeDimensionColumn) {
            return new SQLTimeDimensionColumn((TimeDimensionColumn) dim,
                    getJoinColumn(path), getJoinTableAlias(path), path);
        }
        return new SQLDimensionColumn(dim, getJoinColumn(path), getJoinTableAlias(path), path);
    }

    /**
     * Constructs a new {@link SQLMetric} instance.
     *
     * @param metricField  The entity field of the metric being constructed
     * @param cls  The entity that contains the metric being constructed
     * @param entityDictionary  The auxiliary object that offers binding info used to construct this {@link SQLMetric}
     *
     * @return a {@link SQLMetric}
     */
    @Override
    protected Metric constructMetric(String metricField, Class<?> cls, EntityDictionary entityDictionary) {
        Meta metaData = entityDictionary.getAttributeOrRelationAnnotation(cls, Meta.class, metricField);
        Class<?> fieldType = entityDictionary.getType(cls, metricField);

        List<Class<? extends Aggregation>> aggregations = getAggregations(metricField, cls, entityDictionary);

        return new SQLMetric(
                this,
                metricField,
                metaData,
                fieldType,
                aggregations,
                getColumnName(entityDictionary, cls, metricField)
        );
    }

    /**
     * Maps a logical entity attribute into a physical SQL column name.
     * @param entityDictionary The dictionary for this elide instance.
     * @param cls The entity class.
     * @param fieldName The entity attribute.
     * @return The physical SQL column name.
     */
    public static String getColumnName(EntityDictionary entityDictionary, Class<?> cls, String fieldName) {
        Column[] column = entityDictionary.getAttributeOrRelationAnnotations(cls, Column.class, fieldName);

        // this would only be valid for dimension columns
        JoinColumn[] joinColumn = entityDictionary.getAttributeOrRelationAnnotations(cls,
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
     * Maps an entity class to a physical table of subselect query, if neither {@link Table} nor {@link Subselect}
     * annotation is present on this class, use the class alias as default.
     *
     * @param entityDictionary The dictionary for this elide instance.
     * @param cls The entity class.
     * @return The physical SQL table or subselect query.
     */
    public static String getTableOrSubselect(EntityDictionary entityDictionary, Class<?> cls) {
        Subselect subselectAnnotation = entityDictionary.getAnnotation(cls, Subselect.class);

        if (subselectAnnotation == null) {
            Table tableAnnotation = entityDictionary.getAnnotation(cls, Table.class);

            return (tableAnnotation == null)
                    ? entityDictionary.getJsonAliasFor(cls)
                    : tableAnnotation.name();
        } else {
            return "(" + subselectAnnotation.value() + ")";
        }
    }

    /**
     * Returns the physical database column name of an entity field.
     * @param cls The entity which owns the field.
     * @param fieldName The field name to lookup
     * @return physical database column name of an entity field
     */
    private String getColumnName(Class<?> cls, String fieldName) {
        return getColumnName(entityDictionary, cls, fieldName);
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
