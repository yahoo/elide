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
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLType;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Interacts with {@link PersistentResource} to fetch data for GraphQL.
 */
@Slf4j
public class PersistentResourceFetcher implements DataFetcher {

    @Override
    public Object get(DataFetchingEnvironment requestScope) {
        GraphQLContext context = (GraphQLContext) requestScope.getContext();
        Object source = requestScope.getSource();
        GraphQLType parent = requestScope.getParentType();
        GraphQLType output = requestScope.getFieldType();
        List<Field> fields = requestScope.getFields();
        Map<String, Object> args = requestScope.getArguments();

        log.info("fetching {} fields for {} with parent {}<{}>",
                fields.stream().map(field -> {
                    List<Field> children = field.getSelectionSet() != null
                            ? (List) field.getSelectionSet().getChildren()
                            : new ArrayList<>();
                    return field.getName() + (
                            children.size() > 0
                                    ? "(" + children.stream().map(Field::getName).collect(Collectors.toList()) + ")"
                                    : ""
                    );
                })
                        .collect(Collectors.toList()), source, parent.getClass().getSimpleName(), parent.getName());

        if (output instanceof GraphQLList) {
            GraphQLObjectType type = (GraphQLObjectType) ((GraphQLList) output).getWrappedType();
            return loadCollectionOf(type.getName(), context);
        } else if (output instanceof GraphQLObjectType) {
            return load(output.getName(), context);
        } else if (output instanceof GraphQLScalarType) {
            return fetchProperty(context);
        } else {
            throw new IllegalStateException("WTF mate?");
        }
    }

    private Object fetchProperty(GraphQLContext context) {
        return null;
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
