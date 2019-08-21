package com.yahoo.elide.contrib.testhelpers.graphql.elements;

import lombok.NonNull;

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
