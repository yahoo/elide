/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.testhelpers.graphql.elements.response;

import com.yahoo.elide.contrib.testhelpers.graphql.elements.Selection;
import com.yahoo.elide.contrib.testhelpers.graphql.elements.SelectionSet;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Response {@link ObjectField} is an instantiated query
 * {@link com.yahoo.elide.contrib.testhelpers.graphql.elements.ObjectField} and is used to construct GraphQL response.
 * <p>
 * For example,
 * <pre>
 * +--------------+-----------------------------+
 * |    Query     |           Response          |
 * +--------------+-----------------------------+
 * | {            | {                           |
 * |     hero {   |     "data": {               |
 * |         name |         "hero": {           |
 * |     }        |             "name": "R2-D2" |
 * | }            |         }                   |
 * |              |     }                       |
 * |              | }                           |
 * +--------------+------------------------------+
 * </pre>
 * The response {@link ObjectField} of
 * <pre>
 * {@code
 * "hero": {
 *     "name": "R2-D2"
 * }
 * }
 * </pre>
 * is an instantiation of the query {@link com.yahoo.elide.contrib.testhelpers.graphql.elements.ObjectField}:
 * <pre>
 * {@code
 * hero {
 *     name
 * }
 * }
 * </pre>
 */
@RequiredArgsConstructor
public class ObjectField implements Selection {

    private static final long serialVersionUID = -5720704831334893003L;

    /**
     * The "name" TOKEN defined in GraphQL grammar.
     */
    @Getter(AccessLevel.PRIVATE)
    private final String name;

    /**
     * Models the instantiation, i.e. all entity instances
     */
    @Getter(AccessLevel.PRIVATE)
    private final List<SelectionSet> nodes;

    @Override
    public String toGraphQLSpec() {
        return String.format(
                "\"%s\":{\"edges\":[%s]}",
                getName(),
                attachNodes(getNodes())
        );
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
    private static String attachNodes(List<SelectionSet> nodes) {
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
                .map(node -> String.format("{\"node\":{%s}}", serializeNode(node)))
                .collect(Collectors.joining(","));
    }

    /**
     * Serializes a node represented by a {@link SelectionSet} into GraphQL response data format.
     *
     * @param node  An object containing all fields of an instantiated entity
     *
     * @return a sub-string of response data
     */
    private static String serializeNode(SelectionSet node) {
        return node.getSelections().stream()
                .map(Selection::toGraphQLSpec)
                .collect(Collectors.joining(","));
    }
}
