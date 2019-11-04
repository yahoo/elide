/*
 * Copyright 2017, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql.containers;

import com.yahoo.elide.graphql.Environment;
import com.yahoo.elide.graphql.PersistentResourceFetcher;

/**
 * Root container for GraphQL requests.
 */
public class RootContainer implements GraphQLContainer {
    @Override
    public Object processFetch(Environment context, PersistentResourceFetcher fetcher) {
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
