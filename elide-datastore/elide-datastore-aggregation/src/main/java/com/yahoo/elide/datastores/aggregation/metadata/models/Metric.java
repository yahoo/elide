/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata.models;

import static com.yahoo.elide.utils.TypeHelper.getFieldAlias;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.datastores.aggregation.annotation.Meta;
import com.yahoo.elide.datastores.aggregation.annotation.MetricAggregation;
import com.yahoo.elide.datastores.aggregation.annotation.MetricFormula;
import com.yahoo.elide.datastores.aggregation.metadata.enums.Format;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLReferenceResolver;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLReferenceTable;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.ManyToOne;

/**
 * Column which supports aggregation.
 */
@EqualsAndHashCode(callSuper = true)
@Include(type = "metric")
@Data
public class Metric extends Column {
    private Format defaultFormat;

    @ManyToOne
    @ToString.Exclude
    private MetricFunction metricFunction;

    public Metric(Table table, String fieldName, EntityDictionary dictionary) {
        super(table, fieldName, dictionary);
        Class<?> tableClass = dictionary.getEntityClass(table.getId());

        MetricAggregation aggregation = dictionary.getAttributeOrRelationAnnotation(
                tableClass,
                MetricAggregation.class,
                fieldName);

        Meta meta = dictionary.getAttributeOrRelationAnnotation(
                tableClass,
                Meta.class,
                fieldName);

        if (aggregation != null) {
            this.metricFunction = resolveAggregation(tableClass, fieldName, aggregation, meta, dictionary);
        } else {
            MetricFormula formula = dictionary.getAttributeOrRelationAnnotation(
                    tableClass,
                    MetricFormula.class,
                    fieldName);

            if (formula != null) {
                this.metricFunction = constructMetricFunction(
                        constructColumnName(tableClass, fieldName, dictionary) + "[" + fieldName + "]",
                        meta == null ? null : meta.longName(),
                        meta == null ? null : meta.description(),
                        formula.value(),
                        new HashSet<>());

            } else {
                throw new IllegalArgumentException("Trying to construct metric field "
                        + getId() + " without @MetricAggregation and @MetricFormula.");
            }
        }
    }

    /**
     * Resolve aggregation function from {@link MetricAggregation} annotation.
     *
     * @param tableClass table class
     * @param fieldName metric field name
     * @param aggregation aggregation annotation on the field
     * @param meta meta annotation on the field
     * @param dictionary dictionary with entity information
     * @return resolved metric function instance
     */
    private static MetricFunction resolveAggregation(Class<?> tableClass,
                                                     String fieldName,
                                                     MetricAggregation aggregation,
                                                     Meta meta,
                                                     EntityDictionary dictionary) {
        String columnName = constructColumnName(tableClass, fieldName, dictionary);
        try {
            MetricFunction metricFunction = aggregation.function().newInstance();
            metricFunction.setName(columnName + "[" + metricFunction.getName() + "]");

            if (meta != null) {
                metricFunction.setLongName(meta.longName());
                metricFunction.setDescription(meta.description());
            }

            return metricFunction;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalArgumentException("Can't initialize function for metric " + columnName);
        }
    }

    /**
     * Dynamically construct a metric function
     *
     * @param id metric function id
     * @param longName meta long name
     * @param description meta description
     * @param expression expression string
     * @param arguments function arguments
     * @return a metric function instance
     */
    protected MetricFunction constructMetricFunction(String id,
                                                     String longName,
                                                     String description,
                                                     String expression,
                                                     Set<FunctionArgument> arguments) {
        return new MetricFunction(id, longName, description, expression, arguments);
    }

    /**
     * Build a resolver for {@link MetricAggregation} metric field
     *
     * @param tableClass table class
     * @param fieldName metric field name
     * @return a resolver
     */
    private SQLReferenceResolver getAggregationResolver(Class<?> tableClass, String fieldName) {
        return new SQLReferenceResolver(this) {
            @Override
            public String resolveReference(SQLReferenceTable referenceTable, String tableAlias) {
                return String.format(
                        getMetricFunction().getExpression(),
                        getFieldAlias(
                                tableAlias,
                                referenceTable.getDictionary().getAnnotatedColumnName(tableClass, fieldName)));
            }
        };
    }
}
