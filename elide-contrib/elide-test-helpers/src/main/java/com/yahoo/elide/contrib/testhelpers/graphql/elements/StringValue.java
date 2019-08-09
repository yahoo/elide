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

/**
 * A {@link StringValue} is a string value that is quoted in its serialized form.
 *
 * @see EnumValue
 */
@RequiredArgsConstructor
public class StringValue implements ValueWithVariable {

    private static final long serialVersionUID = -5602245524578931206L;

    @NonNull
    @Getter(AccessLevel.PRIVATE)
    private final String value;

    @Override
    public String toGraphQLSpec() {
        // We quote it due to the GraphQL grammar for string stringValue:
        // StringValue: '"' (~(["\\\n\r\u2028\u2029])|EscapedChar)* '"';
        return String.format("\"%s\"", getValue());
    }
}
