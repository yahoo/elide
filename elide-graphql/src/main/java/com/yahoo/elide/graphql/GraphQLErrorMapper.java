/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql;

import com.yahoo.elide.ElideError;

import graphql.GraphQLError;

/**
 * Maps {@link ElideError} to {@link GraphQLError}.
 */
public interface GraphQLErrorMapper {
    /**
     * Maps {@link ElideError} to {@link GraphQLError}.
     *
     * @param error the {@link ElideError} to map
     * @return the mapped {@link GraphQLError}
     */
    GraphQLError toGraphQLError(ElideError error);
}
