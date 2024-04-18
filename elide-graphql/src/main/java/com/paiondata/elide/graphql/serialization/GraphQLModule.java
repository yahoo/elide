/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.graphql.serialization;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.module.SimpleModule;

import graphql.ExecutionResult;
import graphql.GraphQLError;

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
