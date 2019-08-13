/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.testhelpers.graphql.elements;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * {@link Node} represents the same concepts as the "node" in Relay's connection pattern
 * (https://graphql.org/learn/pagination/#pagination-and-edges).
 */
@RequiredArgsConstructor
public class Node implements Selection {

    private static final long serialVersionUID = 2525022170227460587L;

    /**
     * The selections inside the node.
     * <p>
     * For example
     *
     */
    @NonNull
    @Getter(AccessLevel.PRIVATE)
    private final SelectionSet fields;

    @Override
    public String toGraphQLSpec() {
        return String.format(
                "node %s",
                getFields().toGraphQLSpec()
        );
    }
}
