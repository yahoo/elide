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
 * {@link VariableValue} models a variable name following a {@code $} sign.
 */
@RequiredArgsConstructor
public class VariableValue implements ValueWithVariable {

    private static final long serialVersionUID = -7483470663100670977L;

    @NonNull
    @Getter(AccessLevel.PRIVATE)
    private final String name;

    @Override
    public String toGraphQLSpec() {
        // variable : '$' name;
        return String.format("$%s", getName());
    }
}
