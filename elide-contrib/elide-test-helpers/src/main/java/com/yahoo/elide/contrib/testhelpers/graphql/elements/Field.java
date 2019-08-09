/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.testhelpers.graphql.elements;

/**
 * {@link Field} represents the same concept as {@link graphql.language.Field GraphQL Field} but specializes in
 * serialization, in contrast to {@link graphql.language.Field GraphQL Field}, which is designed for deserialization.
 * <p>
 * According to GraphQL grammar (6.0):
 * <pre>
 * {@code
 *     selection :
 *         field |
 *         fragmentSpread |
 *         inlineFragment;
 *
 *     field : alias? name arguments? directives? selectionSet?;
 * }
 * </pre>
 * A {@link Field} is a sub-type of {@link Selection}. The simplest {@link Field} is a {@link ScalarField}; otherwise
 * it would be an {@link ObjectField}
 *
 * This is a {@link java.util.function functional interface} whose functional method is {@link #toGraphQLSpec()}.
 *
 * @see Selection
 * @see ScalarField
 * @see ObjectField
 * @see <a href="https://graphql.org/learn/schema/#scalar-types">Scalar types</a>
 * @see <a href="https://graphql.org/learn/schema/#object-types-and-fields">Object types and fields</a>
 */
@FunctionalInterface
public interface Field extends Selection {

    // intentionally left blank
}
