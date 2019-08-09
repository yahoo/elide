/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.testhelpers.graphql.elements;

import java.io.Serializable;

/**
 * {@link Selection} represents the same concept as {@link graphql.language.Selection} but specializes in serialization,
 * in contrast to {@link graphql.language.Selection GraphQL Field Selection}, which is designed for deserialization.
 * <p>
 * According to GraphQL grammar (6.0)
 * <pre>
 * {@code
 * selection :
 *     field |
 *     fragmentSpread |
 *     inlineFragment;
 * }
 * </pre>
 * A {@link Selection} has 3 sub-types"
 * <ol>
 *     <li> {@link Field}
 *     <li> TODO - support fragmentSpread
 *     <li> TODO - support inlineFragment
 * </ol>
 *
 * This is a {@link java.util.function functional interface} whose functional method is {@link #toGraphQLSpec()}.
 *
 * @see Field
 */
public interface Selection extends Serializable {

    /**
     * Returns the query string that corresponds to a selection part of a GraphQL query.
     *
     * @return a sub-string of a GraphQL query
     */
    String toGraphQLSpec();
}
