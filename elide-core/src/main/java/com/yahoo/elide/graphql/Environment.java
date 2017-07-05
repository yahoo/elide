/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql;

import com.google.common.collect.ImmutableList;
import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.RequestScope;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLType;
import graphql.language.Field;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Encapsulates GraphQL's DataFetchingEnvironment
 */
public class Environment {
    public final RequestScope requestScope;
    public final List<String> ids;
    public final Optional<String> sort;
    public final List<Map<String, Object>> data;
    public final Optional<String> filters;
    public final Optional<String> offset;
    public final Optional<String> first;

    public final Object source;
    public final PersistentResource parentResource;
    public final GraphQLType parentType;
    public final GraphQLType outputType;
    public final Field field;

    public Environment(DataFetchingEnvironment environment) {
        Map<String, Object> args = environment.getArguments();

        requestScope = (RequestScope) environment.getContext();

        filters = Optional.ofNullable((String) args.get(ModelBuilder.ARGUMENT_FILTER));
        offset = Optional.ofNullable((String) args.get(ModelBuilder.ARGUMENT_OFFSET));
        first = Optional.ofNullable((String) args.get(ModelBuilder.ARGUMENT_FIRST));
        sort = Optional.ofNullable((String) args.get(ModelBuilder.ARGUMENT_SORT));

        source = environment.getSource();
        parentType = environment.getParentType();

        outputType = environment.getFieldType();
        parentResource = isRoot() ? null : (PersistentResource) source;
        field = environment.getFields().get(0);

        List<String> ids = (List<String>) args.get(ModelBuilder.ARGUMENT_IDS);
        if(ids != null) {
            this.ids = ImmutableList.copyOf((List) args.get(ModelBuilder.ARGUMENT_IDS));
        } else {
            this.ids = ImmutableList.of();
        }

        List<Map<String, Object>> data = (List<Map<String, Object>>) args.get(ModelBuilder.ARGUMENT_DATA);
        if(data != null) {
            this.data = ImmutableList.copyOf(data);
        } else {
            this.data = ImmutableList.of();
        }
    }

    public boolean isRoot() {
        return source instanceof RequestScope;
    }
}
