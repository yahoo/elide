/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql;

import com.yahoo.elide.core.PersistentResource;
import graphql.language.Field;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLType;

import java.util.ArrayList;
import java.util.List;

/**
 * Interacts with {@link PersistentResource} to fetch data for GraphQL.
 */
public class PersistentResourceFetcher implements DataFetcher {

    @Override
    public Object get(DataFetchingEnvironment requestScope) {
        GraphQLContext context = (GraphQLContext) requestScope.getContext();
        GraphQLType parent = requestScope.getParentType();
        GraphQLType output = requestScope.getFieldType();
        List<Field> fields = requestScope.getFields();

        if (output instanceof GraphQLList) {
            GraphQLObjectType type = (GraphQLObjectType) ((GraphQLList) output).getWrappedType();
            return loadCollectionOf(type.getName(), context);
        } else {
            return load(output.getName(), context);
        }
    }

    protected List loadCollectionOf(String type, GraphQLContext context) {
        ArrayList list = new ArrayList();
        try {
            list.add(context.dictionary.getEntityClass(type).newInstance());
        } catch (InstantiationException | IllegalAccessException ignored) {
        }
        return list;
    }

    protected Object load(String type, GraphQLContext context) {
        try {
            return context.dictionary.getEntityClass(type).newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            return null;
        }
    }
}
