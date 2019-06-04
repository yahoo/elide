/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.filter.visitor;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.filter.FilterPredicate;
import com.yahoo.elide.core.filter.expression.AndFilterExpression;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.expression.FilterExpressionVisitor;
import com.yahoo.elide.core.filter.expression.NotFilterExpression;
import com.yahoo.elide.core.filter.expression.OrFilterExpression;
import com.yahoo.elide.datastores.aggregation.annotation.MetricAggregation;
import com.yahoo.elide.parsers.expression.FilterExpressionNormalizationVisitor;

import org.apache.commons.lang3.tuple.Pair;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link SplitFilterExpressionVisitor} splits the {@link FilterExpression} into a {@code WHERE} expression and a
 * {@code Having} expression.
 * <p>
 * {@link SplitFilterExpressionVisitor} is leveraged by the AggregationStore to construct the JPQL query.
 * {@link FilterExpression} for AggregationDataStore must be split into those that apply to metric aggregations
 * ({@code HAVING} clauses) and those that apply to dimensions ({@code WHERE} clauses), although only a single
 * {@link FilterExpression} is passed to the datastore in each query. The split groups {@code HAVING} clauses and
 * {@code WHERE} clauses separately. For example:
 * <pre>
 * +-------------------+-----------------------------+------------------------+
 * |    Expression     |             SQL             | WHERE-clause Promotion |
 * +-------------------+-----------------------------+------------------------+
 * | H1 AND W1 AND H2  | WHERE W1 HAVING (H1 AND H2) | No                     |
 * | (H1 OR H2) AND W1 | WHERE W1 HAVING (H1 OR H2)  | No                     |
 * | H1 OR W1          | HAVING (H1 OR W1)           | Yes                    |
 * | (W1 AND H1) OR W2 | HAVING ((W1 AND H1) OR W2)  | Yes                    |
 * +-------------------+-----------------------------+------------------------+
 * </pre>
 * Note that {@link SplitFilterExpressionVisitor} might incur more-than-expected network I/O in the case of WHERE-clause
 * promotion.
 * <p>
 * {@link SplitFilterExpressionVisitor} splits by storing {@code WHERE} and {@code HAVING} clauses in
 * {@link Pair#getLeft() left} and {@link Pair#getRight() right} of a {@link Pair}, respectively. For example:
 * <pre>
 * {@code
 * Pair<FilterExpression, FilterExpression> filterPair = filterExpression.accept(splitFilterExpressionVisitor);
 *
 * FilterExpression whereClauseFilter = filterPair.getLeft();
 * FilterExpression havingClauseFilter = filterPair.getRight();
 * }
 * </pre>
 * {@link SplitFilterExpressionVisitor} is thread-safe and can be accessed by multiple threads at the same time.
 */
@Slf4j
public class SplitFilterExpressionVisitor implements FilterExpressionVisitor<WhereHaving> {

    @Getter(value = AccessLevel.PRIVATE)
    private final EntityDictionary entityDictionary;
    @Getter(value = AccessLevel.PRIVATE)
    private final FilterExpressionNormalizationVisitor normalizationVisitor;

    /**
     * Constructor.
     *
     * @param entityDictionary  Object that offers annotation information about an entity field
     * @param normalizationVisitor  A helper {@link FilterExpressionVisitor} that is leveraged to transform
     * expressions like {@code NOT (A AND B)} into {@code NOT A OR NOT B}
     */
    public SplitFilterExpressionVisitor(
            final EntityDictionary entityDictionary,
            final FilterExpressionNormalizationVisitor normalizationVisitor
    ) {
        this.entityDictionary = entityDictionary;
        this.normalizationVisitor = normalizationVisitor;
    }

    @Override
    public WhereHaving visitPredicate(final FilterPredicate filterPredicate) {
        return isHavingPredicate(filterPredicate)
                ? WhereHaving.pureHaving(filterPredicate) // this filterPredicate belongs to a HAVING clause
                : WhereHaving.pureWhere(filterPredicate); // this filterPredicate belongs to a WHERE clause
    }

    @Override
    public WhereHaving visitAndExpression(final AndFilterExpression expression) {
        WhereHaving left = expression.getLeft().accept(this);
        WhereHaving right = expression.getRight().accept(this);

        if (left.isWhereExpression() && right.isWhereExpression()) {
            // pure-W AND pure-W
            return WhereHaving.pureWhere(
                    new AndFilterExpression(
                            left.getWhereExpression(),
                            right.getWhereExpression()
                    )
            );
        } else if (left.isHavingExpression() && right.isWhereExpression()) {
            // pure-H AND pure-W
            return WhereHaving.withWhereAndHaving(
                    right.getWhereExpression(),
                    left.getHavingExpression()
            );

        } else if (left.isWhereExpression() && right.isHavingExpression()) {
            // pure-W AND pure-H
            return WhereHaving.withWhereAndHaving(
                    left.getWhereExpression(),
                    right.getHavingExpression()
            );
        } else {
            return WhereHaving.withWhereAndHaving(
                    AndFilterExpression.and(
                            left.getWhereExpression(),
                            right.getWhereExpression()
                    ),
                    AndFilterExpression.and(
                            left.getHavingExpression(),
                            right.getHavingExpression()
                    )
            );
        }
    }

    @Override
    public WhereHaving visitOrExpression(final OrFilterExpression expression) {
        WhereHaving left = expression.getLeft().accept(this);
        WhereHaving right = expression.getRight().accept(this);

        if (left.isWhereExpression() && right.isWhereExpression()) {
            // pure-W OR pure-W
            return WhereHaving.pureWhere(
                    OrFilterExpression.or(
                            left.getWhereExpression(),
                            right.getWhereExpression()
                    )
            );
        } else {
            // H OR H
            // W OR H (W promote)
            // H OR W (W promote)
            return WhereHaving.pureHaving(
                    OrFilterExpression.or(
                            expression.getLeft(),
                            expression.getRight()
                    )
            );
        }
    }

    @Override
    public WhereHaving visitNotExpression(NotFilterExpression expression) {
        FilterExpression normalized = getNormalizationVisitor().visitNotExpression(expression);

        if (normalized instanceof AndFilterExpression) {
            return visitAndExpression((AndFilterExpression) normalized);
        } else if (normalized instanceof OrFilterExpression) {
            return visitOrExpression((OrFilterExpression) normalized);
        } else if (normalized instanceof NotFilterExpression) {
            normalized = ((NotFilterExpression) normalized).getNegated();
            return visitNotExpression((NotFilterExpression) normalized);
        } else {
            return visitPredicate((FilterPredicate) normalized);
        }
    }

    /**
     * Returns whether or not a {@link FilterPredicate} corresponds to a {@code HAVING} clause in JPQL query.
     * <p>
     * A {@link FilterPredicate} corresponds to a {@code HAVING} clause iff the predicate field has
     * {@link MetricAggregation} annotation on it.
     *
     * @param filterPredicate  The terminal filter expression to check for
     *
     * @return {@code true} if the {@link FilterPredicate} is a HAVING clause
     */
    private boolean isHavingPredicate(final FilterPredicate filterPredicate) {
        Class entityClass = filterPredicate.getEntityType();
        String fieldName = filterPredicate.getField();

        return getEntityDictionary()
                .getAttributeOrRelationAnnotation(entityClass, MetricAggregation.class, fieldName) != null;
    }
}
