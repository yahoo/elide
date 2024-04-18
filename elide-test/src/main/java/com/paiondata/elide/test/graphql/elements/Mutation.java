/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.test.graphql.elements;

import lombok.NonNull;

/**
 * Represents GraphQL mutation query.
 */
public class Mutation extends TypedOperation {

    private static final long serialVersionUID = 1990970251076956044L;

    public Mutation(
            final String name,
            final VariableDefinitions variableDefinitions,
            @NonNull final SelectionSet selectionSet
    ) {
        super("mutation", name, variableDefinitions, selectionSet);
    }
}
