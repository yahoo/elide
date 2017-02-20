package com.yahoo.elide.graphql;

import graphql.schema.GraphQLEnumType;

import static graphql.schema.GraphQLEnumType.newEnum;

public enum RelationshipOp {
    FETCH,
    DELETE,
    ADD,
    REPLACE;

    public static GraphQLEnumType toGraphQLType() {
        GraphQLEnumType.Builder builder = newEnum()
                .name("op")
                .description("Relationship operations.");

        for (RelationshipOp op : RelationshipOp.values()) {
            builder.value(op.name(), op);
        }

        return builder.build();
    }
}
