/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation;

import com.yahoo.elide.Injector;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.filter.FilterPredicate;
import com.yahoo.elide.datastores.aggregation.annotation.Metric;
import com.yahoo.elide.core.Path;
import com.yahoo.elide.datastores.aggregation.annotation.MetricAggregation;
import com.yahoo.elide.datastores.aggregation.annotation.MetricComputation;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromSubquery;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromTable;
import com.yahoo.elide.security.checks.Check;

import org.hibernate.annotations.Subselect;

import java.util.Map;
import javax.persistence.Column;
import javax.persistence.JoinColumn;

/**
 * Dictionary that supports more aggregation data store specific functionality
 */
public class AggregationDictionary extends EntityDictionary {
    public AggregationDictionary(Map<String, Class<? extends Check>> checks) {
        super(checks);
    }

    public AggregationDictionary(Map<String, Class<? extends Check>> checks, Injector injector) {
        super(checks, injector);
    }

    /**
     * Returns whether or not an entity field is a metric field.
     * <p>
     * A field is a metric field iff that field is annotated by at least one of
     * <ol>
     *     <li> {@link MetricAggregation}
     *     <li> {@link MetricComputation}
     * </ol>
     *
     * @param fieldName  The entity field
     *
     * @return {@code true} if the field is a metric field
     */
    public boolean isMetricField(Class<?> cls, String fieldName) {
        return attributeOrRelationAnnotationExists(cls, fieldName, Metric.class);
    }

    public static boolean isAnalyticView(Class<?> cls) {
        return cls.isAnnotationPresent(FromTable.class) || cls.isAnnotationPresent(FromSubquery.class);
    }

    /**
     * Maps a logical entity attribute into a physical SQL column name.
     * @param cls The entity class.
     * @param fieldName The entity attribute.
     * @return The physical SQL column name.
     */
    public String getColumnName(Class<?> cls, String fieldName) {
        Column[] column = getAttributeOrRelationAnnotations(cls, Column.class, fieldName);

        // this would only be valid for dimension columns
        JoinColumn[] joinColumn = getAttributeOrRelationAnnotations(cls, JoinColumn.class, fieldName);

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

    public String getJoinColumn(Path path) {
        Path.PathElement last = path.lastElement().get();
        Class<?> lastClass = last.getType();

        return getColumnName(lastClass, last.getFieldName());
    }

    public static String getClassAlias(Class<?> entityClass) {
        return FilterPredicate.getTypeAlias(entityClass);
    }

    /**
     * Maps an entity class to a physical table of subselect query, if neither {@link javax.persistence.Table}
     * nor {@link Subselect} annotation is present on this class, use the class alias as default.
     *
     * @param cls The entity class.
     * @return The physical SQL table or subselect query.
     */
    public String getTableOrSubselect(Class<?> cls) {
        Subselect subselectAnnotation = getAnnotation(cls, Subselect.class);

        if (subselectAnnotation == null) {
            javax.persistence.Table tableAnnotation =
                    getAnnotation(cls, javax.persistence.Table.class);

            return (tableAnnotation == null)
                    ? getJsonAliasFor(cls)
                    : tableAnnotation.name();
        } else {
            return "(" + subselectAnnotation.value() + ")";
        }
    }
}
