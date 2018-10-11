/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql;

import org.testng.Assert;
import org.testng.annotations.Test;

import graphql.ExecutionResult;

/**
 * Test the Fetch operation.
 *
 * NOTE: Resource files for request/response for `runComparisonTest` are found in the
 *   /resources/graphql/{requests,responses}/fetch directory, respectively.
 */
public class FetcherFetchTest extends PersistentResourceFetcherTest {

    @Test
    public void testRootSingle() throws Exception {
        runComparisonTest("rootSingle");
    }

    @Test
    public void testRootMultipleIds() throws Exception {
        runComparisonTest("rootMultipleIds");
    }

    @Test
    public void testRootCollection() throws Exception {
        runComparisonTest("rootCollection");
    }

    @Test
    public void testNestedSingle() throws Exception {
        runComparisonTest("nestedSingle");
    }

    @Test
    public void testNestedCollection() throws Exception {
        runComparisonTest("nestedCollection");
    }

    @Test
    public void testRootCollectionSort() throws Exception {
        runComparisonTest("rootCollectionSort");
    }

    @Test
    public void testRootCollectionMultiSort() throws Exception {
        runComparisonTest("rootCollectionMultiSort");
    }

    @Test
    public void testNestedCollectionSort() throws Exception {
        runComparisonTest("nestedCollectionSort");
    }

    @Test
    public void testRootCollectionPaginateWithoutOffset() throws Exception {
        runComparisonTest("rootCollectionPaginateWithoutOffset");
    }

    @Test
    public void testRootCollectionPaginateWithOffset() throws Exception {
        runComparisonTest("rootCollectionPaginateWithOffset");
    }

    @Test
    public void testNestedCollectionPaginate() throws Exception {
        runComparisonTest("nestedCollectionPaginate");
    }

    @Test
    public void testNestedCollectionFilter() throws Exception {
        runComparisonTest("nestedCollectionFilter");
    }

    @Test
    public void testRootCollectionFilter() throws Exception {
        runComparisonTest("rootCollectionFilter");
    }

    @Test
    public void testDateGreaterThanFilter() throws Exception {
        runComparisonTest("rootCollectionDateGTFilter");
    }

    @Test
    public void testDateLessThanFilter() throws Exception {
        runComparisonTest("rootCollectionDateLTFilter");
    }

    @Test
    public void testFailuresWithBody() throws Exception {
        String graphQLRequest = "{ "
                + "book(ids: [\"1\"], data: [{\"id\": \"1\"}]) { "
                + "edges { node { "
                + "id "
                + "title "
                + "}}"
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

    @Test
    public void testComputedAttributes() throws Exception {
        runComparisonTest("computedAttributes");
    }

    @Test
    public void testFetchWithFragments() throws Exception {
        runComparisonTest("fetchWithFragment");
    }

    @Test
    public void testSchemaIntrospection() throws Exception {
        String graphQLRequest = "{"
            + "__schema {"
            + "types {"
            + "   name"
            + "}"
            + "}"
            + "}";
        ExecutionResult result = api.execute(graphQLRequest, requestScope);

        Assert.assertTrue(result.getErrors().isEmpty());
    }

    @Test
    public void testTypeIntrospection() throws Exception {
        String graphQLRequest = "{"
            + "__type(name: \"author\") {"
            + "   name"
            + "   fields {"
            + "     name"
            + "   }"
            + "}"
            + "}";
        ExecutionResult result = api.execute(graphQLRequest, requestScope);

        Assert.assertTrue(result.getErrors().isEmpty());
    }

    @Override
    public void runComparisonTest(String testName) throws Exception {
        // Preface with "fetch" directory:
        super.runComparisonTest("fetch/" + testName);
    }
}
