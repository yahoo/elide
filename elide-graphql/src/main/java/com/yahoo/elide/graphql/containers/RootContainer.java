/*
 * Copyright 2017, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql.containers;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.graphql.Environment;
import com.yahoo.elide.graphql.PersistentResourceFetcher;

import graphql.language.Field;

/**
 * Root container for GraphQL requests.
 */
public class RootContainer implements GraphQLContainer {
    @Override
    public Object processFetch(Environment context, PersistentResourceFetcher fetcher) {
        EntityDictionary dictionary = context.requestScope.getDictionary();
        Class<?> entityClass = dictionary.getEntityClass(context.field.getName());
        boolean generateTotals = requestContainsPageInfo(context.field);
        return fetcher.fetchObject(context, context.requestScope, entityClass, context.ids,
                context.sort, context.offset, context.first, context.filters, generateTotals);
    }

    public static boolean requestContainsPageInfo(Field field) {
        return field.getSelectionSet().getSelections().stream()
                .anyMatch(f -> f instanceof Field
                        && ConnectionContainer.PAGE_INFO_KEYWORD.equals(((Field) f).getName()));
    }
}
