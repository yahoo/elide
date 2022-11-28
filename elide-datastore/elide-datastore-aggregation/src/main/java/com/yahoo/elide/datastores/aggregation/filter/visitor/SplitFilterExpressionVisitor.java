/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.filter.visitor;

import com.yahoo.elide.core.filter.expression.AndFilterExpression;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.expression.FilterExpressionVisitor;
import com.yahoo.elide.core.filter.expression.NotFilterExpression;
import com.yahoo.elide.core.filter.expression.OrFilterExpression;
import com.yahoo.elide.core.filter.predicates.FilterPredicate;
import com.yahoo.elide.core.filter.visitors.FilterExpressionNormalizationVisitor;
import com.yahoo.elide.datastores.aggregation.annotation.MetricFormula;
import com.yahoo.elide.datastores.aggregation.metadata.models.Table;
import org.apache.commons.lang3.tuple.Pair;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

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
public class SplitFilterExpressionVisitor implements FilterExpressionVisitor<FilterConstraints> {

    @Getter(value = AccessLevel.PRIVATE)
    private final Table table;

    @Getter(value = AccessLevel.PRIVATE)
    private final FilterExpressionNormalizationVisitor normalizationVisitor;

    /**
     * Constructor.
     *
     * @param table  Object that offers meta information about an entity field
     *
     * @throws NullPointerException if any one of the argument is {@code null}
     */
    public SplitFilterExpressionVisitor(final Table table) {
        this.table = Objects.requireNonNull(table, "table");
        this.normalizationVisitor = new FilterExpressionNormalizationVisitor();
    }

    @Override
    public FilterConstraints visitPredicate(final FilterPredicate filterPredicate) {
        return isHavingPredicate(filterPredicate)
                ? FilterConstraints.pureHaving(filterPredicate) // this filterPredicate belongs to a HAVING clause
                : FilterConstraints.pureWhere(filterPredicate); // this filterPredicate belongs to a WHERE clause
    }

    @Override
    public FilterConstraints visitAndExpression(final AndFilterExpression expression) {
        /*
         * Definition:
         *     C = condition
         *     pure-W = WHERE C
         *     pure-H = HAVING C
         *     mix-HW = WHERE C HAVING C'
         *
         * Given that L and R operands of an AndFilterExpression can only be one of "pure-H", "pure-W", or "mix-HW",
         * then:
         *
         *     pure-W1 AND pure-W2 = WHERE C1 AND WHERE C2 = WHERE (C1 AND C2)    = pure-W
         *     pure-H1 AND pure-H2 = HAVING C1 AND HAVING C2 = HAVING (C1 AND C2) = pure-H
         *
         *     pure-H1 AND pureW2 = HAVING C1 AND WHERE C2 = WHERE C2 HAVING C1   = mix-HW
         *     pure-W1 AND pureH2                          = WHERE C1 HAVING C2   = mix-HW
         *
         *     mix-HW1 AND pure-W2 = WHERE C1 HAVING C1' AND WHERE C2 = WHERE (C1 & C2) HAVING C1'  = mix-HW
         *     mix-HW1 AND pure-H2 = WHERE C1 HAVING C1' AND HAVING C2 = WHERE C1 HAVING (C1' & C2) = mix-HW
         *
         *     mix-HW1 AND mim-HW2 = WHERE C1 HAVING C1' AND WHERE C2 HAVING C2' = WHERE (C1 & C2) HAVING (C1' & C2')
         *                         = mix-HW
         */

        FilterConstraints left = expression.getLeft().accept(this);
        FilterConstraints right = expression.getRight().accept(this);

        if (left.isPureWhere() && right.isPureWhere()) {
            // pure-W1 AND pure-W2 = WHERE (C1 & C2) = pure-W
            return FilterConstraints.pureWhere(
                    new AndFilterExpression(
                            left.getWhereExpression(),
                            right.getWhereExpression()
                    )
            );
        }
        if (left.isPureHaving() && right.isPureHaving()) {
            // pure-H1 AND pure-H2 = HAVING (C1 AND C2) = pure-H
            return FilterConstraints.pureHaving(
                    new AndFilterExpression(
                            left.getHavingExpression(),
                            right.getHavingExpression()
                    )
            );
        }
        // all of the rests are mix-HW
        return FilterConstraints.withWhereAndHaving(
                AndFilterExpression.fromPair(
                        left.getWhereExpression(),
                        right.getWhereExpression()
                ),
                AndFilterExpression.fromPair(
                        left.getHavingExpression(),
                        right.getHavingExpression()
                )
        );
    }

