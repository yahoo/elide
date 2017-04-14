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
        String expectedResponse = "{\"book\":[{\"id\":\"1\",\"title\":\"Book Numero Uno\"}]}";

        assertQueryEquals(graphQLRequest, expectedResponse);
    }

    @Test
    public void testRootCollection() throws JsonProcessingException {
        String graphQLRequest = "{ book { id title genre language } }";
        String expectedResponse = "{\"book\":[{\"id\":\"1\",\"title\":\"Book Numero Uno\",\"genre\":null,\"language\":null}]}";

        assertQueryEquals(graphQLRequest, expectedResponse);
    }

    @Test
    public void testNestedSingle() throws JsonProcessingException {
        String graphQLRequest = "{ author(id: \"1\") { books(id: \"1\") { id title } } }";
        String expectedResponse = "{\"author\":[{\"books\":[{\"id\":\"1\",\"title\":\"Book Numero Uno\"}]}]}";

        assertQueryEquals(graphQLRequest, expectedResponse);
    }

    @Test
    public void testNestedCollection() throws JsonProcessingException {
        String graphQLRequest = "{ author(id: \"1\") { books { id title } } }";
        String expectedResponse = "{\"author\":[{\"books\":[{\"id\":\"1\",\"title\":\"Book Numero Uno\"}]}]}";

        assertQueryEquals(graphQLRequest, expectedResponse);
    }

    @Test
    public void testFailuresWithBody() throws JsonProcessingException {
        String graphQLRequest = "{ book(id: \"1\", data: [{\"id\": \"1\"}]) { id title } }";
        ExecutionResult result = api.execute(graphQLRequest, requestScope);
        Assert.assertTrue(!result.getErrors().isEmpty());
    }
}
