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
public class FetcherDeleteTest extends AbstractPersistentResourceFetcherTest {

    @Test
    public void testRootBadInput() throws JsonProcessingException {
        String graphQLRequest = "mutation { author(op:DELETE) { id } }";
        String expectedResponse = "{}";

        assertQueryEquals(graphQLRequest, expectedResponse);
    }

    @Test
    public void testRootIdNoData() throws JsonProcessingException {
        String graphQLRequest = "mutation { author(op:DELETE, id: \"1\") { id } }";
        String expectedResponse = "{}";

        assertQueryEquals(graphQLRequest, expectedResponse);
    }

    @Test
    public void testRootIdWithGoodData() throws JsonProcessingException {
        String graphQLRequest = "mutation { author(op:DELETE, id: \"1\", data: {id: \"1\"}) { id } }";
        String expectedResponse = "{}";

        assertQueryEquals(graphQLRequest, expectedResponse);
    }

    @Test
    public void testRootIdWithBadData() throws JsonProcessingException {
        String graphQLRequest = "mutation { author(op:DELETE, id: \"1\", data: {id: \"2\"}) { id } }";
        String expectedResponse = "{}";

        assertQueryEquals(graphQLRequest, expectedResponse);
    }

    @Test
    public void testRootIdWithExtraData() throws JsonProcessingException {
        String graphQLRequest = "mutation { author(op:DELETE, id: \"1\", data: [{id: \"1\"}, {id: \"2\"}]) { id } }";
        String expectedResponse = "{}";

        assertQueryEquals(graphQLRequest, expectedResponse);
    }

    @Test
    public void testRootCollection() throws JsonProcessingException {
        String graphQLRequest = "mutation { author(op:DELETE, data: [{id: \"1\"}, {id: \"2\"}]) { id } }";
        String expectedResponse = "{}";

        assertQueryEquals(graphQLRequest, expectedResponse);
    }

    @Test
    public void testNestedBadInput() throws JsonProcessingException {
        String graphQLRequest = "mutation { author(id: \"1\") { books(op:DELETE) { id } } }";
        String expectedResponse = "{}";

        assertQueryEquals(graphQLRequest, expectedResponse);
    }


    @Test
    public void testNestedSingleId() throws JsonProcessingException {
        String graphQLRequest = "mutation { author(id: \"1\") { books(op:DELETE, id: \"1\") { id } } }";
        String expectedResponse = "{}";

        assertQueryEquals(graphQLRequest, expectedResponse);
    }

    @Test
    public void testNestedSingleData() throws JsonProcessingException {
        String graphQLRequest = "mutation { author(id: \"1\") { books(op:DELETE, data: {id: \"1\"}) { id } } }";
        String expectedResponse = "{}";

        assertQueryEquals(graphQLRequest, expectedResponse);
    }

    @Test
    public void testNestedCollection() throws JsonProcessingException {
        String graphQLRequest = "mutation { author(id: \"1\") { books(op:DELETE, data: [{id: \"1\"}, {id: \"2\"}]) { id } } }";
        String expectedResponse = "{}";

        assertQueryEquals(graphQLRequest, expectedResponse);
    }
}