    @Override
    public FilterConstraints visitOrExpression(final OrFilterExpression expression) {
        /*
         * Definition:
         *     C = condition
         *     pure-W = WHERE C
         *     pure-H = HAVING C
         *     mix-HW = WHERE C HAVING C'
         *
         * Given that L and R operands of an OrFilterExpression can only be one of "pure-H", "pure-W", or "mix-HW",
         * then:
         *
         *     pure-W1 OR pure-W2 = WHERE C1 OR WHERE C2 = WHERE (C1 OR C2)    = pure-W
         *     pure-H1 OR pure-H2 = HAVING C1 OR HAVING C2 = HAVING (C1 OR C2) = pure-H
         *
         *     pure-H1 OR pureW2 = HAVING C1 OR WHERE C2 = HAVING (C1 OR C2)   = pure-H
         *     pure-W1 OR pureH2 = WHERE C1 OR HAVING C2 = HAVING (C1 OR C2)   = pure-H
         *
         *     mix-HW1 OR pure-W2 = (WHERE C1 HAVING C1') OR WHERE C2 = HAVING (C1 & C1' | C2)  = pure-H
         *     mix-HW1 OR pure-H2 = (WHERE C1 HAVING C1') OR HAVING C2 = HAVING (C1 & C1' | C2) = pure-H
         *
         *     mix-HW1 OR mim-HW2 = (WHERE C1 HAVING C1') OR (WHERE C2 HAVING C2') = HAVING ((C1 & C1') | (C2 & C2'))
         *                        = pure-H
         */

        FilterConstraints left = expression.getLeft().accept(this);
        FilterConstraints right = expression.getRight().accept(this);

        if (left.isPureWhere() && right.isPureWhere()) {
            // pure-W1 OR pure-W2 = WHERE (C1 OR C2) = pure-W
            return FilterConstraints.pureWhere(
                    OrFilterExpression.fromPair(
                            left.getWhereExpression(),
                            right.getWhereExpression()
                    )
            );
        }
        // all of the rests are pure-H
        return FilterConstraints.pureHaving(
                OrFilterExpression.fromPair(
                        AndFilterExpression.fromPair(
                                left.getWhereExpression(),
                                left.getHavingExpression()
                        ),
                        AndFilterExpression.fromPair(
                                right.getWhereExpression(),
                                right.getHavingExpression()
                        )
                )
        );
    }

    @Override
    public FilterConstraints visitNotExpression(NotFilterExpression expression) {
        FilterExpression normalized = getNormalizationVisitor().visitNotExpression(expression);

        if (normalized instanceof AndFilterExpression) {
            return visitAndExpression((AndFilterExpression) normalized);
        }
        if (normalized instanceof OrFilterExpression) {
            return visitOrExpression((OrFilterExpression) normalized);
        }
        if (normalized instanceof NotFilterExpression) {
            FilterConstraints negatedConstraint = visitNotExpression((NotFilterExpression) normalized);

            if (negatedConstraint.isPureWhere()) {
                return FilterConstraints.pureWhere(new NotFilterExpression(negatedConstraint.getWhereExpression()));
            }
            // It is not possible to have a mixed where/having for a NotFilterExpression after normalization
            // so this must be a pure HAVING
            return FilterConstraints.pureHaving(new NotFilterExpression(negatedConstraint.getHavingExpression()));
        }
        return visitPredicate((FilterPredicate) normalized);
    }

    /**
     * Returns whether or not a {@link FilterPredicate} corresponds to a {@code HAVING} clause in JPQL query.
     * <p>
     * A {@link FilterPredicate} corresponds to a {@code HAVING} clause iff the predicate field has
     * {@link MetricFormula} annotation on it.
     *
     * @param filterPredicate  The terminal filter expression to check for
     *
     * @return {@code true} if the {@link FilterPredicate} is a HAVING clause
     */
    private boolean isHavingPredicate(final FilterPredicate filterPredicate) {
        String fieldName = filterPredicate.getField();

        return getTable().isMetric(fieldName);
    }
}
