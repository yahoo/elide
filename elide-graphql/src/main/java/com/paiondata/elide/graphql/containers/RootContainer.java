/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.graphql.containers;

import com.paiondata.elide.graphql.Environment;
import com.paiondata.elide.graphql.PersistentResourceFetcher;

/**
 * Root container for GraphQL requests.
 */
public class RootContainer implements GraphQLContainer {

    @Override
    public GraphQLContainer processFetch(Environment context) {
        String entityName = context.field.getName();
        String aliasName = context.field.getAlias();

        return PersistentResourceFetcher.fetchObject(
                context.requestScope,
                context.requestScope
                        .getProjectionInfo()
                        .getProjection(aliasName, entityName), // root-level projection
                context.ids
        );
    }
}
