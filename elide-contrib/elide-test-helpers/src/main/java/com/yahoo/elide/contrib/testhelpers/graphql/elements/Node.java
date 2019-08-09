package com.yahoo.elide.contrib.testhelpers.graphql.elements;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class Node implements Selection {

    private static final long serialVersionUID = 2525022170227460587L;

    @NonNull
    @Getter(AccessLevel.PRIVATE)
    private final SelectionSet fields;

    @Override
    public String toGraphQLSpec() {
        return String.format(
                "node %s",
                getFields().toGraphQLSpec()
        );
    }
}
