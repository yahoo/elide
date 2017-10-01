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

import java.util.List;
import java.util.Map;

/**
 * Test the Fetch operation.
 */
public class FetcherFetchTest extends PersistentResourceFetcherTest {
    private static String NOT_IMPLEMENTED = "not implemented";

    @Test
    public void testRootSingle() throws Exception {
        String graphQLRequest = "{"
                + "book(ids: [\"1\"]) { "
                + "id "
                + "title "
                + "authors {"
                + "id "
                + "name "
                + "}"
                + "}"
                + "}";
        String expectedResponse = "{"
                + "\"book\":[{"
                + "\"id\":\"1\","
                + "\"title\":\"Libro Uno\","
                + "\"authors\":[{"
                + "\"id\":\"1\","
                + "\"name\":\"Mark Twain\""
                + "}]"
                + "}]"
                + "}";

        assertQueryEquals(graphQLRequest, expectedResponse);
    }

    @Test
    public void testRootMultipleIds() throws Exception {
        String graphQLRequest = "{ "
                + "book(ids: [\"1\", \"2\"]) { "
                + "id "
                + "title "
                + "} "
                + "}";
        String expectedResponse = "{"
                + "\"book\":[{"
                + "\"id\":\"1\","
                + "\"title\":\"Libro Uno\""
                + "},"
                + "{"
                + "\"id\":\"2\","
                + "\"title\":\"Libro Dos\""
                + "}]"
                + "}";

        assertQueryEquals(graphQLRequest, expectedResponse);
    }

    @Test
    public void testRootCollection() throws Exception {
        String graphQLRequest = "{ "
                + "book { "
                + "id "
                + "title "
                + "genre "
                + "language "
                + "} "
                + "}";
        String expectedResponse = "{"
                + "\"book\":[{"
                + "\"id\":\"1\","
                + "\"title\":\"Libro Uno\","
                + "\"genre\":null,"
                + "\"language\":null"
                + "},"
                + "{"
                + "\"id\":\"2\","
                + "\"title\":\"Libro Dos\","
                + "\"genre\":null,"
                + "\"language\":null"
                + "},"
                + "{\"id\":\"3\",\"title\":\"Doctor Zhivago\",\"genre\":null,\"language\":null}"
                + "]"
                + "}";

        assertQueryEquals(graphQLRequest, expectedResponse);
    }

    @Test
    public void testNestedSingle() throws Exception {
        String graphQLRequest = "{ "
                + "author(ids: [\"1\"]) { "
                + "name "
                + "penName { "
                + "name "
                + "} "
                + "} "
                + "}";
        String expectedResponse = "{"
                + "\"author\":[{"
                + "\"name\":\"Mark Twain\","
                + "\"penName\":{"
                + "\"name\":\"The People's Author\""
                + "}"
                + "}]"
                + "}";

        assertQueryEquals(graphQLRequest, expectedResponse);
    }

    @Test
    public void testNestedCollection() throws Exception {
        String graphQLRequest = "{ "
                + "author(ids: [\"1\"]) { "
                + "books(ids: [\"1\"]) { "
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
    }

    @Test
    public void testRootCollectionSort() throws Exception {
        String graphQLRequest = "{ "
                + "book(sort: \"-title\") { "
                + "id "
                + "title "
                + "} "
                + "}";
        String expectedResponse = "{\"book\":[{\"id\":\"1\",\"title\":\"Libro Uno\"},{\"id\":\"2\","
                + "\"title\":\"Libro Dos\"},{\"id\":\"3\",\"title\":\"Doctor Zhivago\"}]}";

        assertQueryEquals(graphQLRequest, expectedResponse);
    }

    @Test
    public void testRootCollectionMultiSort() throws Exception {
        String graphQLRequest = "{ "
                + "book(sort: \"-publisher.id,id\") { "
                + "id "
                + "title "
                + "publisher {"
                + "  id"
                + "}"
                + "} "
                + "}";
        String expectedResponse = "{\"book\":[{\"id\":\"3\",\"title\":\"Doctor Zhivago\",\"publisher\":{\"id\":\"2\"}},"
                + "{\"id\":\"1\",\"title\":\"Libro Uno\",\"publisher\":{\"id\":\"1\"}},{\"id\":\"2\","
                + "\"title\":\"Libro Dos\",\"publisher\":{\"id\":\"1\"}}]}";

        assertQueryEquals(graphQLRequest, expectedResponse);
    }

