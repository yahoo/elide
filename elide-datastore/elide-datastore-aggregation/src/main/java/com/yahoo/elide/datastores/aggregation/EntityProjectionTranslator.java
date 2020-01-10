/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.exceptions.InvalidOperationException;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.datastores.aggregation.filter.visitor.FilterConstraints;
import com.yahoo.elide.datastores.aggregation.filter.visitor.SplitFilterExpressionVisitor;
import com.yahoo.elide.datastores.aggregation.metadata.metric.MetricFunctionInvocation;
import com.yahoo.elide.datastores.aggregation.metadata.models.AnalyticView;
import com.yahoo.elide.datastores.aggregation.metadata.models.Dimension;
import com.yahoo.elide.datastores.aggregation.metadata.models.Table;
import com.yahoo.elide.datastores.aggregation.metadata.models.TimeDimension;
import com.yahoo.elide.datastores.aggregation.metadata.models.TimeDimensionGrain;
import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.query.TimeDimensionProjection;
import com.yahoo.elide.request.Argument;
import com.yahoo.elide.request.Attribute;
import com.yahoo.elide.request.EntityProjection;
import com.yahoo.elide.request.Relationship;

import com.google.common.collect.Sets;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Helper for Aggregation Data Store which does the work associated with extracting {@link Query}.
 */
public class EntityProjectionTranslator {
    private AnalyticView queriedTable;

    private EntityProjection entityProjection;
    private Set<ColumnProjection> dimensionProjections;
    private Set<TimeDimensionProjection> timeDimensions;
    private List<MetricFunctionInvocation> metrics;
    private FilterExpression whereFilter;
    private FilterExpression havingFilter;
    private EntityDictionary dictionary;

    public EntityProjectionTranslator(Table table, EntityProjection entityProjection, EntityDictionary dictionary) {
        if (!(table instanceof AnalyticView)) {
            throw new InvalidOperationException("Queried table is not analyticView: " + table.getName());
        }

        this.queriedTable = (AnalyticView) table;
        this.entityProjection = entityProjection;
        this.dictionary = dictionary;
        dimensionProjections = resolveNonTimeDimensions();
        timeDimensions = resolveTimeDimensions();
        metrics = resolveMetrics();
        splitFilters();
    }

    /**
     * Builds the query from internal state.
     * @return {@link Query} query object with all the parameters provided by user.
     */
    public Query getQuery() {
        Query query = Query.builder()
                .analyticView(queriedTable)
                .metrics(metrics)
                .groupByDimensions(dimensionProjections)
                .timeDimensions(timeDimensions)
                .whereFilter(whereFilter)
                .havingFilter(havingFilter)
                .sorting(entityProjection.getSorting())
                .pagination(entityProjection.getPagination())
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

                    Argument grainArgument = timeDimAttr.getArguments().stream()
                            .filter(attr -> attr.getName().equals("grain"))
                            .findAny()
                            .orElse(null);

                    TimeDimensionGrain resolvedGrain;
                    if (grainArgument == null) {
                        //The first grain is the default.
                        resolvedGrain = timeDim.getSupportedGrains().stream()
                                .findFirst()
                                .orElseThrow(() -> new IllegalStateException(
                                        String.format("Requested default grain, no grain defined on %s",
                                                timeDimAttr.getName())));
                    } else {
                        String requestedGrainName = grainArgument.getValue().toString();

                        resolvedGrain = timeDim.getSupportedGrains().stream()
                                .filter(supportedGrain -> supportedGrain.getGrain().name().toLowerCase(Locale.ENGLISH)
                                        .equals(requestedGrainName))
                                .findFirst()
                                .orElseThrow(() -> new InvalidOperationException(
                                        String.format("Unsupported grain %s for field %s",
                                                requestedGrainName,
                                                timeDimAttr.getName())));
                    }

                    return ColumnProjection.toProjection(timeDim, resolvedGrain.getGrain(), timeDimAttr.getAlias());
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
                    return dimension == null ? null : ColumnProjection.toProjection(dimension, dimAttr.getAlias());
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<ColumnProjection> relationships = entityProjection.getRelationships().stream()
                .map(dimAttr -> {
                    Dimension dimension = queriedTable.getDimension(dimAttr.getName());
                    return dimension == null ? null : ColumnProjection.toProjection(dimension, dimAttr.getAlias());
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        return Sets.union(attributes, relationships);
    }

    /**
     * Gets metrics based on attributes from {@link EntityProjection}.
     */
    private List<MetricFunctionInvocation> resolveMetrics() {
        return entityProjection.getAttributes().stream()
                .filter(attribute -> queriedTable.isMetric(attribute.getName()))
                .map(attribute -> queriedTable.getMetric(attribute.getName())
                        .getMetricFunction()
                        .invoke(attribute.getArguments(), attribute.getAlias()))
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
}
