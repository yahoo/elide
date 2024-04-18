/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.graphql;


import static org.junit.jupiter.api.Assertions.assertEquals;

import com.paiondata.elide.ElideError;
import com.paiondata.elide.graphql.models.SourceLocationBuilder;
import com.paiondata.elide.graphql.serialization.GraphQLModule;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import graphql.GraphQLError;
import graphql.execution.ResultPath;

import java.util.List;
import java.util.Map;

/**
 * Test for DefaultGraphQLErrorMapper.
 */
class DefaultGraphQLErrorMapperTest {

    private ObjectMapper objectMapper = new ObjectMapper();
    private GraphQLErrorMapper mapper = new DefaultGraphQLErrorMapper();

    @BeforeEach
    public void setup() {
        objectMapper.registerModule(new GraphQLModule());
    }

    @Test
    void toGraphQLError() throws JsonProcessingException {
        GraphQLError graphqlError = mapper
                .toGraphQLError(ElideError.builder()
                        .message("<script>message</script>")
                        .attribute("id", "id")
                        .attribute("status", "status")
                        .attribute("code", "code")
                        .attribute("title", "title")
                        .build());
        String actual = objectMapper.writeValueAsString(graphqlError);
        String expected = """
                {"message":"&lt;script&gt;message&lt;/script&gt;","extensions":{"id":"id","status":"status","code":"code","title":"title","classification":"DataFetchingException"}}""";
        assertEquals(expected, actual);
    }

    @Test
    void toGraphQLErrorExtensions() throws JsonProcessingException {
        GraphQLError graphqlError = mapper
                .toGraphQLError(ElideError.builder()
                        .message("message")
                        .attribute("property", "property")
                        .build());
        String actual = objectMapper.writeValueAsString(graphqlError);
        String expected = """
                {"message":"message","extensions":{"property":"property","classification":"DataFetchingException"}}""";
        assertEquals(expected, actual);
    }

    @Test
    void toGraphQLErrorLinks() throws JsonProcessingException {
        GraphQLError graphqlError = mapper
                .toGraphQLError(ElideError.builder()
                        .message("message")
                        .attribute("links", Map.of("about", "about"))
                        .build());
        String actual = objectMapper.writeValueAsString(graphqlError);
        String expected = """
                {"message":"message","extensions":{"links":{"about":"about"},"classification":"DataFetchingException"}}""";
        assertEquals(expected, actual);
    }

    @Test
    void toGraphQLErrorSource() throws JsonProcessingException {
        GraphQLError graphqlError = mapper
                .toGraphQLError(ElideError.builder()
                        .message("message")
                        .attribute("source", Map.of("pointer", "pointer"))
                        .build());
        String actual = objectMapper.writeValueAsString(graphqlError);
        String expected = """
                {"message":"message","extensions":{"source":{"pointer":"pointer"},"classification":"DataFetchingException"}}""";
        assertEquals(expected, actual);
    }

    @Test
    void toGraphQLErrorClassification() throws JsonProcessingException {
        GraphQLError graphqlError = mapper
                .toGraphQLError(ElideError.builder()
                        .message("message")
                        .attribute("classification", "test")
                        .build());
        String actual = objectMapper.writeValueAsString(graphqlError);
        String expected = """
                {"message":"message","extensions":{"classification":"test"}}""";
        assertEquals(expected, actual);
    }

    @Test
    void toGraphQLErrorPath() throws JsonProcessingException {
        GraphQLError graphqlError = mapper
                .toGraphQLError(ElideError.builder()
                        .message("message")
                        .attribute("path", List.of("hero", "heroFriends", 1, "name"))
                        .build());
        String actual = objectMapper.writeValueAsString(graphqlError);
        String expected = """
                {"message":"message","path":["hero","heroFriends",1,"name"],"extensions":{"classification":"DataFetchingException"}}""";
        assertEquals(expected, actual);
    }

    @Test
    void toGraphQLErrorResultPath() throws JsonProcessingException {
        GraphQLError graphqlError = mapper
                .toGraphQLError(ElideError.builder()
                        .message("message")
                        .attribute("path", ResultPath.fromList(List.of("hero", "heroFriends", 1, "name")))
                        .build());
        String actual = objectMapper.writeValueAsString(graphqlError);
        String expected = """
                {"message":"message","path":["hero","heroFriends",1,"name"],"extensions":{"classification":"DataFetchingException"}}""";
        assertEquals(expected, actual);
    }

    @Test
    void toGraphQLErrorLocations() throws JsonProcessingException {
        GraphQLError graphqlError = mapper
                .toGraphQLError(ElideError.builder()
                        .message("message")
                        .attribute("locations", List.of(new SourceLocationBuilder().line(6).column(7).build()))
                        .build());
        String actual = objectMapper.writeValueAsString(graphqlError);
        String expected = """
                {"message":"message","locations":[{"line":6,"column":7}],"extensions":{"classification":"DataFetchingException"}}""";
        assertEquals(expected, actual);
    }
}
