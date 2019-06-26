/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.filter.expression;

import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.expression.FilterExpressionVisitor;

/**
 * A sub-type of {@link FilterExpression} that is evaluated in persistent storage.
 * <p>
 * This is a {@link java.util.function functional interface} whose functional method is
 * {@link #accept(FilterExpressionVisitor)}
 */
@FunctionalInterface
public interface WhereFilterExpression extends FilterExpression {

    // intentionally left blank
}
