/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.RequestScope;
import org.junit.jupiter.api.Test;

import graphql.ExecutionResult;

/**
 * Test the Fetch operation.
 *
 * NOTE: Resource files for request/response for `runComparisonTest` are found in the
 *   /resources/graphql/{requests,responses}/fetch directory, respectively.
 */
public class FetcherFetchTest extends PersistentResourceFetcherTest {

    private final String baseUrl = "http://localhost:8080/graphql";

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
    public void testComplexAttribute() throws Exception {
        runComparisonTest("complexAttribute");
    }

    @Test
    public void testComplexAttributeList() throws Exception {
        runComparisonTest("complexAttributeList");
    }

    @Test
    public void testComplexAttributeMap() throws Exception {
        runComparisonTest("complexAttributeMap");
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
        DataStoreTransaction tx = inMemoryDataStore.beginTransaction();
        RequestScope requestScope = new GraphQLRequestScope(baseUrl, tx, null, settings);

        String graphQLRequest = "{ "
                + "book(ids: [\"1\"], data: [{\"id\": \"1\"}]) { "
                + "edges { node { "
                + "id "
                + "title "
                + "}}"
                + "} "
                + "}";
        ExecutionResult result = api.execute(graphQLRequest, requestScope);
        assertTrue(!result.getErrors().isEmpty());
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
        DataStoreTransaction tx = inMemoryDataStore.beginTransaction();
        RequestScope requestScope = new GraphQLRequestScope(baseUrl, tx, null, settings);

        String graphQLRequest = "{"
            + "__schema {"
            + "types {"
            + "   name"
            + "}"
            + "}"
            + "}";
        ExecutionResult result = api.execute(graphQLRequest, requestScope);

        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    public void testTypeIntrospection() throws Exception {
        DataStoreTransaction tx = inMemoryDataStore.beginTransaction();
        RequestScope requestScope = new GraphQLRequestScope(baseUrl, tx, null, settings);

        String graphQLRequest = "{"
            + "__type(name: \"author\") {"
            + "   name"
            + "   fields {"
            + "     name"
            + "   }"
            + "}"
            + "}";
        ExecutionResult result = api.execute(graphQLRequest, requestScope);

        assertTrue(result.getErrors().isEmpty());
    }

    @Override
    public void runComparisonTest(String testName) throws Exception {
        // Preface with "fetch" directory:
        super.runComparisonTest("fetch/" + testName);
    }
}
