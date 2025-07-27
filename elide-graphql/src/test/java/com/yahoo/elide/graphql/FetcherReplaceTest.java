/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql;

import org.junit.jupiter.api.Test;

public class FetcherReplaceTest extends PersistentResourceFetcherTest {

    @Test
    public void testReplaceAttributeRoot() throws Exception {
        runComparisonTest("replaceRootCollection");
    }

    @Test
    public void testReplaceNestedCollection() throws Exception {
        runComparisonTest("replaceNestedCollection");
    }

    @Test
    public void testReplaceEmptyCollections() throws Exception {
        runComparisonTest("replaceEmptyCollections");
    }

    @Test
    public void testReplaceWithIdsFails() throws Exception {
        String expectedMessage = "Exception while fetching data (/book) : REPLACE must not include ids argument";
        runErrorComparisonTest("replaceWithIdsFails", expectedMessage);
    }

    @Test
    public void testReplaceNestedToOneWithId() throws Exception {
        String graphQLRequest = "mutation { "
                + "author(ids: [\"3\"]) { "
                + "edges { node {"
                + "penName(op:REPLACE, data: { id: \"1\"}) { "
                + "edges { node { id name } } "
                + "} } }"
                + "}"
                + "}";
        assertQueryEquals(graphQLRequest, "{\"author\":{\"edges\":[{\"node\":{\"penName\":{\"edges\":[{\"node\":{\"id\":\"1\",\"name\":\"The People's Author\"}}]}}}]}}");
    }

    @Test
    public void testReplaceNestedToOneWithData() throws Exception {
        String graphQLRequest = "mutation { "
                + "author(ids: [\"3\"]) { "
                + "edges { node {"
                + "penName(op:REPLACE, data: { name: \"Hello World\"}) { "
                + "edges { node { id name } } "
                + "} } }"
                + "}"
                + "}";
        assertQueryEquals(graphQLRequest, "{\"author\":{\"edges\":[{\"node\":{\"penName\":{\"edges\":[{\"node\":{\"id\":\"3\",\"name\":\"Hello World\"}}]}}}]}}");
    }

    @Test
    public void testReplaceNestedToOneWithNullData() throws Exception {
        String graphQLRequest = "mutation { "
                + "author(ids: [\"3\"]) { "
                + "edges { node {"
                + "penName(op:REPLACE, data: null) { "
                + "edges { node { id name } } "
                + "} } }"
                + "}"
                + "}";
        assertQueryFailsWith(graphQLRequest, "Exception while fetching data (/author/edges[0]/node/penName) : REPLACE must include data argument");
    }

    @Test
    public void testReplaceNestedToOneWithEmptyData() throws Exception {
        String graphQLRequest = "mutation { "
                + "author(ids: [\"3\"]) { "
                + "edges { node {"
                + "penName(op:REPLACE, data: {}) { "
                + "edges { node { id name } } "
                + "} } }"
                + "}"
                + "}";
        assertQueryEquals(graphQLRequest, "{\"author\":{\"edges\":[{\"node\":{\"penName\":{\"edges\":[{\"node\":{\"id\":\"3\",\"name\":null}}]}}}]}}");
    }

    @Override
    public void runComparisonTest(String testName) throws Exception {
        super.runComparisonTest("replace/" + testName);
    }

    @Override
    public void runErrorComparisonTest(String testName, String expectedMessage) throws Exception {
        super.runErrorComparisonTest("replace/" + testName, expectedMessage);
    }
}
