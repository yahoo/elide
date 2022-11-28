/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation;

import static com.yahoo.elide.core.request.Argument.getArgumentMapFromArgumentSet;

import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.exceptions.InvalidOperationException;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.expression.PredicateExtractionVisitor;
import com.yahoo.elide.core.request.EntityProjection;
import com.yahoo.elide.datastores.aggregation.filter.visitor.FilterConstraints;
import com.yahoo.elide.datastores.aggregation.filter.visitor.SplitFilterExpressionVisitor;
import com.yahoo.elide.datastores.aggregation.metadata.models.Dimension;
import com.yahoo.elide.datastores.aggregation.metadata.models.Table;
import com.yahoo.elide.datastores.aggregation.metadata.models.TimeDimension;
import com.yahoo.elide.datastores.aggregation.query.DimensionProjection;
import com.yahoo.elide.datastores.aggregation.query.ImmutablePagination;
import com.yahoo.elide.datastores.aggregation.query.MetricProjection;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.query.TimeDimensionProjection;
import com.google.common.collect.Sets;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Helper for Aggregation Data Store which does the work associated with extracting {@link Query}.
 */
public class EntityProjectionTranslator {
    private Table queriedTable;
    private QueryEngine engine;

    private EntityProjection entityProjection;
    private Set<DimensionProjection> dimensionProjections;
    private Set<TimeDimensionProjection> timeDimensions;
    private List<MetricProjection> metrics;
    private FilterExpression whereFilter;
    private FilterExpression havingFilter;
    private Boolean bypassCache;
    private RequestScope scope;

    public EntityProjectionTranslator(QueryEngine engine, Table table,
                                      EntityProjection entityProjection, RequestScope scope,
                                      Boolean bypassCache) {
        this.engine = engine;
        this.queriedTable = table;
        this.entityProjection = entityProjection;
        this.bypassCache  = bypassCache;
        this.scope = scope;
        dimensionProjections = resolveNonTimeDimensions();
        timeDimensions = resolveTimeDimensions();
        metrics = resolveMetrics();
        splitFilters();
        addHavingMetrics();
    }

    /**
     * Builds the query from internal state.
     * @return {@link Query} query object with all the parameters provided by user.
     */
    public Query getQuery() {
        Query query = Query.builder()
                .source(queriedTable.toQueryable())
                .metricProjections(metrics)
                .dimensionProjections(dimensionProjections)
                .timeDimensionProjections(timeDimensions)
                .whereFilter(whereFilter)
                .havingFilter(havingFilter)
                .sorting(entityProjection.getSorting())
                .pagination(ImmutablePagination.from(entityProjection.getPagination()))
                .arguments(getArgumentMapFromArgumentSet(entityProjection.getArguments()))
                .bypassingCache(bypassCache)
                .scope(scope)
                .build();

        QueryValidator validator = engine.getValidator();
        validator.validate(query);
        return query;
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
        SplitFilterExpressionVisitor visitor = new SplitFilterExpressionVisitor(queriedTable);
        FilterConstraints constraints = filterExpression.accept(visitor);
        whereFilter = constraints.getWhereExpression();
        havingFilter = constraints.getHavingExpression();
    }

    /**
     * Adds to the list of queried metrics any metric in the HAVING filter that has not been explicitly requested
     * by the client.
     */
    private void addHavingMetrics() {
        if (havingFilter == null) {
            return;
        }

        //Flatten the HAVING filter expression into a list of predicates...
        havingFilter.accept(new PredicateExtractionVisitor()).forEach(filterPredicate -> {
            String fieldName = filterPredicate.getField();

            //If the predicate field is a metric
            if (queriedTable.getMetric(fieldName) != null) {

                //If the query doesn't contain this metric.
                if (metrics.stream().noneMatch((metric -> metric.getName().equals(fieldName)))) {

                    //Construct a new projection and add it to the query.
                    MetricProjection havingMetric = engine.constructMetricProjection(
                            queriedTable.getMetric(fieldName), fieldName, new HashMap<>());

                    metrics.add(havingMetric);
                }
            }
        });
    }

    /**
     * Gets time dimensions based on relationships and attributes from {@link EntityProjection}.
     *
     * @return projections for time dimension columns
     * @throws InvalidOperationException Thrown if a requested time grain is not supported.
     */
    private Set<TimeDimensionProjection> resolveTimeDimensions() {
        return entityProjection.getAttributes().stream()
                .filter(attribute -> queriedTable.getTimeDimension(attribute.getName()) != null)
                .map(timeDimAttr -> {
                    TimeDimension timeDim = queriedTable.getTimeDimension(timeDimAttr.getName());

                    return engine.constructTimeDimensionProjection(
                            timeDim,
                            timeDimAttr.getAlias(),
                            getArgumentMapFromArgumentSet(timeDimAttr.getArguments()));
                })
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Gets dimensions except time dimensions based on relationships and attributes from {@link EntityProjection}.
     */
    private Set<DimensionProjection> resolveNonTimeDimensions() {
        Set<DimensionProjection> attributes = entityProjection.getAttributes().stream()
                .filter(attribute -> queriedTable.getTimeDimension(attribute.getName()) == null)
                .map(dimAttr -> {
                    Dimension dimension = queriedTable.getDimension(dimAttr.getName());
                    return dimension == null
                            ? null
                            : engine.constructDimensionProjection(
                                    dimension,
                                    dimAttr.getAlias(),
                                    getArgumentMapFromArgumentSet(dimAttr.getArguments()));
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<DimensionProjection> relationships = entityProjection.getRelationships().stream()
                .map(dimAttr -> {
                    Dimension dimension = queriedTable.getDimension(dimAttr.getName());
                    return dimension == null
                            ? null
                            : engine.constructDimensionProjection(
                                dimension,
                                dimAttr.getAlias(),
                                Collections.emptyMap());
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        return Sets.union(attributes, relationships);
    }

    /**
     * Gets metrics based on attributes from {@link EntityProjection}.
     */
    private List<MetricProjection> resolveMetrics() {
        return entityProjection.getAttributes().stream()
                .filter(attribute -> queriedTable.isMetric(attribute.getName()))
                .map(attribute -> engine.constructMetricProjection(
                        queriedTable.getMetric(attribute.getName()),
                        attribute.getAlias(),
                        getArgumentMapFromArgumentSet(attribute.getArguments())))
                .collect(Collectors.toList());
    }
}
