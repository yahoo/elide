/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.graphql.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Utility functions to parse the envelope that wraps every GraphQL query.
 */
public interface QueryParser {

    static final String QUERY = "query";
    static final String OPERATION_NAME = "operationName";
    static final String VARIABLES = "variables";

    /**
     * Parse a document which could consist of 1 or more GraphQL queries.
     * @param message The document to parse.
     * @param mapper An object mapper to do JSON parsing.
     * @return A list of 1 or more parsed GraphQL queries.
     * @throws IOException If there is a JSON processing error.
     */
    default List<GraphQLQuery> parseDocument(String message, ObjectMapper mapper) throws IOException {
        List<GraphQLQuery> results = new ArrayList<>();
        JsonNode topLevel = mapper.readTree(message);

        if (topLevel.isArray()) {
            Iterator<JsonNode> nodeIterator = topLevel.iterator();

            while (nodeIterator.hasNext()) {
                JsonNode document = nodeIterator.next();

                results.add(parseQuery(document, mapper));
            }
        } else {
            results.add(parseQuery(topLevel, mapper));
        }

        return results;
    }

    /**
     * Parse a GraphQL query.
     * @param topLevel The query JsonNode to further parse.
     * @param mapper An object mapper to do JSON parsing.
     * @return A parsed GraphQL queries.
     * @throws IOException If there is a JSON processing error.
     */
    default GraphQLQuery parseQuery(JsonNode topLevel, ObjectMapper mapper) throws IOException {
        String query = topLevel.has(QUERY) ? topLevel.get(QUERY).asText() : null;
        String operationName = "";

        Map<String, Object> variables = new HashMap<>();
        if (topLevel.has(VARIABLES) && !topLevel.get(VARIABLES).isNull()) {
            variables = mapper.convertValue(topLevel.get(VARIABLES), Map.class);
        }

        if (topLevel.has(OPERATION_NAME) && !topLevel.get(OPERATION_NAME).isNull()) {
            operationName = topLevel.get(OPERATION_NAME).asText();
        }

        return new GraphQLQuery(query, operationName, variables);
    }
}
