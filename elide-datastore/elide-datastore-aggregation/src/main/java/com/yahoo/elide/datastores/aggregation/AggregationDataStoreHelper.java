/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation;

import com.yahoo.elide.core.exceptions.InvalidOperationException;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.datastores.aggregation.filter.visitor.FilterConstraints;
import com.yahoo.elide.datastores.aggregation.filter.visitor.SplitFilterExpressionVisitor;
import com.yahoo.elide.datastores.aggregation.query.ProjectedDimension;
import com.yahoo.elide.datastores.aggregation.query.ProjectedTimeDimension;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.schema.Schema;
import com.yahoo.elide.datastores.aggregation.schema.metric.Aggregation;
import com.yahoo.elide.datastores.aggregation.schema.metric.Metric;
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
    private Set<ProjectedDimension> projectedDimensions;
    private Set<ProjectedTimeDimension> timeDimensions;
    private Map<Metric, Class<? extends Aggregation>> metricMap;
    private FilterExpression whereFilter;
    private FilterExpression havingFilter;

    public AggregationDataStoreHelper(Schema schema, EntityProjection entityProjection) {
        this.schema = schema;
        this.entityProjection = entityProjection;
        projectedDimensions = new LinkedHashSet<>();
        timeDimensions = new LinkedHashSet<>();
        resolveDimensionLists(projectedDimensions, timeDimensions);
        Set<Metric> metrics = new LinkedHashSet<>();
        resolveMetricList(metrics);
        metricMap = new LinkedHashMap<>();
        resolveMetricMap(metrics);
        splitFilters();
    }

    /**
     * Builds the query from internal state.
     * @return {@link Query} query object with all the parameters provided by user.
     */
    public Query getQuery() {
        return Query.builder()
                .schema(schema)
                .metrics(metricMap)
                .groupDimensions(projectedDimensions)
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
     * @param projectedDimensions Empty set of {@link ProjectedDimension} objects.
     * @param timeDimensions Empty set of {@link ProjectedTimeDimension} objects.
     */
    private void resolveDimensionLists(Set<ProjectedDimension> projectedDimensions,
                                       Set<ProjectedTimeDimension> timeDimensions) {
        Set<String> relationshipNames = getRelationships();
        Set<String> attributeNames = getAttributes(); // time dimensions are under attribute in entity projection
        for (ProjectedDimension projectedDimension : schema.getDimensions()) {
            if (projectedDimension instanceof ProjectedTimeDimension
                    && attributeNames.contains(projectedDimension.getName())) {
                timeDimensions.add((ProjectedTimeDimension) projectedDimension);
            }
            else if (relationshipNames.contains(projectedDimension.getName())
                    || attributeNames.contains(projectedDimension.getName())) {
                projectedDimensions.add(projectedDimension);
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
        if (metrics.isEmpty()) {
            throw new InvalidOperationException("Must provide at least one metric in query");
        }
        for (Metric metric : metrics) {
            metricMap.put(metric, metric.getAggregations().get(AGGREGATION_METHOD_INDEX));
        }
    }

    /**
     * Gets attribute names from {@link EntityProjection}.
     * @return attributes list of {@link Attribute} names
     */
    private Set<String> getAttributes() {
        return entityProjection.getAttributes().stream()
                .map(Attribute::getName).collect(Collectors.toSet());
    }

    /**
     * Gets relationship names from {@link EntityProjection}.
     * @return relationships list of {@link Relationship} names
     */
    private Set<String> getRelationships() {
        return entityProjection.getRelationships().stream()
                .map(Relationship::getName).collect(Collectors.toSet());
    }
}
