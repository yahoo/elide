/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql.subscriptions;

import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.request.EntityProjection;
import com.yahoo.elide.core.request.Relationship;
import com.yahoo.elide.graphql.ElideDataFetcher;
import com.yahoo.elide.graphql.NonEntityDictionary;
import com.yahoo.elide.graphql.containers.CollectionContainer;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SubscriptionDataFetcher extends ElideDataFetcher implements DataFetcher<Object> {

    public SubscriptionDataFetcher(NonEntityDictionary nonEntityDictionary) {
        super(nonEntityDictionary);
    }

    @Override
    public Object get(DataFetchingEnvironment environment) throws Exception {
        /* fetch arguments in mutation/query */
        Map<String, Object> args = environment.getArguments();

        return null;
    }

    @Override
    public CollectionContainer fetchRelationship(
            PersistentResource<?> parentResource,
            Relationship relationship,
            Optional<List<String>> ids) {
        return null;
    }

    @Override
    public CollectionContainer fetchObject(
            RequestScope requestScope,
            EntityProjection projection,
            Optional<List<String>> ids) {
        return null;
    }
}
