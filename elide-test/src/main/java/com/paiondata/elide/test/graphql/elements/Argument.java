/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.test.graphql.elements;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;

/**
 * {@link Argument} represents the same concept as {@link graphql.language.Argument GraphQL Argument} but specializes
 * in serialization, in contrast to {@link graphql.language.Argument GraphQL Argument}, which is designed for
 * deserialization.
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
    private final Object value;

    /**
     * Returns a GraphQL query string that representing an argument.
     *
     * @return a sub-string of a GraphQL query
     */
    public String toGraphQLSpec() {
        return String.format("%s: %s", getName(), getValue());
    }
}
