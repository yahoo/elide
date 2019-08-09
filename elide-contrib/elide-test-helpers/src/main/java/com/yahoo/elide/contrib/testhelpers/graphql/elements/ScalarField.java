/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.testhelpers.graphql.elements;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * A {@link ScalarField} represents the leaf of a GraphQL query.
 * <p>
 * According to GraphQL grammar (6.0):
 * <pre>
 * {@code
 * field : alias? name arguments? directives? selectionSet? = field : name
 * }
 * </pre>
 * A {@link ScalarField} is simply a un-quotes string value (name).
 *
 * @see Field
 */
@RequiredArgsConstructor
public class ScalarField implements Field {

    private static final long serialVersionUID = -7781299062335338345L;

    /**
     * The "name" TOKEN defined in GraphQL grammar.
     */
    @Getter
    @NonNull
    private final String name;

    @Override
    public String toGraphQLSpec() {
        return getName();
    }
}
