/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql.serialization;

import graphql.ExecutionResult;
import graphql.GraphQLError;

import tools.jackson.core.Version;
import tools.jackson.databind.module.SimpleModule;

/**
 * GraphQL Module.
 */
public class GraphQLModule extends SimpleModule {
    private static final long serialVersionUID = 1L;

    public GraphQLModule() {
        super("GraphQLModule", Version.unknownVersion());
        addSerializer(ExecutionResult.class, new ExecutionResultSerializer());
        addSerializer(GraphQLError.class, new GraphQLErrorSerializer());
        addDeserializer(GraphQLError.class, new GraphQLErrorDeserializer());
        addDeserializer(ExecutionResult.class, new ExecutionResultDeserializer());
    }
}
