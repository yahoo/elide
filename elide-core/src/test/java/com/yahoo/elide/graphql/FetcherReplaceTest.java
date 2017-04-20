/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.testng.annotations.Test;

/**
 * Test the Replace operation.
 */
public class FetcherReplaceTest extends AbstractPersistentResourceFetcherTest {
    @Test
    public void testRootSingleNoId() throws JsonProcessingException {
        String graphQLRequest = "mutation { author(op:REPLACE, id: \"1\", data: { name: \"abc\" }) { id, name } }";
        String expectedResponse = "{\"author\":[{\"id\":\"1\",\"name\":\"abc\"}]}";

        assertQueryEquals(graphQLRequest, expectedResponse);

        // Ensure we don't accidentally null values
        graphQLRequest = "{ author(id: \"1\") { id, type, books { id } } }";
        expectedResponse = "{\"author\":[{\"id\":\"1\",\"type\":\"EXCLUSIVE\",\"books\":[{\"id\":\"1\"},{\"id\":\"2\"}]}]}";

        assertQueryEquals(graphQLRequest, expectedResponse);
    }

    @Test
    public void testRootSingleWithId() throws JsonProcessingException {
        String graphQLRequest = "mutation { book(op:REPLACE, id: \"1\", data: {id: \"42\", title: \"abc\"}) { id, title } }";
        String expectedResponse = "{\"book\":[{\"id\":\"42\",\"title\":\"abc\"}]}";

        assertQueryEquals(graphQLRequest, expectedResponse);
    }

    @Test
    public void testRootSingleWithList() throws JsonProcessingException {
        String graphQLRequest = "mutation { book(op:REPLACE, id: \"1\", data: [{id: \"42\", title: \"abc\"}, {id: \"42\", title: \"abc\"}]) { id, title } }";
        String expectedResponse = "{\"book\":[{\"id\":\"42\",\"title\":\"abc\"}]}";

        assertQueryEquals(graphQLRequest, expectedResponse);
    }

    @Test
    public void testRootCollectionMixedIds() throws JsonProcessingException {
        String graphQLRequest = "mutation { book(op:REPLACE, data: [{id: \"1\", title: \"abc\"}, {id: \"42\", title: \"abc\"}, {title: \"abc\"}]) { id, title } }"; // create 2, update 1
        String expectedResponse = "{\"book\":[{\"id\":\"1\",\"title\":\"abc\"},{\"id\":\"42\",\"title\":\"abc\"},{\"id\":\"43\",\"title\":\"abc\"}]}";

        assertQueryEquals(graphQLRequest, expectedResponse);
    }

    @Test
    public void testRootCollectionNoIds() throws JsonProcessingException {
        String graphQLRequest = "mutation { book(op:REPLACE, data: [{title: \"abc\"}, {title: \"abc\"}, {title: \"abc\"}]) { id, title } }";
        String expectedResponse = "{\"book\":[{\"id\":\"3\",\"title\":\"abc\"},{\"id\":\"4\",\"title\":\"abc\"}]}";

        assertQueryEquals(graphQLRequest, expectedResponse);
    }

    @Test
    public void testNestedSingleUpdate() throws JsonProcessingException {
        String graphQLRequest = "mutation { author(id: \"1\") { id books(op:REPLACE, data: {id: \"1\", title: \"abc\"}) { id, title } } }";
        String expectedResponse = "{\"author\":[{\"id\":\"1\",\"books\":[{\"id\":\"1\",\"title\":\"abc\"}]}]}";

        assertQueryEquals(graphQLRequest, expectedResponse);
    }

    @Test
    public void testNestedSingleReplace() throws JsonProcessingException {
        String graphQLRequest = "mutation { author(id: \"1\") { id name books(op:REPLACE, id:\"1\", data: {id: \"1\", title: \"abc\"}) { id title } } }";
        String expectedResponse = "{\"author\":[{\"id\":\"1\",\"name\":\"The People's Author\",\"books\":[{\"id\":\"1\",\"title\":\"abc\"}]}]}";

        assertQueryEquals(graphQLRequest, expectedResponse);
    }

    @Test
    public void testNestedCollection() {
        // TODO: Test nested update.
    }

    @Test
    public void testRootSingleWithIdFailFilter() {
        String graphQLRequest = "mutation { book(op:REPLACE, id: \"1\", data: {id: \"42\", title: \"abc\"}, filter: \"title=\\\"Book Dos\\\"\") { id, title } }";
        assertQueryFails(graphQLRequest);
    }

    @Test
    public void testRootSingleWithIdFailPaginate() {
        String graphQLRequest = "mutation { book(op:REPLACE, id: \"1\", data: {id: \"42\", title: \"abc\"}, first: 1) { id, title } }";
        assertQueryFails(graphQLRequest);

        graphQLRequest = "mutation { book(op:REPLACE, id: \"1\", data: {id: \"42\", title: \"abc\"}, offset: 1) { id, title } }";
        assertQueryFails(graphQLRequest);
    }

    @Test
    public void testRootSingleWithIdFailSort() {
        String graphQLRequest = "mutation { book(op:REPLACE, id: \"1\", data: {id: \"42\", title: \"abc\"}, sort: \"-title\") { id, title } }";
        assertQueryFails(graphQLRequest);
    }

    @Test
    public void testRootCollectionWithIdFailFilter() {
        String graphQLRequest = "mutation { book(op:REPLACE, id: \"1\", data: [{id: \"42\", title: \"abc\"},{id: \"42\", title: \"abc\"}], filter: \"title=\\\"Book Dos\\\"\") { id, title } }";
        assertQueryFails(graphQLRequest);
    }

    @Test
    public void testRootCollectionWithIdFailPaginate() {
        String graphQLRequest = "mutation { book(op:REPLACE, id: \"1\", data: [{id: \"42\", title: \"abc\"},{id: \"42\", title: \"abc\"}], first: 1) { id, title } }";
        assertQueryFails(graphQLRequest);

        graphQLRequest = "mutation { book(op:REPLACE, id: \"1\", data: [{id: \"42\", title: \"abc\"},{id: \"42\", title: \"abc\"}], offset: 1) { id, title } }";
        assertQueryFails(graphQLRequest);
    }

    @Test
    public void testRootCollectionWithIdFailSort() {
        String graphQLRequest = "mutation { book(op:REPLACE, id: \"1\", data: [{id: \"42\", title: \"abc\"},{id: \"42\", title: \"abc\"}] sort: \"-title\") { id, title } }";
        assertQueryFails(graphQLRequest);
    }
}
