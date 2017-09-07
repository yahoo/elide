/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.testng.annotations.Test;

/**
 * Test the Delete operation.
 */
public class FetcherDeleteTest extends PersistentResourceFetcherTest {
    @Test
    public void testRootBadInput() {
        String graphQLRequest = "mutation { "
                    + "author(op:DELETE) { "
                    + "id "
                    + "} "
                    + "}";
        assertQueryFails(graphQLRequest);
    }

    @Test
    public void testRootIdNoData() throws JsonProcessingException {
        String graphQLRequest = "mutation { "
                    + "author(op:DELETE, ids: [\"1\"]) { "
                    + "id "
                    + "name "
                    + "} "
                    + "}";
        String expectedResponse = "{"
                    + "\"author\":[{"
                    + "\"id\":\"1\","
                    + "\"name\":\"Mark Twain\""
                    + "}]"
                    + "}";

        assertQueryEquals(graphQLRequest, expectedResponse);

        String graphQLFetchRequest = "mutation { "
                + "author(ids: [\"1\"]) { "
                + "id "
                + "name "
                + "} "
                + "}";
        String expectedFetchResponse = "{"
                + "\"author\":[]"
                + "}";
        assertQueryEquals(graphQLFetchRequest, expectedFetchResponse);
    }

    @Test
    public void testRootIdWithBadData() {
        String graphQLRequest = "mutation { "
                    + "author(op:DELETE, ids: [\"1\"], data: {id: \"2\"}) { "
                    + "id "
                    + "} "
                    + "}";
        assertQueryFails(graphQLRequest);
    }

    @Test
    public void testRootCollection() throws JsonProcessingException {
        String graphQLRequest = "mutation { "
                + "book(op:DELETE, ids: [\"1\", \"2\"]) { "
                + "id "
                + "title "
                + "} "
                + "}";
        String expectedResponse = "{"
                + "\"book\":[{"
                + "\"id\":\"1\","
                + "\"title\":\"Libro Uno\""
                + "},{"
                + "\"id\":\"2\","
                + "\"title\":\"Libro Dos\""
                + "}]"
                + "}";

        assertQueryEquals(graphQLRequest, expectedResponse);

        String graphQLFetchRequest = "mutation { "
                + "book(ids: [\"1\", \"2\"]) { "
                + "id "
                + "title "
                + "} "
                + "}";
        String expectedFetchResponse = "{"
                + "\"book\":[]"
                + "}";

        assertQueryEquals(graphQLFetchRequest, expectedFetchResponse);
    }

    @Test
    public void testNestedBadInput() {
        String graphQLRequest = "mutation { "
                + "author(id: \"1\") { "
                + "books(op:DELETE) { "
                + "id "
                + "} "
                + "} "
                + "}";
        assertQueryFails(graphQLRequest);
    }


    @Test
    public void testNestedSingleId() throws JsonProcessingException {
        String graphQLRequest = "mutation { "
                + "author(ids: [\"1\"]) { "
                + "books(op:DELETE, ids: [\"1\"]) { "
                + "id "
                + "title "
                + "} "
                + "} "
                + "}";
        String expectedResponse = "{"
                + "\"author\":[{"
                + "\"books\":[{"
                + "\"id\":\"1\","
                + "\"title\":\"Libro Uno\""
                + "}]"
                + "}]"
                + "}";

        assertQueryEquals(graphQLRequest, expectedResponse);

        String graphQLFetchRequest = "mutation { "
                + "author(ids: [\"1\"]) { "
                + "books(ids: [\"1\"]) { "
                + "title "
                + "} "
                + "} "
                + "}";

        assertQueryFails(graphQLFetchRequest);
    }

    @Test
    public void testNestedCollection() throws JsonProcessingException {
        String graphQLRequest = "mutation { "
                + "author(ids: [\"1\"]) { "
                + "books(op:DELETE, ids: [\"1\", \"2\"]) { "
                + "id "
                + "title "
                + "} "
                + "} "
                + "}";
        String expectedResponse = "{\"author\":[{"
                + "\"books\":[{"
                + "\"id\":\"1\","
                + "\"title\":\"Libro Uno\""
                + "},{"
                + "\"id\":\"2\","
                + "\"title\":\"Libro Dos\""
                + "}]"
                + "}]}";

        assertQueryEquals(graphQLRequest, expectedResponse);

        String graphQLFetchRequest = "mutation { "
                + "author(ids: [\"1\"]) { "
                + "books { "
                + "id "
                + "title "
                + "} "
                + "} "
                + "}";

        String expectedFetchResponse = "{"
                + "\"author\":[{"
                + "\"books\":[]"
                + "}]"
                + "}";

        assertQueryEquals(graphQLFetchRequest, expectedFetchResponse);
    }
}
