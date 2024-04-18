/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.test.graphql.elements;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.LinkedHashSet;
import java.util.stream.Collectors;

/**
 * {@link SelectionSet} is a serializable object that models the concept of a group of field selection in GraphQL.
 */
@RequiredArgsConstructor
public class SelectionSet extends Definition {

    private static final long serialVersionUID = -2777133166190446552L;

    /**
     * Models {@code selection+}.
     */
    @Getter
    @NonNull
    private final LinkedHashSet<Selection> selections;

    @Override
    public String toGraphQLSpec() {
        return getSelections().stream().map(Selection::toGraphQLSpec)
                .collect(Collectors.joining(" ", "{", "}"));
    }

    @Override
    String toResponse() {
        return getSelections().stream().map(Selection::toResponse)
                .collect(Collectors.joining(", ", "{", "}"));
    }
}
