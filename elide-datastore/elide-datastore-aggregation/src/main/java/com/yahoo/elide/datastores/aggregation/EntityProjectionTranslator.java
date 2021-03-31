/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation;

import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.exceptions.InvalidOperationException;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.expression.PredicateExtractionVisitor;
import com.yahoo.elide.core.request.Argument;
import com.yahoo.elide.core.request.Attribute;
import com.yahoo.elide.core.request.EntityProjection;
import com.yahoo.elide.core.request.Relationship;
import com.yahoo.elide.core.security.User;
import com.yahoo.elide.datastores.aggregation.filter.visitor.FilterConstraints;
import com.yahoo.elide.datastores.aggregation.filter.visitor.SplitFilterExpressionVisitor;
import com.yahoo.elide.datastores.aggregation.metadata.models.Dimension;
import com.yahoo.elide.datastores.aggregation.metadata.models.Table;
import com.yahoo.elide.datastores.aggregation.metadata.models.TimeDimension;
import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
import com.yahoo.elide.datastores.aggregation.query.ImmutablePagination;
import com.yahoo.elide.datastores.aggregation.query.MetricProjection;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.query.TimeDimensionProjection;
import com.google.common.collect.Sets;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Helper for Aggregation Data Store which does the work associated with extracting {@link Query}.
 */
public class EntityProjectionTranslator {
    private Table queriedTable;
    private QueryEngine engine;

    private EntityProjection entityProjection;
    private Set<ColumnProjection> dimensionProjections;
    private Set<TimeDimensionProjection> timeDimensions;
    private List<MetricProjection> metrics;
    private FilterExpression whereFilter;
    private FilterExpression havingFilter;
    private EntityDictionary dictionary;
    private User user;
    private Boolean bypassCache;


    public EntityProjectionTranslator(QueryEngine engine, Table table,
                                      EntityProjection entityProjection, EntityDictionary dictionary,
                                      User user, Boolean bypassCache) {
        this.engine = engine;
        this.queriedTable = table;
        this.entityProjection = entityProjection;
        this.dictionary = dictionary;
        this.user = user;
        this.bypassCache  = bypassCache;
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
                .context(prepareQueryContext())
                .bypassingCache(bypassCache)
                .build();

        QueryValidator validator = new QueryValidator(query, getAllFields(), dictionary);
        validator.validate();
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
                if (metrics.stream().noneMatch((metric -> metric.getAlias().equals(fieldName)))) {

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
                            timeDimAttr.getArguments().stream()
                                    .collect(Collectors.toMap(Argument::getName, Function.identity())));
                })
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Gets dimensions except time dimensions based on relationships and attributes from {@link EntityProjection}.
     */
    private Set<ColumnProjection> resolveNonTimeDimensions() {
        Set<ColumnProjection> attributes = entityProjection.getAttributes().stream()
                .filter(attribute -> queriedTable.getTimeDimension(attribute.getName()) == null)
                .map(dimAttr -> {
                    Dimension dimension = queriedTable.getDimension(dimAttr.getName());
                    return dimension == null
                            ? null
                            : engine.constructDimensionProjection(
                                    dimension,
                                    dimAttr.getAlias(),
                                    dimAttr.getArguments().stream()
                                            .collect(Collectors.toMap(Argument::getName, Function.identity())));
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<ColumnProjection> relationships = entityProjection.getRelationships().stream()
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
                        attribute.getArguments().stream()
                                .collect(Collectors.toMap(Argument::getName, Function.identity()))))
                .collect(Collectors.toList());
    }

    /**
     * Gets relationship names from {@link EntityProjection}.
     * @return relationships list of {@link Relationship} names
     */
    private Set<String> getRelationships() {
        return entityProjection.getRelationships().stream()
                .map(Relationship::getName).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Gets attribute names from {@link EntityProjection}.
     * @return relationships list of {@link Attribute} names
     */
    private Set<String> getAttributes() {
        return entityProjection.getAttributes().stream()
                .map(Attribute::getName).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Helper method to get all field names from the {@link EntityProjection}.
     * @return allFields set of all field names
     */
    private Set<String> getAllFields() {
        Set<String> allFields = getAttributes();
        allFields.addAll(getRelationships());
        return allFields;
    }

    private Map<String, Object> prepareQueryContext() {
        Map<String, Object> context = new HashMap<>();
        populateUserContext(context);
        populateRequestContext(context);
        return context;
    }

    private void populateUserContext(Map<String, Object> context) {
        Map<String, Object> userMap = new HashMap<>();
        context.put("$$user", userMap);
        userMap.put("identity", user.getName());
    }

    private void populateRequestContext(Map<String, Object> context) {

        Map<String, Object> requestMap = new HashMap<>();
        context.put("$$request", requestMap);
        Map<String, Object> tableMap = new HashMap<>();
        requestMap.put("table", tableMap);
        Map<String, Object> columnsMap = new HashMap<>();
        requestMap.put("columns", columnsMap);

        // Populate $$request.table context
        tableMap.put("name", queriedTable.getName());
        tableMap.put("args", entityProjection.getArguments().stream()
                        .collect(Collectors.toMap(Argument::getName, Argument::getValue)));

        // Populate $$request.columns context
        entityProjection.getAttributes().forEach(attr -> {
            String columnName = attr.getName();
            Map<String, Object> columnMap = new HashMap<>();
            columnsMap.put(columnName, columnMap);

            // Populate $$request.columns.column context
            columnMap.put("name", attr.getName());
            columnMap.put("args", attr.getArguments().stream()
                            .collect(Collectors.toMap(Argument::getName, Argument::getValue)));
        });
    }
}