    @Test
    public void testNestedCollectionSort() throws Exception {
        String graphQLRequest = "{ "
                + "author(ids: [\"1\"]) { "
                + "books(sort: \"title\") { "
                + "id "
                + "title "
                + "} "
                + "} "
                + "}";
        String expectedResponse = "{"
                + "\"author\":[{"
                + "\"books\":[{"
                + "\"id\":\"2\","
                + "\"title\":\"Libro Dos\""
                + "},{"
                + "\"id\":\"1\","
                + "\"title\":\"Libro Uno\""
                + "}]"
                + "}]"
                + "}";

        assertQueryEquals(graphQLRequest, expectedResponse);
    }

    @Test
    public void testRootCollectionPaginate() throws Exception {
        /* Only first argument */
        String graphQLRequest = "{ "
                + "book(first: \"1\") { "
                + "id "
                + "title "
                + "} "
                + "}";
        String expectedResponse = "{"
                + "\"book\":[{"
                + "\"id\":\"1\","
                + "\"title\":\"Libro Uno\""
                + "}]"
                + "}";

        ExecutionResult result = api.execute(graphQLRequest, requestScope);
        assertQueryEquals(graphQLRequest, expectedResponse);

        /* Both first and offset argument */
        graphQLRequest = "{ "
                + "book(first: \"1\", offset: \"1\") { "
                + "id "
                + "title "
                + "} "
                + "}";
        expectedResponse = "{"
                + "\"book\":[{"
                + "\"id\":\"2\","
                + "\"title\":\"Libro Dos\""
                + "}]"
                + "}";
        assertQueryEquals(graphQLRequest, expectedResponse);
    }

    @Test
    public void testNestedCollectionPaginate() throws Exception {
        String graphQLRequest = "{ "
                + "author(ids: [\"1\"]) { "
                + "books(first: \"1\", offset: \"1\") { "
                + "id "
                + "title "
                + "} "
                + "} "
                + "}";
        String expectedResponse = "{"
                + "\"author\":[{"
                + "\"books\":[{"
                + "\"id\":\"2\","
                + "\"title\":\"Libro Dos\""
                + "}]"
                + "}]"
                + "}";
        assertQueryEquals(graphQLRequest, expectedResponse);
    }

    @Test
    public void testNestedCollectionFilter() throws Exception {
        String graphQLRequest = "{ author(ids: [\"1\"]) { books(filter: \"title==\\\"Libro U*\\\"\") { id title } } }";
        String expectedResponse = "{\"author\":[{\"books\":[{\"id\":\"1\",\"title\":\"Libro Uno\"}]}]}";

        assertQueryEquals(graphQLRequest, expectedResponse);
    }

    @Test
    public void testRootCollectionFilter() throws Exception {
        String graphQLRequest = "{ book(filter: \"title==\\\"Libro U*\\\"\") { id title } }";
        String expectedResponse = "{\"book\":[{\"id\":\"1\",\"title\":\"Libro Uno\"}]}";

        assertQueryEquals(graphQLRequest, expectedResponse);
    }

    @Test
    public void testFailuresWithBody() throws Exception {
        String graphQLRequest = "{ "
                + "book(ids: [\"1\"], data: [{\"id\": \"1\"}]) { "
                + "id "
                + "title "
                + "} "
                + "}";
        ExecutionResult result = api.execute(graphQLRequest, requestScope);
        Assert.assertTrue(!result.getErrors().isEmpty());
    }

    @Test
    public void testPageTotalsRoot() throws Exception {
        runComparisonTest("pageTotalsRoot");
    }

    @Test
    public void testPageTotalsRootWithFilter() throws Exception {
        runComparisonTest("pageTotalsRootWithFilter");
    }

    @Test
    public void testPageTotalsRootWithPagination() throws Exception {
        // TODO: Rename first and offset parameters
        runComparisonTest("pageTotalsRootWithPagination");
    }

    @Test
    public void testPageTotalsRootWithIds() throws Exception {
        runComparisonTest("pageTotalsRootWithIds");
    }

    @Test
    public void testPageTotalsRelationship() throws Exception {
        runComparisonTest("pageTotalsRelationship");
    }
}
