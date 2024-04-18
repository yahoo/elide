/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.datastores.aggregation.cache;

import com.paiondata.elide.core.Path;
import com.paiondata.elide.core.filter.expression.AndFilterExpression;
import com.paiondata.elide.core.filter.expression.FilterExpression;
import com.paiondata.elide.core.filter.expression.FilterExpressionVisitor;
import com.paiondata.elide.core.filter.expression.NotFilterExpression;
import com.paiondata.elide.core.filter.expression.OrFilterExpression;
import com.paiondata.elide.core.filter.predicates.FilterPredicate;
import com.paiondata.elide.core.request.Argument;
import com.paiondata.elide.core.request.Pagination;
import com.paiondata.elide.core.request.Sorting;
import com.paiondata.elide.core.type.Type;
import com.paiondata.elide.datastores.aggregation.query.ColumnProjection;
import com.paiondata.elide.datastores.aggregation.query.Query;
import com.paiondata.elide.datastores.aggregation.query.Queryable;

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
        visit(query.getSource());

        beginGroup();
        // `metrics` is a list - don't sort
        query.getMetricProjections().forEach(this::visit);
        endGroup();
        beginGroup();
        // `groupByDimensions` is an unordered set - sort
        query.getDimensionProjections().stream().sorted(Comparator.comparing(ColumnProjection::getSafeAlias))
                .forEachOrdered(this::visit);
        endGroup();
        beginGroup();
        // `timeDimensions` is an unordered set - sort
        query.getTimeDimensionProjections().stream().sorted(Comparator.comparing(ColumnProjection::getSafeAlias))
                .forEachOrdered(this::visit);
        endGroup();

        visitExpression(query.getWhereFilter());
        visitExpression(query.getHavingFilter());
        visit(query.getSorting());
        visit(query.getPagination());
        // eliding `scope` and `bypassingCache` fields
    }

    // Query Components
    private void visit(Queryable source) {
        visit(source.getAlias());
    }

    private void visit(ColumnProjection columnProjection) {
        visit(columnProjection.getSafeAlias());
        visit(columnProjection.getArguments());
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
    private void visit(Type<?> type) {
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
