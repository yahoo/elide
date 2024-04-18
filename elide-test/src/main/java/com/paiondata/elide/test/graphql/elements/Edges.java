/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.test.graphql.elements;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

/**
 * {@link Edges} represents the same concepts as the edges in Relay's connection pattern
 * (https://graphql.org/learn/pagination/#pagination-and-edges).
 * <p>
 * {@link Edges} is a list of {@link Node}s.
 *
 * @see Node
 */
@RequiredArgsConstructor
public class Edges extends Selection {

    private static final long serialVersionUID = 1860183539630769326L;

    /**
     * Models the list of {@link Node}s.
     */
    @Getter
    @NonNull
    private final List<Node> nodes;

    /**
     * Returns the query string that corresponds to the edges that connects sub-graphs in a GraphQL query.
     *
     * @return a sub-string of a GraphQL query
     */
    @Override
    public String toGraphQLSpec() {
        return String.format("edges {%s}", getNodes().get(0).toGraphQLSpec());
    }

    @Override
    public String toResponse() {
        return String.format("\"edges\":[%s]", attachNodes(getNodes()));
    }

    /**
     * Serializes a list of nodes represented by a list of {@link SelectionSet} into GraphQL response data format.
     * <p>
     * For example,
     * <pre>
     * {@code
     * {
     *     "node" {
     *         "id": "1",
     *         "title": "Effective Java",
     *         "authors": ...
     *     }
     * },
     * {
     *     "node" {
     *         "id": "2",
     *         "title": "JVM Specification",
     *         "authors": ...
     *     }
     * }
     * }
     * </pre>
     * <p>
     * If the list is empty, this method returns empty string.
     *
     * @param nodes  Objects each containing all fields of an instantiated entity
     *
     * @return a sub-string of response data
     */
    private static String attachNodes(List<Node> nodes) {
        if (nodes.isEmpty()) {
            /* we want response to be something like
             *
             * "book": {
             *   "edges": []
             * }
             */
            return "";
        }

        return nodes.stream()
                .map(Node::toResponse)
                .collect(Collectors.joining(","));
    }
}
