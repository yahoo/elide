/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.filter.visitor;

import com.yahoo.elide.core.filter.expression.FilterExpression;

import lombok.Getter;

import java.util.Objects;

/**
 * {@link FilterConstraints} is an auxiliary class for {@link SplitFilterExpressionVisitor} that wraps a {@code WHERE}
 * filter expression and {@code HAVING} filter expression.
 * <p>
 * {@link FilterConstraints} is thread-safe and can be accessed by multiple threads.
 */
public class FilterConstraints {

    /**
     * Creates a new {@link FilterConstraints} instance that wraps a specified {@code HAVING} filter expression only.
     *
     * @param havingExpression  A pure {@code HAVING} filter expression
     *
     * @return a new instance of {@link FilterConstraints}
     *
     * @throws NullPointerException if the provided {@code HAVING} filter expression is {@code null}
     */
    public static FilterConstraints pureHaving(FilterExpression havingExpression) {
        return new FilterConstraints(
                null,
                Objects.requireNonNull(havingExpression, "havingExpression")
        );
    }

    /**
     * Creates a new {@link FilterConstraints} instance that wraps a specified {@code WHERE} filter expression only.
     *
     * @param whereExpression  A pure {@code WHERE} filter expression
     *
     * @return a new instance of {@link FilterConstraints}
     *
     * @throws NullPointerException if the provided {@code WHERE} filter expression is {@code null}
     */
    public static FilterConstraints pureWhere(FilterExpression whereExpression) {
        return new FilterConstraints(
                Objects.requireNonNull(whereExpression, "whereExpression"),
                null
        );
    }

    /**
     * Creates a new {@link FilterConstraints} instance that wraps a pair of specified {@code WHERE} filter expression
     * and {@code HAVING} filter expression.
     *
     * @param whereExpression  A pure {@code HAVING} filter expression
     * @param havingExpression  A pure {@code WHERE} filter expression
     *
     * @return a new instance of {@link FilterConstraints}
     *
     * @throws NullPointerException if the provided {@code WHERE} or {@code HAVING} filter expression is {@code null}
     */
    public static FilterConstraints withWhereAndHaving(
            FilterExpression whereExpression,
            FilterExpression havingExpression
    ) {
        return new FilterConstraints(
                Objects.requireNonNull(whereExpression, "whereExpression"),
                Objects.requireNonNull(havingExpression, "havingExpression")
        );
    }

    @Getter
    private final FilterExpression whereExpression;

    @Getter
    private final FilterExpression havingExpression;

    /**
     * Private constructor.
     *
     * @param whereExpression
     * @param havingExpression
     */
    private FilterConstraints(FilterExpression whereExpression, FilterExpression havingExpression) {
        this.whereExpression = whereExpression;
        this.havingExpression = havingExpression;
    }

    /**
     * Returns whether or not this {@link FilterConstraints} filter expression pair contains only a {@code HAVING}
     * expression, i.e. no {@code WHERE} clause.
     *
     * @return {@code true} if there is {@code HAVING} expression only and not {@code WHERE} expression.
     */
    public boolean isPureHaving() {
        return getWhereExpression() == null && getHavingExpression() != null;
    }

    /**
     * Returns whether or not this {@link FilterConstraints} filter expression pair contains only a {@code WHERE}
     * expression, i.e. no {@code HAVING} clause.
     *
     * @return {@code true} if there is {@code HAVING} expression only and not {@code WHERE} expression.
     */
    public boolean isPureWhere() {
        return getWhereExpression() != null && getHavingExpression() == null;
    }
}
