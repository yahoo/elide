/*
 * Copyright 2017, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql.containers;

import com.yahoo.elide.graphql.Environment;
import com.yahoo.elide.graphql.NonEntityDictionary;
import com.yahoo.elide.graphql.PersistentResourceFetcher;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Container for nodes.
 */
@AllArgsConstructor
public class NonEntityContainer implements GraphQLContainer {
    @Getter private final Object nonEntity;

    @Override
    public Object processFetch(Environment context, PersistentResourceFetcher fetcher) {
        NonEntityDictionary nonEntityDictionary = fetcher.getNonEntityDictionary();

        String fieldName = context.field.getName();

        Object object = nonEntityDictionary.getValue(nonEntity, fieldName, context.requestScope);

        if (nonEntityDictionary.hasBinding(object.getClass())) {
            return new NonEntityContainer(object);
        }

        return object;
    }
}
