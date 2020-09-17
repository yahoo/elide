/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.IOException;
import java.util.Map;

/**
 * Class to store different parts of GraphQL Query that can be passed along to QueryRunner.
 *
 */
@Data
@AllArgsConstructor
public class GraphQLQuery {

    private String graphQLDocument;
    private JsonNode node;
    private String query;
    private String operationName;
    private Map<String, Object> variables;
    private boolean isMutation;

    public GraphQLQuery(String graphQLDocument, JsonNode node) {
        this.graphQLDocument = graphQLDocument;
        this.node = node;
        this.query = QueryRunner.extractQuery(node);
        this.variables = QueryRunner.extractVariables(node);
        this.operationName = QueryRunner.extractOperation(node);
        this.isMutation = QueryRunner.isMutation(query);
    }

    public GraphQLQuery(String graphQLDocument) throws IOException {
        this(graphQLDocument, QueryRunner.getTopLevelNode(graphQLDocument));
    }
}
