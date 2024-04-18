/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.graphql.models;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import graphql.language.SourceLocation;

import java.util.Arrays;
import java.util.Map;

/**
 * Test for ExtendedGraphqlErrorBuilder.
 */
class GraphQLErrorTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void extensionsConsumer() throws JsonProcessingException {
        graphql.GraphQLError error = GraphQLError.builder()
                .message("Name for character with ID 1002 could not be fetched.").extensions(extensions -> {
                    extensions.put("code", "CAN_NOT_FETCH_BY_ID");
                    extensions.put("timestamp", "Fri Feb 9 14:33:09 UTC 2018");
                }).build();

        String actual = objectMapper.writeValueAsString(error);
        String expected = """
                {"message":"Name for character with ID 1002 could not be fetched.","extensions":{"code":"CAN_NOT_FETCH_BY_ID","timestamp":"Fri Feb 9 14:33:09 UTC 2018","classification":"DataFetchingException"}}""";
        assertEquals(expected, actual);
    }

    @Test
    void extensions() throws JsonProcessingException {
        graphql.GraphQLError error = GraphQLError.builder()
                .message("Name for character with ID 1002 could not be fetched.")
                .extensions(Map.of("code", "CAN_NOT_FETCH_BY_ID")).build();

        String actual = objectMapper.writeValueAsString(error);
        String expected = """
                {"message":"Name for character with ID 1002 could not be fetched.","extensions":{"code":"CAN_NOT_FETCH_BY_ID","classification":"DataFetchingException"}}""";
        assertEquals(expected, actual);
    }

    @Test
    void locationsConsumer() throws JsonProcessingException {
        SourceLocation location = new SourceLocationBuilder().line(6).column(7).build();
        graphql.GraphQLError error = GraphQLError.builder()
                .message("Name for character with ID 1002 could not be fetched.")
                .locations(locations -> locations.add(location)).build();
        String actual = objectMapper.writeValueAsString(error);
        String expected = """
                {"message":"Name for character with ID 1002 could not be fetched.","locations":[{"line":6,"column":7}],"extensions":{"classification":"DataFetchingException"}}""";
        assertEquals(expected, actual);
    }

    @Test
    void locationsConsumerMultiple() throws JsonProcessingException {
        graphql.GraphQLError error = GraphQLError.builder()
                .message("Name for character with ID 1002 could not be fetched.")
                .location(location -> location.line(1).column(2)).location(location -> location.line(3).column(4))
                .build();
        String actual = objectMapper.writeValueAsString(error);
        String expected = """
                {"message":"Name for character with ID 1002 could not be fetched.","locations":[{"line":1,"column":2},{"line":3,"column":4}],"extensions":{"classification":"DataFetchingException"}}""";
        assertEquals(expected, actual);
    }

    @Test
    void locations() throws JsonProcessingException {
        SourceLocation location = new SourceLocationBuilder().line(6).column(7).build();
        graphql.GraphQLError error = GraphQLError.builder()
                .message("Name for character with ID 1002 could not be fetched.").locations(Arrays.asList(location))
                .build();
        String actual = objectMapper.writeValueAsString(error);
        String expected = """
                {"message":"Name for character with ID 1002 could not be fetched.","locations":[{"line":6,"column":7}],"extensions":{"classification":"DataFetchingException"}}""";
        assertEquals(expected, actual);
    }
}
