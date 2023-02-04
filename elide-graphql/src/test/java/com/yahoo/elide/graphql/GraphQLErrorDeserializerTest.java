/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import graphql.GraphQLError;
import graphql.language.SourceLocation;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class GraphQLErrorDeserializerTest {
    private ObjectMapper mapper;

    @BeforeAll
    public void init() {
        mapper = new ObjectMapper();
        mapper.registerModule(new SimpleModule("GraphQLError")
                .addDeserializer(GraphQLError.class, new GraphQLErrorDeserializer()));

    }

    @Test
    public void testDeserialization() throws Exception {
        String errorText = "{\"message\":\"Exception while fetching data (/book/title) : Bad Request\",\"locations\":[{\"line\":1,\"column\":38}],\"path\":[\"book\",\"title\"],\"extensions\":{\"classification\":\"DataFetchingException\"}}";

        GraphQLError error = mapper.readValue(errorText, GraphQLError.class);

        assertEquals(1, error.getLocations().size());
        assertEquals(new SourceLocation(1, 38), error.getLocations().get(0));
        assertEquals(2, error.getPath().size());
        assertEquals("book", error.getPath().get(0));
        assertEquals("title", error.getPath().get(1));
        assertEquals("Exception while fetching data (/book/title) : Bad Request", error.getMessage());
    }

    @Test
    public void testDeserializationArray() throws Exception {
        String errorText = "[{\"message\":\"Exception while fetching data (/book/title) : Bad Request\",\"locations\":[{\"line\":1,\"column\":38}],\"path\":[\"book\",\"title\"],\"extensions\":{\"classification\":\"DataFetchingException\"}}]";

        GraphQLError[] error = mapper.readValue(errorText, GraphQLError[].class);

        assertEquals(1, error[0].getLocations().size());
        assertEquals(new SourceLocation(1, 38), error[0].getLocations().get(0));
        assertEquals(2, error[0].getPath().size());
        assertEquals("book", error[0].getPath().get(0));
        assertEquals("title", error[0].getPath().get(1));
        assertEquals("Exception while fetching data (/book/title) : Bad Request", error[0].getMessage());
    }

    @Test
    public void testDeserializationWithMissingPath() throws Exception {
        String errorText = "{\"message\":\"Exception while fetching data (/book/title) : Bad Request\",\"locations\":[{\"line\":1,\"column\":38}]}";

        GraphQLError error = mapper.readValue(errorText, GraphQLError.class);

        assertEquals(1, error.getLocations().size());
        assertEquals(new SourceLocation(1, 38), error.getLocations().get(0));
        assertNull(error.getPath());
        assertEquals("Exception while fetching data (/book/title) : Bad Request", error.getMessage());
    }

    @Test
    public void testDeserializationWithMissingMessage() throws Exception {
        String errorText = "{\"locations\":[{\"line\":1,\"column\":38}],\"path\":[\"book\",\"title\"],\"extensions\":{\"classification\":\"DataFetchingException\"}}";

        GraphQLError error = mapper.readValue(errorText, GraphQLError.class);

        assertEquals(1, error.getLocations().size());
        assertEquals(new SourceLocation(1, 38), error.getLocations().get(0));
        assertEquals(2, error.getPath().size());
        assertEquals("book", error.getPath().get(0));
        assertEquals("title", error.getPath().get(1));
        assertNull(error.getMessage());
    }

    @Test
    public void testDeserializationWithMissingLocation() throws Exception {
        String errorText = "{\"message\":\"Exception while fetching data (/book/title) : Bad Request\",\"path\":[\"book\",\"title\"],\"extensions\":{\"classification\":\"DataFetchingException\"}}";

        GraphQLError error = mapper.readValue(errorText, GraphQLError.class);

        assertNull(error.getLocations());
        assertEquals(2, error.getPath().size());
        assertEquals("book", error.getPath().get(0));
        assertEquals("title", error.getPath().get(1));
        assertEquals("Exception while fetching data (/book/title) : Bad Request", error.getMessage());
    }
}
