/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.testng.annotations.Test;

/**
 * Test the Add operation.
 */
public class FetcherUpsertTest extends PersistentResourceFetcherTest {
    @Test
    public void testCreateRootSingle() throws JsonProcessingException {
        String graphQLRequest =
                "mutation { " +
                        "book(op: UPSERT, data: {title: \"Book Numero Dos\"} ) { " +
                        "title " +
                        "} " +
                        "}";
        String expectedResponse =
                "{" +
                        "\"book\":[{" +
                        "\"title\":\"Book Numero Dos\"" +
                        "}]" +
                        "}";

        assertQueryEquals(graphQLRequest, expectedResponse);
    }

    @Test
    public void testCreateRootCollection() throws JsonProcessingException {
        String graphQLRequest =
                "mutation { " +
                        "book(op: UPSERT, data: [{title: \"Book Numero Dos\"},{title:\"Book Numero Tres\"}] ) { " +
                        "title " +
                        "} " +
                        "}";
        String expectedResponse =
                "{" +
                        "\"book\":[{" +
                        "\"title\":\"Book Numero Dos\"" +
                        "},{" +
                        "\"title\":\"Book Numero Tres\"" +
                        "}]" +
                        "}";

        assertQueryEquals(graphQLRequest, expectedResponse);
    }

    @Test
    public void testCreateNestedSingle() throws JsonProcessingException {
        String graphQLRequest =
                "mutation { " +
                        "author(ids: [\"1\"]) { " +
                        "id " +
                        "books(op: UPSERT, data: {title: \"Book Numero Dos\"}) { " +
                        "title " +
                        "} " +
                        "} " +
                        "} ";
        String expectedResponse =
                "{" +
                        "\"author\":[{" +
                        "\"id\":\"1\"," +
                        "\"books\":[{" +
                        "\"title\":\"Book Numero Dos\"" +
                        "}]" +
                        "}]" +
                        "}";

        assertQueryEquals(graphQLRequest, expectedResponse);
    }

    @Test
    public void testCreateNestedCollection() throws JsonProcessingException {
        String graphQLRequest =
                "mutation { " +
                        "author(ids: [\"1\"]) { " +
                        "id " +
                        "books(op: UPSERT, data: [{title: \"Book Numero Dos\"}, {title: \"Book Numero Tres\"}]) { " +
                        "title " +
                        "} " +
                        "} " +
                        "} ";
        String expectedResponse =
                "{" +
                        "\"author\":[{" +
                        "\"id\":\"1\"," +
                        "\"books\":[{" +
                        "\"title\":\"Book Numero Dos\"" +
                        "},{" +
                        "\"title\":\"Book Numero Tres\"" +
                        "}]" +
                        "}]" +
                        "}";

        assertQueryEquals(graphQLRequest, expectedResponse);
    }

    @Test
    public void testCreateRootSinglePagination() {
        String graphQLRequest =
                "mutation { " +
                        "book(op: UPSERT, data: {title: \"Book Numero Dos\"}, first: \"1\") { " +
                        "title " +
                        "} " +
                        "}";
        String expectedResponse =
                "{" +
                        "\"book\":[{" +
                        "\"title\":\"Book Numero Dos\"" +
                        "}]" +
                        "}";
        assertQueryEquals(graphQLRequest, expectedResponse);
    }

    @Test
    public void testCreateRootCollectionPagination() {
        String graphQLRequest =
                "mutation { " +
                        "book(op: UPSERT, data: [{title: \"Book Numero Dos\"},{title:\"Book Numero Tres\"}], first: \"1\") { " +
                        "title " +
                        "}" +
                        "}";
        String expectedResponse =
                "{" +
                        "\"book\":[{" +
                        "\"title\":\"Book Numero Dos\"" +
                        "}]" +
                        "}";
        assertQueryEquals(graphQLRequest, expectedResponse);
    }

    @Test
    public void testCreateRootSingleSort() {
        String graphQLRequest =
                "mutation { " +
                        "book(op: UPSERT, data: {title: \"Book Numero Dos\"}, sort: \"-title\" ) { " +
                        "title " +
                        "} " +
                        "}";
        String expectedResponse =
                "{" +
                        "\"book\":[{" +
                        "\"title\":\"Book Numero Dos\"" +
                        "}]" +
                        "}";
        assertQueryEquals(graphQLRequest, expectedResponse);
    }

    @Test
    public void testCreateRootCollectionFailSort() throws JsonProcessingException {
        String graphQLRequest =
                "mutation { " +
                        "book(op: UPSERT, data: [{title: \"Book Numero Dos\"},{title:\"Book Numero Tres\"}], sort: \"+title\" ) { " +
                        "title " +
                        "} " +
                        "}";
        String expectedResponse =
                "{" +
                        "\"book\":[{" +
                        "\"title\":\"Book Numero Tres\"" +
                        "},{" +
                        "\"title\":\"Book Numero Dos\"" +
                        "}]" +
                        "}";
        assertQueryEquals(graphQLRequest, expectedResponse);
    }

    @Test
    public void testCreateRootSingleFailFilter() { //fails
        String graphQLRequest = "mutation { book(op: UPSERT, data: {title: \"Book Numero Dos\"}, filter: \"title=\\\"boom\\\"\" ) { title } }";
        assertQueryFails(graphQLRequest);
    }

    @Test
    public void testCreateRootCollectionFailFilter() throws JsonProcessingException { //fails
        String graphQLRequest = "mutation { book(op: UPSERT, data: [{title: \"Book Numero Dos\"},{title:\"Book Numero Tres\"}], filter: \"title=\\\"boom\\\"\" ) { title } }";
        assertQueryFails(graphQLRequest);
    }
}
