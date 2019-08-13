/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.testhelpers.graphql.elements.response;

import com.yahoo.elide.contrib.testhelpers.graphql.elements.Field;
import com.yahoo.elide.contrib.testhelpers.graphql.elements.Selection;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Response {@link ScalarField} is an instantiated query
 * {@link com.yahoo.elide.contrib.testhelpers.graphql.elements.ScalarField} and is used to construct GraphQL response.
 * <p>
 * For example,
 * <pre>
 * +--------------+-----------------------------+
 * |    Query     |           Response          |
 * +--------------+-----------------------------+
 * | {            | {                           |
 * |     hero {   |     "data": {               |
 * |         name |         "hero": {           |
 * |     }        |             "name": "R2-D2" |
 * | }            |         }                   |
 * |              |     }                       |
 * |              | }                           |
 * +--------------+------------------------------+
 * </pre>
 * The response {@link ScalarField} of
 * <pre>
 * {@code
 * "name": "R2-D2"
 * }
 * </pre>
 * is an instantiation of the query {@link com.yahoo.elide.contrib.testhelpers.graphql.elements.ScalarField}:
 * <pre>
 * {@code
 * name
 * </pre>
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ScalarField implements Selection {

    /**
     * Creates a {@link ScalarField} whose value is quoted, e.g. "R2-D2".
     *
     * @param name  The field name
     * @param value  The instantiated field value
     *
     * @return a response scalar field
     *
     * @see #withUnquotedValue(String, String)
     * @see <a href="https://graphql.org/learn/queries/#fields">Fields</a>
     * @see <a href="https://graphql.org/learn/schema/#scalar-types">Scalar Field</a>
     */
    public static ScalarField withQuotedValue(String name, String value) {
        return new ScalarField(name, value, true);
    }

    /**
     * Creates a {@link ScalarField} whose value is not quoted, e.g. R2-D2.
     *
     * @param name  The field name
     * @param value  The instantiated field value
     *
     * @return a response scalar field
     *
     * @see #withQuotedValue(String, String)
     * @see <a href="https://graphql.org/learn/queries/#fields">Fields</a>
     * @see <a href="https://graphql.org/learn/schema/#scalar-types">Scalar Field</a>
     */
    public static ScalarField withUnquotedValue(String name, String value) {
        return new ScalarField(name, value, false);
    }

    private static final long serialVersionUID = -7561327546037080578L;

    /**
     * The "name" TOKEN defined in GraphQL grammar.
     */
    @NonNull
    @Getter(AccessLevel.PRIVATE)
    private final String name;

    /**
     * The value of an instantiated field.
     */
    @Getter(AccessLevel.PRIVATE)
    private final String value;

    /**
     * Some values are not quoted in response such as {@code null} or a serialized {@link Map} or {@link List}.
     */
    @Getter(AccessLevel.PRIVATE)
    private final boolean quotedValue;

    @Override
    public String toGraphQLSpec() {
        return isQuotedValue()
                ? String.format("\"%s\":\"%s\"", getName(), getValue())
                : String.format("\"%s\":%s", getName(), getValue());
    }
}
