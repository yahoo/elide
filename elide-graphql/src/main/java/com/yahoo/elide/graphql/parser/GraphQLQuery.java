/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql.parser;

import lombok.Value;

import java.util.Map;

/**
 * Represents a complete GraphQL query including its envelope.
 */
@Value
public class GraphQLQuery {
    private String query;
    private String operationName;
    private Map<String, Object> variables;
}
