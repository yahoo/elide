/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql;

import graphql.schema.GraphQLEnumType;

import static graphql.schema.GraphQLEnumType.newEnum;

/**
 * Specifies how a relationship should be modified.
 */
public enum RelationshipOp {
    FETCH,
    DELETE,
    ADD,
    REPLACE;

    /**
     * Converts this enum to a graphQL schema type.
     * @return graphql enum type
     */
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
