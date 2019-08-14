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

    /**
     * Returns the complete GraphQL response that this {@link Document} represents.
     * <p>
     * In the case of multiple {@link Definition response definitions}, a pair of square brackets is surrounding the
     * entire response. For example:
     * <pre>
     * {@code
     * {
     *     "data": {
     *         "book": {
     *             "edges": [
     *                 {
     *                     "node": {
     *                         "id": "1",
     *                         "title": "1984",
     *                         "authors": {
     *                             "edges": [
     *                                 {
     *                                     "node": {
     *                                         "id": "1",
     *                                         "name": "George Orwell"
     *                                     }
     *                                 }
     *                             ]
     *                         }
     *                     }
     *                 },
     *                 {
     *                     "node": {
     *                         "id": "2",
     *                         "title": "Grapes of Wrath",
     *                         "authors": {
     *                             "edges": [
     *                                 {
     *                                     "node": {
     *                                         "id": "2",
     *                                         "name": "John Setinbeck"
     *                                     }
     *                                 }
     *                             ]
     *                         }
     *                     }
     *                 }
     *             ]
     *         }
     *     }
     * }
     *
     * [
     *     {
     *         "data": {
     *             "book": {
     *                 "edges": [
     *                     {
     *                         "node": {
     *                             "id": "4",
     *                             "title": "my book created in batch!"
     *                         }
     *                     }
     *                 ]
     *             }
     *         }
     *     },
     *     {
     *         "data": {
     *             "book": {
     *                 "edges": [
     *                     {
     *                         "node": {
     *                             "id": "4",
     *                             "title": "my book created in batch!"
     *                         }
     *                     }
     *                 ]
     *             }
     *         }
     *     }
     * ]
     * }
     * </pre>
     *
     * @return a string representation of a GraphQL response
     */
    public String toResponse() {
        return String.format(
                getDefinitions().size() == 1 ? "%s" : "[%s]",
                getDefinitions().stream()
                        .map(definition -> String.format("{\"data\":%s}", definition.toGraphQLSpec()))
                        .collect(Collectors.joining(","))
        );
    }
}
