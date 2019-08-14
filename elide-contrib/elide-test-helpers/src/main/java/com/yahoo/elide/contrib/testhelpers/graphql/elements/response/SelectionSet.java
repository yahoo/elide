/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.testhelpers.graphql.elements.response;

import com.yahoo.elide.contrib.testhelpers.graphql.elements.OperationDefinition;
import com.yahoo.elide.contrib.testhelpers.graphql.elements.Selection;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.LinkedHashSet;
import java.util.stream.Collectors;

/**
 * {@link SelectionSet} is a serializable object that models the concept of a group of field selection in GraphQL
 * response.
 *
 * @see com.yahoo.elide.contrib.testhelpers.graphql.elements.SelectionSet
 */
@RequiredArgsConstructor
public class SelectionSet implements OperationDefinition {

    private static final long serialVersionUID = -4210505655442694900L;

    /**
     * Models {@code selection+}.
     */
    @Getter
    @NonNull
    private final LinkedHashSet<Selection> selections;

    @Override
    public String toGraphQLSpec() {
        return String.format(
                "{%s}",
                getSelections().stream().map(Selection::toGraphQLSpec)
                .collect(Collectors.joining(", "))
        );
    }
}
