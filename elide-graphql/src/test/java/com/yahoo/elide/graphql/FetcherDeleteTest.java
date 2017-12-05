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
public class FetcherDeleteTest extends PersistentResourceFetcherTest {
    @Test
    public void testRootBadInput() {
        String graphQLRequest = "mutation { "
                    + "author(op:DELETE) { "
                    + "edges { node { id } } "
                    + "} "
                    + "}";
        assertQueryFails(graphQLRequest);
    }

    @Test
    public void testRootIdNoData() throws Exception {
        // Part1: Delete the object
        runComparisonTest("rootIdNoDataPt1");

        // Part2: Make sure it's really gone
        String graphQLRequest = "{\n"
                + "  author(ids: [\"1\"]) {\n"
                + "    edges {\n"
                + "      node {\n"
                + "        id\n"
                + "        name\n"
                + "      }\n"
                + "    }\n"
                + "  }\n"
                + "}";
        assertQueryFails(graphQLRequest);
    }

    @Test
    public void testRootIdWithBadData() {
        String graphQLRequest = "mutation { "
                    + "author(op:DELETE, ids: [\"1\"], data: {id: \"2\"}) { "
                    + "edges { node { id } } "
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
    public void testNestedBadInput() {
        String graphQLRequest = "mutation { "
                + "author(id: \"1\") { "
                + "books(op:DELETE) { "
                + "edges { node { id } } "
                + "} "
                + "} "
                + "}";
        assertQueryFails(graphQLRequest);
    }


    @Test
    public void testNestedSingleId() throws Exception {
        // Part 1: Delete the objects
        runComparisonTest("nestedSingleId");

        // Part 2: Make sure objects are really gone. Should fail with an "unknown identifier" error for books 1
        String graphQLFetchRequest = "mutation { "
                + "author(ids: [\"1\"]) { "
                + "edges { node { books(ids: [\"1\"]) { "
                + "edges { node { title } } "
                + "} }"
                + "} "
                + "} "
                + "}";

        assertQueryFails(graphQLFetchRequest);
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
        super.runComparisonTest("delete/" + testName);
    }
}
