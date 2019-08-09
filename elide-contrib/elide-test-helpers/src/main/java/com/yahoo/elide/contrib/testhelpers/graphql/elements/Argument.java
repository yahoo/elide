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

/**
 * {@link Argument} represents the same concept as {@link graphql.language.Argument GraphQL Argument} but specializes
 * in serialization, in contrast to {@link graphql.language.Argument GraphQL Argument}, which is designed for
 * deserialization.
 * <p>
 * According to GraphQL grammar (6.0)
 * <pre>
 * {@code
 * argument : name ':' valueWithVariable;
 * }
 * </pre>
 * An {@link Argument} is a pair of argument name and {@link ValueWithVariable argument value}.
 *
 * @see <a href="https://graphql.org/learn/queries/#arguments">GraphQL Arguments</a>
 */
@RequiredArgsConstructor
public class Argument implements Serializable {

    private static final long serialVersionUID = 2346978495975190484L;

    /**
     * The "name" TOKEN defined in GraphQL grammar.
     */
    @NonNull
    @Getter(AccessLevel.PRIVATE)
    private final String name;

    /**
     * A GraphQL concept that defines argument value.
     */
    @NonNull
    @Getter(AccessLevel.PRIVATE)
    private final ValueWithVariable value;

    /**
     * Returns a GraphQL query string that representing an argument
     *
     * @return a sub-string of a GraphQL query
     */
    public String toGraphQLSpec() {
        return String.format("%s: %s", getName(), getValue().toGraphQLSpec());
    }
}
