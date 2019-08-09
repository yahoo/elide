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
 * {@link Document} represents the same concept as {@link graphql.language.Document GraphQL Document} but
 * specializes in serialization, in contrast to {@link graphql.language.Document GraphQL Document}, which is
 * designed for deserialization.
 * <p>
 * According to GraphQL grammar (6.0)
 * <pre>
 * {@code
 * document : definition+;
 * }
 * </pre>
 * A {@link Document} is a list of one or more {@link Definition}s.
 */
@RequiredArgsConstructor
public class Document implements Serializable {

    private static final long serialVersionUID = -1388610970958604545L;

    /**
     * Models the list of one or more {@link Definition}s.
     */
    @NonNull
    @Getter(AccessLevel.PRIVATE)
    private final List<Definition> definitions;

    /**
     * Returns the complete GraphQL query that this {@link Document} represents.
     *
     * @return a string representation of a GraphQL query
     */
    public String toQuery() {
        return getDefinitions().stream()
                .map(Definition::toGraphQLSpec)
                .collect(Collectors.joining(" "));
    }
}
