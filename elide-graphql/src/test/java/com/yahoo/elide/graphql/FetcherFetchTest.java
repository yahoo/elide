/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yahoo.elide.ElideResponse;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

/**
 * Test the Fetch operation.
 *
 * NOTE: Resource files for request/response for `runComparisonTest` are found in the
 *   /resources/graphql/{requests,responses}/fetch directory, respectively.
 */
public class FetcherFetchTest extends PersistentResourceFetcherTest {

    private final String baseUrl = "http://localhost:8080/graphql";

    @Test
    public void testMutationInQueryThrowsError() throws Exception {
        String query = "query {\n"
                + "  book(op: UPSERT, data: {title: \"Book Numero Dos\", publicationDate: \"1984-12-25T00:00Z\"} ) {\n"
                + "    edges {\n"
                + "      node {\n"
                + "        title\n"
                + "        publicationDate\n"
                + "      }\n"
                + "    }\n"
                + "  }\n"
                + "}";

        assertQueryFailsWith(query, "Exception while fetching data (/book) : Data model writes are only allowed in mutations");
    }

    @Test
    public void testSubscriptionThrowsError() throws Exception {
        String query = "subscription {\n"
                + "  book(ids: [\"1\"]) {\n"
                + "    edges {\n"
                + "      node {\n"
                + "        id\n"
                + "        title\n"
                + "      }\n"
                + "    }\n"
                + "  }\n"
                + "}";

        assertQueryFailsWith(query, "Schema is not configured for subscriptions.");
    }

    @Test
    public void testRootSingle() throws Exception {
        runComparisonTest("rootSingle");
    }

    @Test
    public void testRootUnknownField() throws Exception {
        assertParsingFails(loadGraphQLRequest("fetch/rootUnknownField.graphql"));
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
    public void testRootCollectionInvalidSort() throws Exception {
        assertParsingFails(loadGraphQLRequest("fetch/rootCollectionInvalidSort.graphql"));
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
    public void testFilterByAlias() throws Exception {
        runComparisonTest("filterByAlias");
    }

    @Test
    public void sortByAlias() throws Exception {
        runComparisonTest("sortByAlias");
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
    public void testFailuresWithBody() {
        String graphQLRequest = "{ "
                + "book(ids: [\"1\"], data: [{\"id\": \"1\"}]) { "
                + "edges { node { "
                + "id "
                + "title "
                + "}}"
                + "} "
                + "}";

        assertParsingFails(graphQLRequest);
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
    public void testFragmentOnNode() throws Exception {
        runComparisonTest("fragmentOnNode");
    }

    @Test
    public void testFragmentOnConnection() throws Exception {
        runComparisonTest("fragmentOnConnection");
    }

    @Test
    public void testFragmentOnEdges() throws Exception {
        runComparisonTest("fragmentOnEdges");
    }

    @Test
    public void testFragmentLoop() throws Exception {
        assertParsingFails(loadGraphQLRequest("fetch/fragmentLoop.graphql"));
    }

    @Test
    public void testFragmentInline() throws Exception {
        assertParsingFails(loadGraphQLRequest("fetch/fragmentInline.graphql"));
    }

    @Test
    public void testFragmentUnknown() throws Exception {
        assertParsingFails(loadGraphQLRequest("fetch/fragmentUnknown.graphql"));
    }

    @Test
    public void testVariableDefinition() throws Exception {
        runComparisonTest("variableDefinition");
    }

    @Test
    public void testVariableUnknownReference() throws Exception {
        assertParsingFails(loadGraphQLRequest("fetch/variableUnknownReference.graphql"));
    }

    @Test
    public void testVariableInvalidNonNull() throws Exception {
        assertParsingFails(loadGraphQLRequest("fetch/variableInvalidNonNull.graphql"));
    }

    @Test
    public void testAliasAttribute() throws Exception {
        runComparisonTest("aliasAttribute");
    }

    @Test
    public void testAliasRelationship() throws Exception {
        runComparisonTest("aliasRelationship");
    }

    @Test
    public void testAliasSameRelationship() throws Exception {
        runComparisonTest("aliasSameRelationship");
    }

    @Test
    public void testAliasAmbiguous() throws Exception {
        assertParsingFails(loadGraphQLRequest("fetch/aliasAmbiguous.graphql"));
    }

    @Test
    public void testAliasPartialQueryAmbiguous() throws Exception {
        assertParsingFails(loadGraphQLRequest("fetch/aliasPartialQueryAmbiguous.graphql"));
    }

    @Test
    public void testFederationServiceIntrospection() throws Exception {
        String graphQLRequest = "{ _service { sdl }}";

        ElideResponse response = runGraphQLRequest(graphQLRequest, new HashMap<>());

        assertTrue(! response.getBody().contains("errors"));
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

        ElideResponse response = runGraphQLRequest(graphQLRequest, new HashMap<>());

        assertTrue(! response.getBody().contains("errors"));
    }

    @Test
    public void testTypeIntrospection() throws Exception {
        String graphQLRequest = "{"
            + "__type(name: \"Author\") {"
            + "   name"
            + "   fields {"
            + "     name"
            + "     type { name }"
            + "   }"
            + "}"
            + "}";

        ElideResponse response = runGraphQLRequest(graphQLRequest, new HashMap<>());

        assertTrue(! response.getBody().contains("errors"));
    }

    @Override
    public void runComparisonTest(String testName) throws Exception {
        // Preface with "fetch" directory:
        super.runComparisonTest("fetch/" + testName);
    }
}
