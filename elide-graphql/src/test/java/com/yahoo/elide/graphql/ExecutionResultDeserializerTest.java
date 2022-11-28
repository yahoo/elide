/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import graphql.ExecutionResult;
import graphql.GraphQLError;
import graphql.language.SourceLocation;

import java.util.Map;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ExecutionResultDeserializerTest {
    private ObjectMapper mapper;

    @BeforeAll
    public void init() {
        mapper = new ObjectMapper();
        mapper.registerModule(new SimpleModule("ExecutionResult")
                .addDeserializer(GraphQLError.class, new GraphQLErrorDeserializer())
                .addDeserializer(ExecutionResult.class, new ExecutionResultDeserializer())
        );
    }

    @Test
    public void testDeserialization() throws Exception {
        String resultText = "{\"data\":{\"book\":{\"id\":\"1\",\"title\":null}},\"errors\":[{\"message\":\"Exception while fetching data (/book/title) : Bad Request\",\"locations\":[{\"line\":1,\"column\":38}],\"path\":[\"book\",\"title\"],\"extensions\":{\"classification\":\"DataFetchingException\"}}]}}";

        ExecutionResult result = mapper.readValue(resultText, ExecutionResult.class);

        Map<String, Object> data = result.getData();
        Map<String, Object> book = (Map<String, Object>) data.get("book");

        assertEquals("1", book.get("id"));
        assertEquals(null, book.get("title"));

        GraphQLError error = result.getErrors().get(0);

        assertEquals(1, error.getLocations().size());
        assertEquals(new SourceLocation(1, 38), error.getLocations().get(0));
        assertEquals(2, error.getPath().size());
        assertEquals("book", error.getPath().get(0));
        assertEquals("title", error.getPath().get(1));
        assertEquals("Exception while fetching data (/book/title) : Bad Request", error.getMessage());
    }

    @Test
    public void testDeserializationWithMissingData() throws Exception {
        String resultText = "{\"errors\":[{\"message\":\"Exception while fetching data (/book/title) : Bad Request\",\"locations\":[{\"line\":1,\"column\":38}],\"path\":[\"book\",\"title\"],\"extensions\":{\"classification\":\"DataFetchingException\"}}]}}";

        ExecutionResult result = mapper.readValue(resultText, ExecutionResult.class);

        assertNull(result.getData());

        GraphQLError error = result.getErrors().get(0);

        assertEquals(1, error.getLocations().size());
        assertEquals(new SourceLocation(1, 38), error.getLocations().get(0));
        assertEquals(2, error.getPath().size());
        assertEquals("book", error.getPath().get(0));
        assertEquals("title", error.getPath().get(1));
        assertEquals("Exception while fetching data (/book/title) : Bad Request", error.getMessage());
    }

    @Test
    public void testDeserializationWithMissingErrors() throws Exception {
        String resultText = "{\"data\":{\"book\":{\"id\":\"1\",\"title\":null}}}";

        ExecutionResult result = mapper.readValue(resultText, ExecutionResult.class);

        Map<String, Object> data = result.getData();
        Map<String, Object> book = (Map<String, Object>) data.get("book");

        assertEquals("1", book.get("id"));
        assertEquals(null, book.get("title"));
        assertTrue(result.getErrors().isEmpty());
    }
}
