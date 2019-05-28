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

import java.lang.annotation.Annotation;
import java.util.Objects;

/**
 * {@link SplitFilterExpressionVisitor} splits the {@link FilterExpression} into a {@code WHERE} expression and a
 * {@code Having} expression.
 * <p>
 * {@link FilterExpression} for AggregationDataStore must be split into those that apply to metric aggregations
 * ({@code HAVING} clauses) and those that apply to dimensions ({@code WHERE} clauses), although only a single
 * {@link FilterExpression} is passed to the datastore in each query. The split should group {@code HAVING} clauses and
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
 */
@Slf4j
public class SplitFilterExpressionVisitor implements FilterExpressionVisitor<Pair<FilterExpression, FilterExpression>> {

    @Getter(value = AccessLevel.PRIVATE)
    private final EntityDictionary entityDictionary;
    @Getter(value = AccessLevel.PRIVATE)
    private final FilterExpressionNormalizationVisitor normalizationVisitor;

    public SplitFilterExpressionVisitor(
            final EntityDictionary entityDictionary,
            final FilterExpressionNormalizationVisitor normalizationVisitor
    ) {
        this.entityDictionary = entityDictionary;
        this.normalizationVisitor = normalizationVisitor;
    }

    @Override
    public Pair<FilterExpression, FilterExpression> visitPredicate(final FilterPredicate filterPredicate) {
        return isHavingPredicate(filterPredicate)
                ? Pair.of(null, filterPredicate)   // this filterPredicate belongs to a HAVING clause
                : Pair.of(filterPredicate, null); // this filterPredicate belongs to a WHERE clause
    }

    @Override
    public Pair<FilterExpression, FilterExpression> visitAndExpression(final AndFilterExpression expression) {
        Pair<FilterExpression, FilterExpression> left = expression.getLeft().accept(this);
        Pair<FilterExpression, FilterExpression> right = expression.getRight().accept(this);

        if (isWhereExpression(left) && isWhereExpression(right)) {
            // pure-W AND pure-W
            return Pair.of(
                    new AndFilterExpression(
                            extractWhereExpression(left),
                            extractWhereExpression(right)
                    ),
                    null
            );
        } else if (isHavingExpression(left) && isWhereExpression(right)) {
            // pure-H AND pure-W
            return Pair.of(
                    extractHavingExpression(left),
                    extractWhereExpression(right)
            );

        } else if (isWhereExpression(left) && isHavingExpression(right)) {
            // pure-W AND pure-H
            return Pair.of(
                    extractWhereExpression(left),
                    extractHavingExpression(right)
            );
        } else {
            return Pair.of(
                    AndFilterExpression.withLeftAndRight(
                            extractWhereExpression(left),
                            extractWhereExpression(right)
                    ),
                    AndFilterExpression.withLeftAndRight(
                            extractHavingExpression(left),
                            extractHavingExpression(right)
                    )
            );
        }
    }

    @Override
    public Pair<FilterExpression, FilterExpression> visitOrExpression(final OrFilterExpression expression) {
        Pair<FilterExpression, FilterExpression> left = expression.getLeft().accept(this);
        Pair<FilterExpression, FilterExpression> right = expression.getRight().accept(this);

        if (isWhereExpression(left) && isWhereExpression(right)) {
            // W OR W
            return Pair.of(
                    new OrFilterExpression(
                            extractWhereExpression(left),
                            extractWhereExpression(right)
                    ),
                    null
            );
        } else {
            // H OR H
            // W OR H (W promote)
            // H OR W (W promote)
            return Pair.of(
                    null,
                    new OrFilterExpression(
                            extractHavingExpression(left),
                            extractHavingExpression(right)
                    )
            );
        }    }

    @Override
    public Pair<FilterExpression, FilterExpression> visitNotExpression(NotFilterExpression expression) {
        FilterExpression normalized = getNormalizationVisitor().visitNotExpression(expression);

        if (normalized instanceof AndFilterExpression) {
            return visitAndExpression((AndFilterExpression) normalized);
        } else if (normalized instanceof OrFilterExpression) {
            return visitOrExpression((OrFilterExpression) normalized);
        } else {
            return visitPredicate((FilterPredicate) normalized);
        }
    }

    /**
     * Returns whether or not a pair of {@link FilterExpression}s is a pure {@code HAVING} expression, i.e. no
     * {@code WHERE} clause.
     *
     * @param filterExpression  The pair to check
     *
     * @return {@code true} if there is {@code HAVING} expression only and not {@code WHERE} expression.
     */
    private static boolean isHavingExpression(Pair<FilterExpression, FilterExpression> filterExpression) {
        return filterExpression.getLeft() == null && filterExpression.getRight() != null;
    }

    /**
     * Returns whether or not a pair of {@link FilterExpression}s is a pure {@code WHERE} expression, i.e. no
     * {@code HAVING} clause.
     *
     * @param filterExpression  The pair to check
     *
     * @return {@code true} if there is {@code WHERE} expression only and not {@code HAVING} expression.
     */
    private static boolean isWhereExpression(Pair<FilterExpression, FilterExpression> filterExpression) {
        return filterExpression.getLeft() != null && filterExpression.getRight() == null;
    }

    /**
     * Returns the {@code WHERE} expression from the pair of {@link FilterExpression}s.
     * <p>
     * See class documentation for more details on the "pair".
     *
     * @param filterExpressionPair  The pair to extract the {@code WHERE} expression from
     *
     * @return the {@code WHERE} constraint
     */
    private static FilterExpression extractWhereExpression(Pair<FilterExpression, FilterExpression> filterExpressionPair) {
        return filterExpressionPair.getLeft();
    }

    /**
     * Returns the {@code HAVING} expression from the pair of {@link FilterExpression}s.
     * <p>
     * See class documentation for more details on the "pair".
     *
     * @param filterExpressionPair  The pair to extract the {@code HAVING} expression from
     *
     * @return the {@code HAVING} constraint
     */
    private static FilterExpression extractHavingExpression(Pair<FilterExpression, FilterExpression> filterExpressionPair) {
        return filterExpressionPair.getRight();
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
        String entityName = filterPredicate.getEntityType().getName();
        String fieldName = filterPredicate.getField();

        try {
            return getEntityDictionary().getFieldAnnotations(entityName, fieldName).stream()
                    .map(Annotation::annotationType)
                    .map(annotation -> annotation.getAnnotation(MetricAggregation.class))
                    .anyMatch(Objects::nonNull);
        } catch (NoSuchFieldException exception) {
            String message = String.format("Field '%s' not found in entity '%s'", entityName, fieldName);
            log.error(message, exception);
            throw new IllegalStateException(message, exception);
        }
    }
}
