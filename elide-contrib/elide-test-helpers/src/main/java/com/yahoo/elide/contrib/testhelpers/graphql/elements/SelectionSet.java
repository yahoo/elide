/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.testhelpers.graphql.elements;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.LinkedHashSet;
import java.util.stream.Collectors;

/**
 * {@link SelectionSet} is a serializable object that models the concept of a group of field selection in GraphQL.
 * <p>
 * According to GraphQL grammar (6.0)
 * <pre>
 * {@code
 * operationDefinition:
 *     selectionSet |
 *     operationType  name? variableDefinitions? directives? selectionSet;
 *
 * selectionSet :  '{' selection+ '}';
 * }
 * </pre>
 * SelectionSet is a sub-type of {@link OperationDefinition} and is a space-separated list of {@link Selection}s
 * surrounded by a pair of curly braces.
 *
 * @see Selection
 */
@RequiredArgsConstructor
public class SelectionSet implements OperationDefinition {

    private static final long serialVersionUID = -2777133166190446552L;

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
                .collect(Collectors.joining(" "))
        );
    }
}
