/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.test.graphql.elements;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.stream.Collectors;

/**
 * {@link Node} represents the same concepts as the "node" in Relay's connection pattern
 * (https://graphql.org/learn/pagination/#pagination-and-edges).
 */
@RequiredArgsConstructor
public class Node extends Selection {

    private static final long serialVersionUID = 2525022170227460587L;

    /**
     * The selections inside the node.
     * <p>
     * For example
     *
     */
    @NonNull
    @Getter
    private final SelectionSet fields;

    @Override
    public String toGraphQLSpec() {
        return String.format(
                "node %s",
                getFields().toGraphQLSpec()
        );
    }

    @Override
    public String toResponse() {
        return String.format("{\"node\":{%s}}", serializeNode(this));
    }

    /**
     * Serializes a node represented by a {@link SelectionSet} into GraphQL response data format.
     *
     * @param node  An object containing all fields of an instantiated entity
     *
     * @return a sub-string of response data
     */
    private static String serializeNode(Node node) {
        return node.getFields().getSelections().stream()
                .map(Selection::toResponse)
                .collect(Collectors.joining(","));
    }
}
