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
import java.util.Objects;

/**
 * {@link VariableDefinition} represents the same concept as
 * {@link graphql.language.VariableDefinition GraphQL VariableDefinition} but specializes in serialization, in
 * contrast to {@link graphql.language.VariableDefinition GraphQL VariableDefinition}, which is designed for
 * deserialization.
 * <p>
 * According to GraphQL grammar (6.0),
 * <pre>
 * {@code
 * variableDefinition : variable ':' type defaultValue?;
 *
 * variable : '$' name;
 * }
 * </pre>
 * A {@link VariableDefinition} is a key-value pair of string and type literal with optional default value for that
 * variable.
 */
@RequiredArgsConstructor
public class VariableDefinition implements Serializable {

    private static final long serialVersionUID = -899567129301468808L;

    /**
     * The "name" TOKEN defined in GraphQL grammar.
     * <p>
     * Note that there is no "$" in it.
     */
    @NonNull
    @Getter(AccessLevel.PRIVATE)
    private final String variable;

    /**
     * A simplified String representation of the aforementioned type literal.
     */
    @NonNull
    @Getter(AccessLevel.PRIVATE)
    private final String type;

    /**
     * Models the aforementioned default value.
     */
    @Getter(AccessLevel.PRIVATE)
    private final ValueWithVariable defaultValue;

    /**
     * Returns the query string that corresponds to the a {@link graphql.language.VariableDefinition} part
     * of a GraphQL query.
     *
     * @return a sub-string of a GraphQL query
     */
    public String toGraphQLSpec() {
        return String.format(
                "$%s: %s%s",
                getVariable(),
                getType(),
                Objects.isNull(getDefaultValue())
                        ? ""
                        : String.format("=%s", getDefaultValue().toGraphQLSpec())
        );
    }
}
