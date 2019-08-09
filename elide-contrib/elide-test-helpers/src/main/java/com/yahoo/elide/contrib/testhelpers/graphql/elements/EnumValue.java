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
 * A {@link EnumValue} is a string value that is not quoted in its serialized form.
 * <p>
 * {@link EnumValue} maps to the same concept of a GraphQL {@code enumValue} as defined below
 * <pre>
 * {@code
 * enumValue : name ;
 * }
 * </pre>
 *
 * @see StringValue
 */
@RequiredArgsConstructor
public class EnumValue implements ValueWithVariable {

    private static final long serialVersionUID = 2865944895388908470L;

    /**
     * The "name" TOKEN defined in GraphQL grammar.
     */
    @NonNull
    @Getter(AccessLevel.PRIVATE)
    private final String value;

    @Override
    public String toGraphQLSpec() {
        return getValue();
    }
}
