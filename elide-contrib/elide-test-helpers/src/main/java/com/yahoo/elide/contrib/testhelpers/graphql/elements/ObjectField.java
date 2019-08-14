/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.testhelpers.graphql.elements;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * {@link ObjectField} represents the same concept as GraphQL {@link graphql.language.ObjectField}, but specializes
 * in serialization, in contrast to GraphQL{@link graphql.language.ObjectField}, which is designed for deserialization.
 * <p>
 * According to GraphQL grammar (6.0):
 * <pre>
 * {@code
 * field : alias? name arguments? directives? selectionSet? = field : name
 * }
 * </pre>
 * A {@link ObjectField} is a name with optional alias, arguments, directives, or selection set. In this implementation,
 * arguments and directives are supported.
 * <p>
 * TODO - support directives
 */
@RequiredArgsConstructor
public final class ObjectField implements Field {

    private static final long serialVersionUID = -7787366146853612038L;

    /**
     * The "name" TOKEN defined in GraphQL grammar.
     * <p>
     * alias : name ':';
     */
    @Getter(AccessLevel.PRIVATE)
    private final String alias;

    /**
     * The "name" TOKEN defined in GraphQL grammar.
     */
    @NonNull
    @Getter(AccessLevel.PRIVATE)
    private final String name;

    /**
     * Models "arguments".
     */
    @NonNull
    @Getter(AccessLevel.PRIVATE)
    private final Arguments arguments;

    /**
     * Models a "selections set".
     */
    @NonNull
    @Getter(AccessLevel.PRIVATE)
    private final SelectionSet selectionSet;

    @Override
    public String toGraphQLSpec() {
        return String.format(
                "%s%s%s %s",
                getAlias() == null ? "" : String.format("%s: ", getAlias()),
                getName(),
                getArguments().noArgument()
                        ? ""
                        : getArguments().toGraphQLSpec(),
                getSelectionSet().toGraphQLSpec());
    }
}
