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
 * A simplified object that models a GraphQL object in a single string.
 * <p>
 * {@link ObjectValueWithVariable} implements the {@code objectValueWithVariable} defined in GraphQL grammar.
 *
 * @see ValueWithVariable
 */
@RequiredArgsConstructor
public class ObjectValueWithVariable implements ValueWithVariable {

    private static final long serialVersionUID = 6768988285154422347L;

    @NonNull
    @Getter(AccessLevel.PRIVATE)
    private final String object;

    @Override
    public String toGraphQLSpec() {
        return String.format("{%s}", getObject());
    }
}
