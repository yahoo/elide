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
public class FetcherAddTest extends AbstractPersistentResourceFetcherTest {
    @Test
    public void testCreateRootSingle() throws JsonProcessingException {
        String graphQLRequest = "mutation { book(op: ADD, data: {title: \"Book Numero Dos\"} ) { title } }";
        String expectedResponse = "{\"book\":[{\"title\":\"Book Numero Dos\"}]}";

        assertQueryEquals(graphQLRequest, expectedResponse);
    }

    @Test
    public void testCreateRootCollection() throws JsonProcessingException {
        String graphQLRequest = "mutation { book(op: ADD, data: [{title: \"Book Numero Dos\"},{title:\"Book Numero Tres\"}] ) { title } }";
        String expectedResponse = "{\"book\":[{\"title\":\"Book Numero Dos\"},{\"title\":\"Book Numero Tres\"}]}";

        assertQueryEquals(graphQLRequest, expectedResponse);
    }

    @Test
    public void testCreateNestedSingle() throws JsonProcessingException {
        String graphQLRequest = "mutation { author(id: \"1\") { id books(op: ADD, data: {title: \"Book Numero Dos\"}) { title } } } ";
        String expectedResponse = "{\"author\":[{\"id\":\"1\",\"books\":[{\"title\":\"Book Numero Dos\"}]}]}";

        assertQueryEquals(graphQLRequest, expectedResponse);
    }

    @Test
    public void testCreateNestedCollection() throws JsonProcessingException {
        String graphQLRequest = "mutation { author(id: \"1\") { id books(op: ADD, data: {title: \"Book Numero Dos\"}) { title } } } ";
        String expectedResponse = "{\"author\":[{\"id\":\"1\",\"books\":[{\"title\":\"Book Numero Dos\"}]}]}";

        assertQueryEquals(graphQLRequest, expectedResponse);
    }

    @Test
    public void testCreateRootSingleFailPagination() {
        String graphQLRequest = "mutation { book(op: ADD, data: {title: \"Book Numero Dos\"}, first: 1) { title } }";
        assertQueryFails(graphQLRequest);

        graphQLRequest = "mutation { book(op: ADD, data: {title: \"Book Numero Dos\"}, offset: 10) { title } }";
        assertQueryFails(graphQLRequest);
    }

    @Test
    public void testCreateRootCollectionFailPagination() {
        String graphQLRequest = "mutation { book(op: ADD, data: [{title: \"Book Numero Dos\"},{title:\"Book Numero Tres\"}], first: 1) { title } }";
        assertQueryFails(graphQLRequest);

        graphQLRequest = "mutation { book(op: ADD, data: [{title: \"Book Numero Dos\"},{title:\"Book Numero Tres\"}], offset: 10) { title } }";
        assertQueryFails(graphQLRequest);
    }

    @Test
    public void testCreateRootSingleFailFilter() {
        String graphQLRequest = "mutation { book(op: ADD, data: {title: \"Book Numero Dos\"}, filter: \"title=\\\"boom\\\"\" ) { title } }";
        assertQueryFails(graphQLRequest);
    }

    @Test
    public void testCreateRootCollectionFailFilter() throws JsonProcessingException {
        String graphQLRequest = "mutation { book(op: ADD, data: [{title: \"Book Numero Dos\"},{title:\"Book Numero Tres\"}], filter: \"title=\\\"boom\\\"\" ) { title } }";
        assertQueryFails(graphQLRequest);
    }

    @Test
    public void testCreateRootSingleFailSort() {
        String graphQLRequest = "mutation { book(op: ADD, data: {title: \"Book Numero Dos\"}, sort: \"title\" ) { title } }";
        assertQueryFails(graphQLRequest);
    }

    @Test
    public void testCreateRootCollectionFailSort() throws JsonProcessingException {
        String graphQLRequest = "mutation { book(op: ADD, data: [{title: \"Book Numero Dos\"},{title:\"Book Numero Tres\"}], sort: \"title\" ) { title } }";
        assertQueryFails(graphQLRequest);
    }
}
