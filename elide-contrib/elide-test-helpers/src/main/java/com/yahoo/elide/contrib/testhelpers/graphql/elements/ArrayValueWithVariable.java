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

import java.util.List;
import java.util.stream.Collectors;

/**
 * According to GraphQL grammar (6.0):
 * <pre>
 * {@code
 * valueWithVariable :
 * ... |
 * arrayValueWithVariable |
 * ...
 *
 * arrayValueWithVariable: '[' valueWithVariable* ']';
 * }
 * </pre>
 * {@code arrayValueWithVariable} is a space-separated list of {@link ValueWithVariable} surrounded by a pair of squre
 * brackets. The list can be empty.
 * <p>
 * For example, {@link ArrayValueWithVariable} containing {@link StringValue strings} serializes to
 * <pre>
 * {@code
 * ["foo", "bar", "bat"]
 * }
 * </pre>
 */
@RequiredArgsConstructor
public class ArrayValueWithVariable implements ValueWithVariable {

    private static final long serialVersionUID = 1338072626866717717L;

    /**
     * The list of {@link ValueWithVariable}'s.
     */
    @NonNull
    @Getter(AccessLevel.PRIVATE)
    private final List<ValueWithVariable> arrayValues;

    @Override
    public String toGraphQLSpec() {
        return String.format(
                "[%s]",
                getArrayValues().stream()
                        .map(ValueWithVariable::toGraphQLSpec)
                        .collect(Collectors.joining(", "))
        );
    }
}
