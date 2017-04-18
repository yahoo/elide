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
    public void testRootBadInput() {
        String graphQLRequest = "mutation { author(op:DELETE) { id } }";
        assertQueryFails(graphQLRequest);
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
    public void testRootIdWithBadData() {
        String graphQLRequest = "mutation { author(op:DELETE, id: \"1\", data: {id: \"2\"}) { id } }";
        assertQueryFails(graphQLRequest);
    }

    @Test
    public void testRootIdWithExtraData() {
        String graphQLRequest = "mutation { author(op:DELETE, id: \"1\", data: [{id: \"1\"}, {id: \"2\"}]) { id } }";
        assertQueryFails(graphQLRequest);
    }

    @Test
    public void testRootCollection() throws JsonProcessingException {
        String graphQLRequest = "mutation { author(op:DELETE, data: [{id: \"1\"}, {id: \"2\"}]) { id } }";
        String expectedResponse = "{}";

        assertQueryEquals(graphQLRequest, expectedResponse);
    }

    @Test
    public void testNestedBadInput() {
        String graphQLRequest = "mutation { author(id: \"1\") { books(op:DELETE) { id } } }";
        assertQueryFails(graphQLRequest);
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

    @Test
    public void testRootSingleFailPagination() {
        String graphQLRequest = "mutation { author(op:DELETE, id: \"1\", first: 1) { id } }";
        assertQueryFails(graphQLRequest);

        graphQLRequest = "mutation { author(op:DELETE, id: \"1\", offset: 10) { id } }";
        assertQueryFails(graphQLRequest);
    }

    @Test
    public void testRootCollectionFailPagination() {
        String graphQLRequest = "mutation { author(op:DELETE, data: [{id: \"1\"}, {id: \"2\"}], first: 1) { id } }";
        assertQueryFails(graphQLRequest);

        graphQLRequest = "mutation { author(op:DELETE, data: [{id: \"1\"}, {id: \"2\"}], offset: 10) { id } }";
        assertQueryFails(graphQLRequest);
    }

    @Test
    public void testRootSingleFailSorting() {
        String graphQLRequest = "mutation { author(op:DELETE, id: \"1\", sort=\"name\") { id } }";
        assertQueryFails(graphQLRequest);
    }

    @Test
    public void testRootCollectionFailSorting() {
        String graphQLRequest = "mutation { author(op:DELETE, data: [{id: \"1\"}, {id: \"2\"}], sort=\"name\") { id } }";
        assertQueryFails(graphQLRequest);
    }
    @Test
    public void testRootSingleFailFiltering() {
        String graphQLRequest = "mutation { author(op:DELETE, id: \"1\", filter=\"name=\\\"Author\\\"\") { id } }";
        assertQueryFails(graphQLRequest);
    }

    @Test
    public void testRootCollectionFailFiltering() {
        String graphQLRequest = "mutation { author(op:DELETE, data: [{id: \"1\"}, {id: \"2\"}], filter=\"name=\\\"Author\\\"\") { id } }";
        assertQueryFails(graphQLRequest);
    }
}
