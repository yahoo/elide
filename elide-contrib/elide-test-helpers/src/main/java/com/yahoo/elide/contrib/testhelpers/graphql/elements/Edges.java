/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.testhelpers.graphql.elements;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * {@link Edges} represents the same concepts as the edges in Relay's connection pattern
 * (https://graphql.org/learn/pagination/#pagination-and-edges).
 * <p>
 * {@link Edges} is a list of {@link Node}s.
 *
 * @see Node
 */
@RequiredArgsConstructor
public class Edges implements Selection {

    private static final long serialVersionUID = 1860183539630769326L;

    /**
     * Models the list of {@link Node}s.
     */
    @Getter
    @NonNull
    private final Node node;

    /**
     * Returns the query string that corresponds to the edges that connects sub-graphs in a GraphQL query.
     *
     * @return a sub-string of a GraphQL query
     */
    @Override
    public String toGraphQLSpec() {
        return String.format("edges {%s}", getNode().toGraphQLSpec());
    }
}
