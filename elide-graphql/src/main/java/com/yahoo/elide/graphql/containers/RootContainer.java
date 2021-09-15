/*
 * Copyright 2017, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql.containers;

import com.yahoo.elide.graphql.ElideDataFetcher;
import com.yahoo.elide.graphql.Environment;

/**
 * Root container for GraphQL requests.
 */
public class RootContainer implements GraphQLContainer {

    @Override
    public GraphQLContainer processFetch(Environment context, ElideDataFetcher fetcher) {
        String entityName = context.field.getName();
        String aliasName = context.field.getAlias();

        return fetcher.fetchObject(
                context.requestScope,
                context.requestScope
                        .getProjectionInfo()
                        .getProjection(aliasName, entityName), // root-level projection
                context.ids
        );
    }
}
