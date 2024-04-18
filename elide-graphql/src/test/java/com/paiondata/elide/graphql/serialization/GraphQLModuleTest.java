/*
 * Copyright 2023, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.graphql.serialization;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import graphql.ExecutionResult;
import graphql.GraphQLError;

import java.util.List;
import java.util.Map;

class GraphQLModuleTest {

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setup() {
        objectMapper.registerModule(new GraphQLModule());
    }

    /**
     * Checks that the execution result error messages should be HTML encoded.
     * This is as the GraphQLModule has registered the GraphQLErrorSerializer.
     *
     * @throws JsonProcessingException the exception
     */
    @Test
    void executionResultErrorMessageShouldBeHtmlEncoded() throws JsonProcessingException {
        ExecutionResult executionResult = ExecutionResult.newExecutionResult()
                .errors(List.of(GraphQLError.newError().message("<script>message</script>").build()))
                .extensions(Map.of("timestamp", "Fri Feb 9 14:33:09 UTC 2018"))
                .build();
        String actual = objectMapper.writeValueAsString(executionResult);
        String expected = """
                {"errors":[{"message":"&lt;script&gt;message&lt;/script&gt;","locations":[],"extensions":{"classification":"DataFetchingException"}}],"extensions":{"timestamp":"Fri Feb 9 14:33:09 UTC 2018"}}""";
        assertEquals(expected, actual);
    }
}
