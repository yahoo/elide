/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.testhelpers.graphql.elements;

/**
 * {@link OperationDefinition} represents the same concept as
 * {@link graphql.language.OperationDefinition GraphQL Operation Definition} but specializes in serialization, in
 * contrast to {@link graphql.language.OperationDefinition GraphQL Definition}, which is designed for deserialization.
 * <p>
 * According to GraphQL grammar (6.0)
 * <pre>
 * {@code
 *     definition:
 *         operationDefinition |
 *         fragmentDefinition |
 *         typeSystemDefinition
 *         ;
 *
 *     operationDefinition:
 *         selectionSet |
 *         operationType  name? variableDefinitions? directives? selectionSet;
 * }
 * </pre>
 * An {@link OperationDefinition} is a sub-type of {@link Definition} and has 2 sub-types:
 * <ol>
 *    <li> {@link SelectionSet}
 *    <li> {@link TypedOperation}
 * </ol>
 * <p>
 * This is a {@link java.util.function functional interface} whose functional method is {@link #toGraphQLSpec()}.
 *
 * @see Definition
 * @see SelectionSet
 * @see TypedOperation
 */
@FunctionalInterface
public interface OperationDefinition extends Definition {

    // intentionally left blank
}
