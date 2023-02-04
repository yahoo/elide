/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql;

import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.graphql.containers.GraphQLContainer;
import com.yahoo.elide.graphql.containers.PersistentResourceContainer;
import com.yahoo.elide.graphql.containers.RootContainer;

import graphql.language.Field;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLType;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Encapsulates GraphQL's DataFetchingEnvironment.
 */
public class Environment {
    public final GraphQLRequestScope requestScope;
    public final Optional<List<String>> ids;
    public final Optional<String> sort;
    public final Optional<List<Map<String, Object>>> data;
    public final Optional<String> filters;
    public final Optional<String> offset;
    public final Optional<String> first;
    public final Object rawSource;
    public final GraphQLContainer container;

    public final PersistentResource parentResource;
    public final GraphQLType parentType;
    public final GraphQLType outputType;
    public final Field field;
    public final NonEntityDictionary nonEntityDictionary;

    public Environment(DataFetchingEnvironment environment, NonEntityDictionary nonEntityDictionary) {
        this.nonEntityDictionary = nonEntityDictionary;

        Map<String, Object> args = environment.getArguments();

        requestScope = environment.getLocalContext();

        filters = Optional.ofNullable((String) args.get(ModelBuilder.ARGUMENT_FILTER));
        offset = Optional.ofNullable((String) args.get(ModelBuilder.ARGUMENT_AFTER));
        first = Optional.ofNullable((String) args.get(ModelBuilder.ARGUMENT_FIRST));
        sort = Optional.ofNullable((String) args.get(ModelBuilder.ARGUMENT_SORT));

        parentType = environment.getParentType();

        outputType = environment.getFieldType();

        rawSource = environment.getSource();
        container = isRoot() ? new RootContainer() : (GraphQLContainer) rawSource;

        if (isRoot()) {
            // Flush (but don't commit) between root queries
            requestScope.saveOrCreateObjects();
            requestScope.getTransaction().flush(requestScope);
        }

        if (rawSource instanceof PersistentResourceContainer) {
            parentResource = ((PersistentResourceContainer) rawSource).getPersistentResource();
        } else {
            parentResource = null;
        }

        field = environment.getMergedField().getFields().get(0);

        this.ids = Optional.ofNullable((List<String>) args.get(ModelBuilder.ARGUMENT_IDS));

        List<Map<String, Object>> data;
        if (args.get(ModelBuilder.ARGUMENT_DATA) instanceof Map) {
            data = Arrays.asList((Map<String, Object>) args.get(ModelBuilder.ARGUMENT_DATA));
        } else {
            data = (List<Map<String, Object>>) args.get(ModelBuilder.ARGUMENT_DATA);
        }
        this.data = Optional.ofNullable(data);
    }

    public boolean isRoot() {
        return !(rawSource instanceof GraphQLContainer);
    }
}
