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

/**
 * Test for GraphqlErrors.
 */
class GraphQLErrorsTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void basicErrorResult() throws JsonProcessingException {
        GraphQLErrors errors = GraphQLErrors.builder()
                .error(error -> error.message("Name for character with ID 1002 could not be fetched.")
                        .location(location -> location.line(6).column(7)).path("hero", "heroFriends", 1, "name")
                        .extension("code", "CAN_NOT_FETCH_BY_ID").extension("timestamp", "Fri Feb 9 14:33:09 UTC 2018"))
                .build();
        String actual = objectMapper.writeValueAsString(errors);
        String expected = """
                {"errors":[{"message":"Name for character with ID 1002 could not be fetched.","locations":[{"line":6,"column":7}],"path":["hero","heroFriends",1,"name"],"extensions":{"code":"CAN_NOT_FETCH_BY_ID","timestamp":"Fri Feb 9 14:33:09 UTC 2018","classification":"DataFetchingException"}}]}""";
        assertEquals(expected, actual);
    }
}
