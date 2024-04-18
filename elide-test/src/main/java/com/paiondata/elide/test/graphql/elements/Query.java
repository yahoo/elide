/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.test.graphql.elements;

import lombok.NonNull;

/**
 * Represents GraphQL named query.
 */
public class Query extends TypedOperation {

    private static final long serialVersionUID = 1990970251076956044L;

    public Query(
            final String name,
            final VariableDefinitions variableDefinitions,
            @NonNull final SelectionSet selectionSet
    ) {
        super("query", name, variableDefinitions, selectionSet);
    }
}
