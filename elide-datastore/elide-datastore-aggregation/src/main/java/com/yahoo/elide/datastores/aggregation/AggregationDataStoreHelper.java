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
import java.util.stream.Collectors;

/**
 * Helper for Aggregation Data Store which does the work associated with extracting {@link Query}.
 */
public class AggregationDataStoreHelper {

    //TODO Add support for user selected metrics.
    private static final int AGGREGATION_METHOD_INDEX = 0;

    private Schema schema;
    private EntityProjection entityProjection;
    private Set<Dimension> dimensions;
    private Set<TimeDimension> timeDimensions;
    private Map<Metric, Class<? extends Aggregation>> metricMap;
    private FilterExpression whereFilter;
    private FilterExpression havingFilter;

    public AggregationDataStoreHelper(Schema schema, EntityProjection entityProjection) {
        this.schema = schema;
        this.entityProjection = entityProjection;
        dimensions = new LinkedHashSet<>();
        timeDimensions = new LinkedHashSet<>();
        resolveDimensionLists(dimensions, timeDimensions);
        Set<Metric> metrics = new LinkedHashSet<>();
        resolveMetricList(metrics);
        metricMap = new LinkedHashMap<>();
        resolveMetricMap(metrics);
        splitFilters();
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
     */
    private void splitFilters() {
        FilterExpression filterExpression = entityProjection.getFilterExpression();
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
    private void resolveDimensionLists(Set<Dimension> dimensions, Set<TimeDimension> timeDimensions) {
        Set<String> relationshipNames = getRelationships();
        Set<String> attributeNames = getAttributes(); // time dimensions are under attribute in entity projection
        for (Dimension dimension : schema.getDimensions()) {
            if (dimension instanceof TimeDimension && attributeNames.contains(dimension.getName())) {
                timeDimensions.add((TimeDimension) dimension);
            }
            else if (relationshipNames.contains(dimension.getName()) || attributeNames.contains(dimension.getName())) {
                dimensions.add(dimension);
            }
        }
    }

    /**
     * Gets metrics based on attributes from {@link EntityProjection}.
     * @param metrics Empty set of {@link Metric} objects.
     */
    private void resolveMetricList(Set<Metric> metrics) {
        Set<String> attributeNames = getAttributes();
        for (Metric metric : schema.getMetrics()) {
            if (attributeNames.contains(metric.getName())) {
                metrics.add(metric);
            }
        }
    }

    /**
     * Constructs map between {@link Metric} objects and the type of {@link Aggregation} we want to use.
     * @param metrics Set of {@link Metric} objects.
     */
    private void resolveMetricMap(Set<Metric> metrics) {
        for (Metric metric : metrics) {
            metricMap.put(metric, metric.getAggregations().get(AGGREGATION_METHOD_INDEX));
        }
    }

    /**
     * Gets attribute names from {@link EntityProjection}.
     * @return attributes list of {@link Attribute} names
     */
    private Set<String> getAttributes() {
        return entityProjection.getAttributes().stream().
                map(Attribute::getName).collect(Collectors.toSet());
    }

    /**
     * Gets relationship names from {@link EntityProjection}.
     * @return relationships list of {@link Relationship} names
     */
    private Set<String> getRelationships() {
        return entityProjection.getRelationships().stream().
                map(Relationship::getName).collect(Collectors.toSet());
    }
}
