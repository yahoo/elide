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
 * {@link Field} represents the same concept as {@link graphql.language.Field GraphQL Field} but specializes in
 * serialization, in contrast to {@link graphql.language.Field GraphQL Field}, which is designed for deserialization.
 * <p>
 * According to GraphQL grammar (6.0):
 * <pre>
 * {@code
 *     selection :
 *         field |
 *         fragmentSpread |
 *         inlineFragment;
 *
 *     field : alias? name arguments? directives? selectionSet?;
 * }
 * </pre>
 * A {@link Field} is a sub-type of {@link Selection}. The simplest {@link Field} is a scalar field; otherwise it would
 * be an object field
 *
 * @see Selection
 * @see <a href="https://graphql.org/learn/schema/#scalar-types">Scalar types</a>
 * @see <a href="https://graphql.org/learn/schema/#object-types-and-fields">Object types and fields</a>
 */
@RequiredArgsConstructor
public class Field extends Selection {

    private static final long serialVersionUID = -5906705888838083150L;

    public static Field scalarField(String name) {
        return new Field(null, name, Arguments.emptyArgument(), null);
    }

    public static String quoteValue(String value) {
        return String.format("\"%s\"", value.replace("\"", "\\\""));
    }

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
    @Getter(AccessLevel.PRIVATE)
    private final Serializable selectionSet;

    @Override
    public String toGraphQLSpec() {
        return String.format(
                "%s%s%s%s",
                getAlias() == null ? "" : getAlias() + ": ",
                getName(),
                argument(),
                selection()
        );
    }

    @Override
    public String toResponse() {
        if (selectionSet instanceof String || selectionSet instanceof Number) {
            // scalar response field
            return String.format(
                    "\"%s\":%s",
                    getName(),
                    getSelectionSet().toString().equals("")
                            ? "{\"edges\":[]}"
                            : getSelectionSet().toString()
            );
        }

        if (getSelectionSet() == null) {
            return String.format("\"%s\":%s", getName(), null);
        }
        // object response field
        return String.format("\"%s\":%s", getName(), ((SelectionSet) getSelectionSet()).toResponse());
    }

    private String argument() {
        return getArguments().noArgument()
                ? ""
                : getArguments().toGraphQLSpec();
    }

    private String selection() {
        return getSelectionSet() == null ? "" : " " + ((SelectionSet) getSelectionSet()).toGraphQLSpec();
    }
}
