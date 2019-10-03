/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation;

import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.datastores.aggregation.dimension.Dimension;
import com.yahoo.elide.datastores.aggregation.dimension.TimeDimension;
import com.yahoo.elide.datastores.aggregation.filter.visitor.FilterConstraints;
import com.yahoo.elide.datastores.aggregation.filter.visitor.SplitFilterExpressionVisitor;
import com.yahoo.elide.datastores.aggregation.metric.Aggregation;
import com.yahoo.elide.datastores.aggregation.metric.Metric;
import com.yahoo.elide.datastores.aggregation.schema.Schema;
import com.yahoo.elide.request.Attribute;
import com.yahoo.elide.request.EntityProjection;
import com.yahoo.elide.request.Relationship;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Helper for Aggregation Data Store which does the work associated with extracting {@link Query}.
 */
public class AggregationDataStoreHelper {

    private static final int AGGREGATION_METHOD_INDEX = 0;

    private Schema schema;
    private EntityProjection entityProjection;
    private Set<Dimension> dimensions;
    private Set<TimeDimension> timeDimensions;
    private Map<Metric, Class<? extends Aggregation>> metricMap;
    private FilterExpression whereFilter;
    private FilterExpression havingFilter;

    public AggregationDataStoreHelper(Schema schema, RequestScope scope) {
        this.schema = schema;
        this.entityProjection = scope.getEntityProjection();
        dimensions = new LinkedHashSet<>();
        timeDimensions = new LinkedHashSet<>();
        getDimensionLists(dimensions, timeDimensions);
        Set<Metric> metrics = new LinkedHashSet<>();
        getMetricList(metrics);
        getMetricMap(metrics);
        getFilters(entityProjection.getFilterExpression());
    }

    /**
     * @return {@link Query} query object with all the parameters provided by user.
     */
    public Query getQuery() {
        return Query.builder()
                .schema(schema)
                .metrics(metricMap)
                .groupDimensions(dimensions)
                .timeDimensions(timeDimensions)
                .whereFilter(whereFilter)
                .havingFilter(havingFilter)
                .sorting(entityProjection.getSorting())
                .pagination(entityProjection.getPagination())
                .build();
    }

    /**
     * Gets whereFilter and havingFilter based on provided filter expression from {@link EntityProjection}.
     * @param filterExpression {@link FilterExpression} The filter expression to parse.
     */
    private void getFilters(FilterExpression filterExpression) {
        if (filterExpression == null) {
            whereFilter = null;
            havingFilter = null;
            return;
        }
        SplitFilterExpressionVisitor visitor = new SplitFilterExpressionVisitor(schema);
        FilterConstraints constraints = filterExpression.accept(visitor);
        whereFilter = constraints.getWhereExpression();
        havingFilter = constraints.getHavingExpression();
    }

    /**
     * Gets dimensions and timeDimensions based on relationships and attributes from {@link EntityProjection}.
     * @param dimensions Empty set of {@link Dimension} objects.
     * @param timeDimensions Empty set of {@link TimeDimension} objects.
     */
    public void getDimensionLists(Set<Dimension> dimensions, Set<TimeDimension> timeDimensions) {
        Set<String> dimensionNames = getRelationships();
        Set<String> metricNames = getAttributes(); // time dimensions are under attribute in entity projection
        for (Dimension dimension : schema.getDimensions()) {
            if (dimension instanceof TimeDimension) {
                if(metricNames.contains(dimension.getName()))
                timeDimensions.add((TimeDimension)dimension);
            }
            else if (dimensionNames.contains(dimension.getName()) || metricNames.contains(dimension.getName())) {
                dimensions.add(dimension);
            }
        }
    }

    /**
     * Gets metrics based on attributes from {@link EntityProjection}.
     * @param metrics Empty set of {@link Metric} objects.
     */
    public void getMetricList(Set<Metric> metrics) {
        Set<String> metricNames = getAttributes();
        for (Metric metric : schema.getMetrics()) {
            if (metricNames.contains(metric.getName())) {
                metrics.add(metric);
            }
        }
    }

    /**
     * Constructs map between {@link Metric} objects and the type of {@link Aggregation} we want to use.
     * @param metrics Set of {@link Metric} objects.
     */
    public void getMetricMap(Set<Metric> metrics) {
        metricMap = new LinkedHashMap<>();
        for (Metric metric : metrics) {
            metricMap.put(metric, metric.getAggregations().get(AGGREGATION_METHOD_INDEX));
        }
//        return metrics.stream()
//                .collect(
//                        Collectors.toMap(p -> p.getAggregations().get(AGGREGATION_METHOD_INDEX), Function.identity())
//                );
    }

    /**
     * Gets attribute names from {@link EntityProjection}.
     * @return attributes list of {@link Attribute} names
     */
    private Set<String> getAttributes() {
        Set<String> attributes = new LinkedHashSet<>();
        for (Attribute attribute : entityProjection.getAttributes()) {
            attributes.add(attribute.getName());
        }
        return attributes;
    }

    /**
     * Gets relationship names from {@link EntityProjection}.
     * @return relationships list of {@link Relationship} names
     */
    private Set<String> getRelationships() {
        Set<String> relationships = new LinkedHashSet<>();
        for (Relationship relationship : entityProjection.getRelationships()) {
            relationships.add(relationship.getName());
        }
        return relationships;
    }
}
