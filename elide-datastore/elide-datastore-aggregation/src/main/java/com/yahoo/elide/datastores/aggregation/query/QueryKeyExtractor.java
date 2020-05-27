/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.query;

import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.filter.FilterPredicate;
import com.yahoo.elide.core.filter.expression.AndFilterExpression;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.expression.FilterExpressionVisitor;
import com.yahoo.elide.core.filter.expression.NotFilterExpression;
import com.yahoo.elide.core.filter.expression.OrFilterExpression;
import com.yahoo.elide.datastores.aggregation.metadata.models.Column;
import com.yahoo.elide.datastores.aggregation.metadata.models.Table;
import com.yahoo.elide.request.Argument;
import com.yahoo.elide.request.Pagination;
import com.yahoo.elide.request.Sorting;

import java.util.Comparator;
import java.util.Map;

/**
 * Generate a cache key for a given Query.
 */
public final class QueryKeyExtractor implements FilterExpressionVisitor<Object> {
    private static final char DELIMITER = ';'; // delimit fields

    // Since fields may contain a variable number of values, potentially nested,
    // we need a way to denote structure to prevent ambiguity.
    private static final char BEGIN_GROUP = '{';
    private static final char END_GROUP = '}';

    private static final int ESTIMATED_KEY_SIZE = 128;

    private final StringBuilder keyBuilder;

    private QueryKeyExtractor() {
        keyBuilder = new StringBuilder(ESTIMATED_KEY_SIZE);
    }

    public static String extractKey(Query query) {
        QueryKeyExtractor extractor = new QueryKeyExtractor();
        extractor.visit(query);
        return extractor.keyBuilder.toString();
    }

    private void visit(Query query) {
        visit(query.getTable());

        beginGroup();
        // `metrics` is a list - don't sort
        query.getMetrics().forEach(this::visit);
        endGroup();
        beginGroup();
        // `groupByDimensions` is an unordered set - sort
        query.getGroupByDimensions().stream().sorted(Comparator.comparing(ColumnProjection::getAlias))
                .forEachOrdered(this::visit);
        endGroup();
        beginGroup();
        // `timeDimensions` is an unordered set - sort
        query.getTimeDimensions().stream().sorted(Comparator.comparing(ColumnProjection::getAlias))
                .forEachOrdered(this::visit);
        endGroup();

        visitExpression(query.getWhereFilter());
        visitExpression(query.getHavingFilter());
        visit(query.getSorting());
        visit(query.getPagination());
        // eliding `scope` and `bypassingCache` fields
    }

    // Query Components
    //
    private void visit(Table table) {
        visit(table.getId());
        // `name` and `version` are included in id field
    }

    private void visit(MetricProjection metricProjection) {
        visit(metricProjection.getColumn());
    }

    private void visit(ColumnProjection<?> columnProjection) {
        visit(columnProjection.getColumn());
        visit(columnProjection.getAlias());
        visit(columnProjection.getArguments());
    }

    private void visit(Column column) {
        visit(column.getId());
    }

    private void visit(Map<String, Argument> arguments) {
        beginGroup();
        // `arguments` is an unordered map - sort by key
        arguments.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEachOrdered(e -> {
            visit(e.getKey());
            visit(e.getValue().getName());
            visit(e.getValue().getValue().toString());
        });
        endGroup();
    }

    private void visit(Sorting sorting) {
        if (sorting == null) {
            keyBuilder.append(DELIMITER);
            return;
        }
        beginGroup();
        visit(sorting.getType());
        // `sortingPaths` is an ordered map - don't sort
        sorting.getSortingPaths().forEach((path, order) -> {
            visit(path);
            visit(order.toString());
        });
        endGroup();
    }

    private void visit(Path path) {
        beginGroup();
        // `pathElements` is a list - don't sort
        path.getPathElements().forEach(this::visit);
        endGroup();
    }

    private void visit(Path.PathElement element) {
        beginGroup();
        visit(element.getType());
        visit(element.getFieldType());
        visit(element.getFieldName());
        endGroup();
    }

    private void visit(Pagination pagination) {
        if (pagination == null) {
            keyBuilder.append(DELIMITER);
            return;
        }
        beginGroup();
        visit(pagination.getOffset());
        visit(pagination.getLimit());
        visit(pagination.returnPageTotals() ? "1" : "0");
        endGroup();
    }

    // Filter Expressions
    //
    private void visitExpression(FilterExpression expr) {
        if (expr != null) {
            expr.accept(this);
        } else {
            keyBuilder.append(DELIMITER);
        }
    }

    @Override
    public Object visitPredicate(FilterPredicate filterPredicate) {
        beginGroup();
        visit("P");
        visit(filterPredicate.getPath());
        visit(filterPredicate.getOperator().toString());
        // `values` is list - don't sort
        filterPredicate.getValues().forEach(this::visitObject);
        endGroup();
        // `field` and `fieldPath` are derived from path
        return null;
    }

    @Override
    public Object visitAndExpression(AndFilterExpression expression) {
        beginGroup();
        visit("A");
        expression.getLeft().accept(this);
        expression.getRight().accept(this);
        endGroup();
        return null;
    }

    @Override
    public Object visitOrExpression(OrFilterExpression expression) {
        beginGroup();
        visit("O");
        expression.getLeft().accept(this);
        expression.getRight().accept(this);
        endGroup();
        return null;
    }

    @Override
    public Object visitNotExpression(NotFilterExpression expression) {
        beginGroup();
        visit("N");
        expression.getNegated().accept(this);
        endGroup();
        return null;
    }

    // Basic types
    //
    private void visit(Class<?> type) {
        keyBuilder.append(type.getCanonicalName()).append(DELIMITER);
    }

    private void visit(String string) {
        keyBuilder.append(string).append(DELIMITER);
    }

    private void visit(int value) {
        keyBuilder.append(value).append(DELIMITER);
    }

    private void visitObject(Object object) {
        String string = object.toString();
        keyBuilder.append(string.length()).append(DELIMITER);
        keyBuilder.append(string).append(DELIMITER);
    }

    private void beginGroup() {
        keyBuilder.append(BEGIN_GROUP);
    }

    private void endGroup() {
        keyBuilder.append(END_GROUP);
    }
}
