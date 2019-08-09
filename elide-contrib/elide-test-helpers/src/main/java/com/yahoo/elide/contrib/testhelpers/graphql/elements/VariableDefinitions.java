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

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

/**
 * {@link VariableDefinitions} is a serializable object that models the concept of GraphQL {@code variableDefinitions
 * } in its grammar.
 * <p>
 * According to GraphQL grammar (6.0)
 * <pre>
 * {@code
 * variableDefinitions : '(' variableDefinition+ ')';
 * }
 * </pre>
 * {@link VariableDefinitions} is a space-separated list of {@link VariableDefinition}s surrounded by a pair of
 * parenthesis.
 *
 * @see {@link VariableDefinitions}
 */
@RequiredArgsConstructor
public class VariableDefinitions implements Serializable {

    /**
     * Models the space-separated list of {@link VariableDefinition}s.
     */
    @NonNull
    @Getter(AccessLevel.PRIVATE)
    private final List<VariableDefinition> variableDefinitions;

    /**
     * Returns a GraphQL query string that representing an set of variable definition
     *
     * @return a sub-string of a GraphQL query
     */
    public String toGraphQLSpec() {
        return String.format(
                "(%s)",
                getVariableDefinitions().stream()
                        .map(VariableDefinition::toGraphQLSpec)
                        .collect(Collectors.joining(" "))
        );
    }
}
