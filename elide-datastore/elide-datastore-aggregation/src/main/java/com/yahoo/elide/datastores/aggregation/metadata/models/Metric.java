/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata.models;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.datastores.aggregation.annotation.Meta;
import com.yahoo.elide.datastores.aggregation.annotation.MetricAggregation;
import com.yahoo.elide.datastores.aggregation.annotation.MetricFormula;
import com.yahoo.elide.datastores.aggregation.metadata.enums.Format;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import javax.persistence.ManyToOne;

/**
 * Special column for AnalyticView which supports aggregation.
 */
@EqualsAndHashCode(callSuper = true)
@Include(type = "metric")
@Data
public class Metric extends Column {
    private Format defaultFormat;

    @ManyToOne
    @ToString.Exclude
    private MetricFunction metricFunction;

    public Metric(Class<?> tableClass, String fieldName, EntityDictionary dictionary) {
        super(tableClass, fieldName, dictionary);

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
                this.metricFunction = resolveFormula(tableClass, fieldName, meta, dictionary);
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
            metricFunction.setExpression(String.format(
                    metricFunction.getExpression(),
                    dictionary.getAnnotatedColumnName(tableClass, fieldName)));

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
     * Resolve aggregation function form {@link MetricFormula} annotation.
     *
     * @param tableClass table class
     * @param fieldName metric field name
     * @param meta meta annotation on the field
     * @param dictionary dictionary with entity information
     * @return resolved metric function instance
     */
    private static MetricFunction resolveFormula(Class<?> tableClass,
                                                 String fieldName,
                                                 Meta meta,
                                                 EntityDictionary dictionary) {
        return resolveFormula(tableClass, fieldName, new LinkedHashSet<>(), new HashMap<>(), meta, dictionary);
    }

    /**
     * Resolve aggregation function form {@link MetricFormula} annotation.
     * This require traverse through the formula to resolve all references to other metric field.
     *
     * @param tableClass table class
     * @param fieldName metric field name
     * @param toResolve references that are not resolved yet, to detect cycle-reference
     * @param resolved references that are already resolved
     * @param meta meta annotation on the field
     * @param dictionary dictionary with entity information
     * @return resolved metric function instance
     */
    private static MetricFunction resolveFormula(Class<?> tableClass,
                                String fieldName,
                                LinkedHashSet<String> toResolve,
                                Map<String, MetricFunction> resolved,
                                Meta meta,
                                EntityDictionary dictionary) {
        if (toResolve.contains(fieldName)) {
            throw new IllegalArgumentException("Metric formula reference loop found in class "
                    + dictionary.getJsonAliasFor(tableClass) + ": "
                    + String.join("->", toResolve) + "->" + fieldName);
        }

        MetricFormula formula = dictionary.getAttributeOrRelationAnnotation(
                tableClass,
                MetricFormula.class,
                fieldName);

        // if the metric is directly aggregation, add it into the resolved map
        if (formula == null) {
            resolved.put(fieldName, new Metric(tableClass, fieldName, dictionary).getMetricFunction());
        } else {
            toResolve.add(fieldName);

            String expression = formula.expression();
            String[] references = formula.references();
            Set<FunctionArgument> arguments = new HashSet<>();

            // if the metric is formula, resolve all references it uses
            for (int i = 0; i < references.length; i++) {
                String ref = references[i];

                if (!resolved.containsKey(ref)) {
                    resolveFormula(tableClass, ref, toResolve, resolved, null, dictionary);
                }

                MetricFunction referenceFunction = resolved.get(ref);

                if (referenceFunction.getExpression() == null || referenceFunction.getExpression().equals("")) {
                    throw new IllegalArgumentException(
                            "Metric formula contains metric function that doesn't have expression "
                            + dictionary.getJsonAliasFor(tableClass) + ": "
                            + String.join("->", toResolve) + "->" + ref);
                }

                // replace reference with expression
                final int index = i + 1;
                expression = expression.replace("{%" + index + "}", referenceFunction.getExpression());

                // pass metric argument into arguments of current function
                // TODO: add argument name mapping so that arguments can be injected correctly
                referenceFunction.getArguments()
                        .forEach(arg -> arguments.add(
                                new FunctionArgument(
                                        String.format("%s_%d_%s", ref, index, arg.getId()),
                                        arg.getName(),
                                        arg.getDescription(),
                                        arg.getDataType())));
            }

            toResolve.remove(fieldName);
            // add new function into resolved map
            resolved.put(
                    fieldName,
                    new MetricFunction(
                            constructColumnName(tableClass, fieldName, dictionary) + "[" + fieldName + "]",
                            meta == null ? null : meta.longName(),
                            meta == null ? null : meta.description(),
                            expression,
                            arguments));
        }

        return resolved.get(fieldName);
    }
}
