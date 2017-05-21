/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql;

import com.google.common.collect.ImmutableList;
import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.RequestScope;
import graphql.language.Field;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLType;

import javax.ws.rs.WebApplicationException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.yahoo.elide.graphql.ModelBuilder.ARGUMENT_ID;
import static com.yahoo.elide.graphql.ModelBuilder.ID_ARGUMENT;

public class Environment {
    public static final List<Map<String, Object>> EMPTY_DATA = ImmutableList.of();

    public final RequestScope requestScope;
    public final Optional<String> id;
    public final Object source;
    public final PersistentResource parentResource;
    public final GraphQLType parentType;
    public final GraphQLType outputType;
    public final Field field;
    public final List<Map<String, Object>> data;
    public final Optional<String> filters;

    public Environment(DataFetchingEnvironment environment) {
        if (environment.getFields().size() != 1) {
            throw new WebApplicationException("Resource fetcher contract has changed");
        }
        Map<String, Object> args = environment.getArguments();

        requestScope = (RequestScope) environment.getContext();
        source = environment.getSource();
        parentResource = isRoot() ? null : (PersistentResource) source;
        parentType = environment.getParentType();
        outputType = environment.getFieldType();
        field = environment.getFields().get(0);

        id = Optional.ofNullable((String) args.get(ARGUMENT_ID));
        filters = Optional.ofNullable((String) args.get(ModelBuilder.ARGUMENT_FILTER));

        List<Map<String, Object>> data = (List<Map<String, Object>>) args.get(ModelBuilder.ARGUMENT_DATA);
        if (data == null) {
            this.data = EMPTY_DATA;
        } else {
            this.data = ImmutableList.copyOf(data);
        }
    }

    public boolean isRoot() {
        return source instanceof RequestScope;
    }
}
