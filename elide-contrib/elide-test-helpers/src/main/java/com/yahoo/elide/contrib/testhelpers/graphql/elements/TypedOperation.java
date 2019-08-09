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
 * According to GraphQL grammar (6.0):
 * <pre>
 * {@code
 * operationType  name? variableDefinitions? directives? selectionSet;
 *
 * operationType : SUBSCRIPTION | MUTATION | QUERY;
 * }
 * </pre>
 * A {@link TypedOperation} comes with required {@code operation type} and {@link SelectionSet} along with optional
 * operation name and variables used in query.
 */
@RequiredArgsConstructor
public class TypedOperation implements OperationDefinition {

    /**
     * Models the required {@code operation type}.
     */
    @RequiredArgsConstructor
    public enum OperationType implements Serializable {

        /**
         * {@code operationType : SUBSCRIPTION} token.
         */
        SUBSCRIPTION("subscription"),

        /**
         * {@code operationType : MUTATION} token.
         */
        MUTATION("mutation"),

        /**
         * {@code operationType : QUERY} token.
         */
        QUERY("query");

        /**
         * An ANTLR token.
         */
        @NonNull
        @Getter(AccessLevel.PRIVATE)
        private final String name;

        /**
         * Returns this token in string form.
         *
         * @return a sub-string of a GraphQL query
         */
        public String toGraphQLSpec() {
            return getName();
        }
    }

    private static final long serialVersionUID = 5049217577677973567L;

    /**
     * Models the required {@code operation type}.
     */
    @NonNull
    @Getter(AccessLevel.PRIVATE)
    private final OperationType operationType;

    /**
     * The "name" TOKEN defined in GraphQL grammar.
     */
    @Getter(AccessLevel.PRIVATE)
    private final String name;

    /**
     * Models the {@code variableDefinitions}.
     */
    @Getter(AccessLevel.PRIVATE)
    private final VariableDefinitions variableDefinitions;

    /**
     * Models the required {@link SelectionSet}.
     */
    @NonNull
    @Getter(AccessLevel.PRIVATE)
    private final SelectionSet selectionSet;

    @Override
    public String toGraphQLSpec() {
        return String.format(
                "%s %s%s%s",
                getOperationType().toGraphQLSpec(),
                getName() == null ? "" : getName(),
                getVariableDefinitions() == null
                        ? ""
                        : getVariableDefinitions().toGraphQLSpec() + " ",
                getSelectionSet().toGraphQLSpec()
        );
    }
}
