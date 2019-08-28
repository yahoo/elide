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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * {@link Arguments} is a serializable object that models the concept of GraphQL arguments.
 * <p>
 * According to GraphQL grammar (6.0)
 * <pre>
 * {@code
 * arguments : '(' argument+ ')';
 * }
 * </pre>
 * Arguments is a space-separated list of {@link Argument}s surrounded by a pair of parenthesis.
 *
 * @see Argument
 */
@RequiredArgsConstructor
public class Arguments implements Serializable {

    /**
     * Creates an empty list of {@link Arguments arguments}.
     *
     * @return no argument
     */
    public static Arguments emptyArgument() {
        return new Arguments(Collections.emptyList());
    }

    private static final long serialVersionUID = 1312779653134889019L;

    /**
     * Models {@code argument+}.
     */
    @NonNull
    @Getter(AccessLevel.PRIVATE)
    List<Argument> arguments;

    /**
     * Returns a GraphQL query string that representing an set of arguments
     *
     * @return a sub-string of a GraphQL query
     */
    public String toGraphQLSpec() {
        return String.format(
                "(%s)",
                getArguments().stream()
                        .map(Argument::toGraphQLSpec)
                        .collect(Collectors.joining(" "))
                );
    }

    /**
     * Returns whether or not this {@link Arguments} is empty, i.e. no argument is to be serialized.
     *
     * @return {@code true} if serializing this {@link Arguments} is skipped
     */
    public boolean noArgument() {
        return getArguments().isEmpty();
    }
}
