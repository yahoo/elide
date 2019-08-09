/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.testhelpers.graphql.elements;

import java.io.Serializable;

/**
 * {@link Definition} represents the same concept as {@link graphql.language.Definition GraphQL Definition} but
 * specializes in serialization, in contrast to {@link graphql.language.Definition GraphQL Definition}, which is
 * designed for deserialization.
 * <p>
 * According to GraphQL grammar (6.0)
 * <pre>
 * {@code
 *     definition:
 *         operationDefinition |
 *         fragmentDefinition |
 *         typeSystemDefinition
 *         ;
 * }
 * </pre>
 * A {@link Definition} has 3 sub-types:
 * <ol>
 *     <li> {@link OperationDefinition}
 *     <li> TODO - support fragmentDefinition interface
 *     <li> TODO - support typeSystemDefinition interface
 * </ol>
 *
 * <p>
 * This is a {@link java.util.function functional interface} whose functional method is {@link #toGraphQLSpec()}.
 *
 * @see OperationDefinition
 */
@FunctionalInterface
public interface Definition extends Serializable {

    /**
     * Returns the query string that corresponds to the a {@link graphql.language.Definition} part
     * of a GraphQL query.
     *
     * @return a sub-string of a GraphQL query
     */
    String toGraphQLSpec();
}
