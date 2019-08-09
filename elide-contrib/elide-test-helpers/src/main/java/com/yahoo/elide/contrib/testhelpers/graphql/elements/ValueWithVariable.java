/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.testhelpers.graphql.elements;

import java.io.Serializable;

/**
 * {@link ValueWithVariable} models the {@code valueWithVariable} defined in GraphQL grammar.
 * <p>
 * GraphQL grammar(6.0) for {@code valueWithVariable} consists of sub-definitions:
 * <pre>
 * {@code
 *     valueWithVariable :
 *         variable |
 *         IntValue |
 *         FloatValue |
 *         StringValue |
 *         BooleanValue |
 *         NullValue |
 *         enumValue |
 *         arrayValueWithVariable |
 *         objectValueWithVariable;
 * }
 * </pre>
 * Each sub-definition is an implementation of {@link ValueWithVariable}.
 * <p>
 * This is a {@link java.util.function functional interface} whose functional method is {@link #toGraphQLSpec()}.
 */
@FunctionalInterface
public interface ValueWithVariable extends Serializable {

    /**
     * Returns the query string that corresponds to the valueWithVariable part of a GraphQL query.
     *
     * @return a sub-string of a GraphQL query
     */
    String toGraphQLSpec();
}
