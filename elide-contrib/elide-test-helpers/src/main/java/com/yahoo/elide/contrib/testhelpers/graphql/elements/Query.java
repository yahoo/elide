package com.yahoo.elide.contrib.testhelpers.graphql.elements;

import lombok.NonNull;

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
