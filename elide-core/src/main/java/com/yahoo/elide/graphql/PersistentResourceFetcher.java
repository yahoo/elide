/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql;

import com.yahoo.elide.core.PersistentResource;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

/**
 * Interacts with {@link PersistentResource} to fetch data for GraphQL.
 */
public class PersistentResourceFetcher implements DataFetcher {

    @Override
    public Object get(DataFetchingEnvironment dataFetchingEnvironment) {
        return null;
    }
}
