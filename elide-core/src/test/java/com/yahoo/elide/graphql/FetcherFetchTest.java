/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql;

import com.fasterxml.jackson.core.JsonProcessingException;
import graphql.ExecutionResult;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Test the Fetch operation.
 */
public class FetcherFetchTest extends AbstractPersistentResourceFetcherTest {
    @Test
    public void testRootSingle() throws JsonProcessingException {
        String graphQLRequest = "{ book(id: \"1\") { id title } }";
        String expectedResponse = "{\"book\":[{\"id\":\"1\",\"title\":\"Libro Uno\"}]}";

        assertQueryEquals(graphQLRequest, expectedResponse);
    }

    @Test
    public void testRootCollection() throws JsonProcessingException {
        String graphQLRequest = "{ book { id title genre language } }";
        String expectedResponse = "{\"book\":[{\"id\":\"1\",\"title\":\"Libro Uno\",\"genre\":null,\"language\":null},{\"id\":\"2\",\"title\":\"Libro Dos\",\"genre\":null,\"language\":null}]}";

        assertQueryEquals(graphQLRequest, expectedResponse);
    }

    @Test
    public void testRootCollectionFilter() throws JsonProcessingException {
        String graphQLRequest = "{ book(filter: \"title=\\\"Book*\\\"\") { id title } }";
        String expectedResponse = "{\"book\":[{\"id\":\"1\",\"title\":\"Libro Uno\"}]}";

        Assert.fail("Not implemented");
    }

    @Test
    public void testRootCollectionSort() throws JsonProcessingException {
        String graphQLRequest = "{ book(sort: \"-title\") { id title } }";
        String expectedResponse = "{\"book\":[{\"id\":\"1\",\"title\":\"Libro Uno\"}]}";

        Assert.fail("Not implemented");
    }

    @Test
    public void testRootCollectionPaginate() throws JsonProcessingException {
        String graphQLRequest = "{ book(first: 1) { id title } }";
        String expectedResponse = "{\"book\":[{\"id\":\"1\",\"title\":\"Libro Uno\"}]}";


        graphQLRequest = "{ book(first: 1, offset: 1) { id title } }";
        expectedResponse = "{\"book\":[{\"id\":\"1\",\"title\":\"Libro Dos\"}]}";

        Assert.fail("Not implemented");
    }

    @Test
    public void testNestedSingle() throws JsonProcessingException {
        String graphQLRequest = "{ author(id: \"1\") { books(id: \"1\") { id title } } }";
        String expectedResponse = "{\"author\":[{\"books\":[{\"id\":\"1\",\"title\":\"Libro Uno\"}]}]}";

        assertQueryEquals(graphQLRequest, expectedResponse);
    }

    @Test
    public void testNestedCollection() throws JsonProcessingException {
        String graphQLRequest = "{ author(id: \"1\") { books { id title } } }";
        String expectedResponse = "{\"author\":[{\"books\":[{\"id\":\"1\",\"title\":\"Libro Uno\"},{\"id\":\"2\",\"title\":\"Libro Dos\"}]}]}";

        assertQueryEquals(graphQLRequest, expectedResponse);
    }

    @Test
    public void testNestedCollectionFilter() throws JsonProcessingException {
        String graphQLRequest = "{ author(id: \"1\") { books { id title } } }";
        String expectedResponse = "{\"author\":[{\"books\":[{\"id\":\"1\",\"title\":\"Libro Uno\"}]}]}";

        Assert.fail("Not Implemented");
    }

    @Test
    public void testNestedCollectionSort() throws JsonProcessingException {
        String graphQLRequest = "{ author(id: \"1\") { books { id title } } }";
        String expectedResponse = "{\"author\":[{\"books\":[{\"id\":\"1\",\"title\":\"Libro Uno\"}]}]}";

        Assert.fail("Not Implemented");
    }

    @Test
    public void testNestedCollectionPaginate() throws JsonProcessingException {
        String graphQLRequest = "{ author(id: \"1\") { books { id title } } }";
        String expectedResponse = "{\"author\":[{\"books\":[{\"id\":\"1\",\"title\":\"Libro Uno\"}]}]}";

        Assert.fail("Not Implemented");
    }

    @Test
    public void testFailuresWithBody() throws JsonProcessingException {
        String graphQLRequest = "{ book(id: \"1\", data: [{\"id\": \"1\"}]) { id title } }";
        ExecutionResult result = api.execute(graphQLRequest, requestScope);
        Assert.assertTrue(!result.getErrors().isEmpty());
    }
}
