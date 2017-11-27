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
    public void testNestedCollection() throws Exception {
        // Part 1: Delete the objects
        runComparisonTest("nestedCollectionPt1");

        // Part 2: Make sure objects are really gone
        runComparisonTest("nestedCollectionPt2");
    }

    @Override
    public void runComparisonTest(String testName) throws Exception {
        super.runComparisonTest("remove/" + testName);
    }
}
