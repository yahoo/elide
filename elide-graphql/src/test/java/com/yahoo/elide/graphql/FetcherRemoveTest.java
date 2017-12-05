/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql;

import org.testng.annotations.Test;

/**
 * Test the Delete operation.
 */
public class FetcherRemoveTest extends PersistentResourceFetcherTest {
    @Test
    public void testRootBadInput() {
        String graphQLRequest = "mutation { "
                    + "author(op:REMOVE) { "
                    + "edges { node { id } } "
                    + "} "
                    + "}";
        assertQueryFails(graphQLRequest);
    }


    @Test
    public void testRootIdWithBadData() {
        String graphQLRequest = "mutation { "
                    + "author(op:REMOVE, ids: [\"1\"], data: {id: \"2\"}) { "
                    + "edges { node { id } } "
                    + "} "
                    + "}";
        assertQueryFails(graphQLRequest);
    }


    public void testRootCollectionNothingToRemove() throws Exception {
        String graphQLRequest = "mutation { "
                + "book(op:REMOVE, ids: [\"4\"]) { "
                + "edges { node { id } } "
                + "}"
                + "}";
        assertQueryFails(graphQLRequest);
    }

    @Test
    public void testNestedBadInput() {
        String graphQLRequest = "mutation { "
                + "author(id: \"1\") { "
                + "books(op:REMOVE) { "
                + "edges { node { id } } "
                + "} "
                + "} "
                + "}";
        assertQueryFails(graphQLRequest);
    }

    @Test
    public void testRootCollection() throws Exception {
        // Part 1: Delete the objects
        runComparisonTest("rootCollectionPt1");

        // Part 2: Make sure objects are really gone
        String graphQLRequest = "{\n"
                + "  book(ids: [\"1\", \"2\"]) {\n"
                + "    edges {\n"
                + "      node {\n"
                + "        id\n"
                + "        title\n"
                + "      }\n"
                + "    }\n"
                + "  }\n"
                + "}";
        assertQueryFails(graphQLRequest);
    }

    @Test
    public void testNestedCollection() throws Exception {
        // Part 1: Delete the objects
        runComparisonTest("nestedCollectionPt1");

        // Part 2: Make sure objects are really gone from the relationship
        runComparisonTest("nestedCollectionPt2");

        // Part 3: Make sure objects are not deleted
        runComparisonTest("nestedCollectionPt3");
    }

    @Test
    public void testNestedCollectionNothingToRemove() throws Exception {
        String graphQLRequest = "mutation { "
                + "author(ids: [\"1\"]) { "
                + "edges { node {"
                + "books(op:REMOVE, ids: [\"4\"]) { "
                + "edges { node { id } } "
                + "} } }"
                + "}"
                + "}";
        assertQueryFails(graphQLRequest);
    }

    @Override
    public void runComparisonTest(String testName) throws Exception {
        super.runComparisonTest("remove/" + testName);
    }
}
