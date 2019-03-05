/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql;

import org.testng.annotations.Test;

import java.util.HashMap;

/**
 * Test the Add operation.
 */
public class FetcherUpsertTest extends PersistentResourceFetcherTest {
    /* ==================== */
    /* CREATING NEW OBJECTS */
    /* ==================== */
    @Test
    public void testCreateRootSingle() throws Exception {
        runComparisonTest("createRootSingle");
    }

    @Test
    public void testCreateRootCollection() throws Exception {
        runComparisonTest("createRootCollection");
    }

    @Test
    public void testCreateNestedSingle() throws Exception {
        runComparisonTest("createNestedSingle");
    }

    @Test
    public void testCreateNestedSingleUnshareable() throws Exception {
        runComparisonTest("createNestedSingleUnshareable");
    }


    @Test
    public void testCreateNestedCollection() throws Exception {
        runComparisonTest("createNestedCollection");
    }

    /* ========================= */
    /* UPDATING EXISTING OBJECTS */
    /* ========================= */
    @Test
    public void testRootSingleWithId() throws Exception {
        //author 1 already exist, should update
        runComparisonTest("rootSingleWithId");
    }

    @Test
    public void testRootSingleWithList() throws Exception {
        //book 1 and 2 already exist, should update
        runComparisonTest("rootSingleWithList");
    }

    @Test
    public void testRootCollectionMixedIds() throws Exception {
        // Update 1, create for id 42, create new book with title "abc"
        runComparisonTest("rootCollectionMixedIds");
    }

    @Test
    public void testNestedSingleUpdate() throws Exception {
        runComparisonTest("nestedSingleUpdate");
    }

    @Test
    public void testNestedCollection() throws Exception {
        runComparisonTest("nestedCollection");
    }

    @Test
    public void testUpsertOnCollection() throws Exception {
        runComparisonTest("uspertOnCollection");
    }

    @Test
    public void testNonCreatedIdReferenceCollection() throws Exception {
        runComparisonTest("nonCreatedIdReferenceCollection");
    }

    @Test
    public void testCrossCyclicRelationships() throws Exception {
        runComparisonTest("crossCyclicRelationships");
    }

    @Test
    public void testNestedUpserts() throws Exception {
        runComparisonTest("nestedUpserts");
    }

    @Test
    public void testNonCreatedIdOnlyRequest2Back() throws Exception {
        runComparisonTest("nonCreatedIdOnlyRequest2Back");
    }

    // TODO: Reeanble when supporting arguments into computed attributes.
    @Test(enabled = false)
    public void testSetComputedAttribute() throws Exception {
        runComparisonTest("setComputedAttribute");
    }

    @Test
    public void testCreateWithVariables() throws Exception {
        super.runComparisonTestWithVariables("upsert/createWithVariables", new HashMap<String, Object>() {
            {
                put("title", "My new book title from variable!");
                put("publicationDate", "1985-12-25T00:00Z");
                put("publisherId", "b9aa44b2-8193-4fb3-84ed-613ef104e7c3");
                put("publisherName", "my new publisher");
            }
        });
    }

    @Override
    public void runComparisonTest(String testName) throws Exception {
        super.runComparisonTest("upsert/" + testName);
    }
}
