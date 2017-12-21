/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql;

import org.testng.annotations.Test;

/**
 * Test the Add operation.
 */
public class FetcherUpdateTest extends PersistentResourceFetcherTest {

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
    public void testRootCollectionInvalidIds() throws Exception {
        // Update 1, create for id 42, create new book with title "abc"
        String expectedMessage = "Exception while fetching data (/book) : Unknown identifier '[42]' for book";
        runErrorComparisonTest("rootCollectionInvalidIds", expectedMessage);
    }

    @Test
    public void testRootCollectionMissingIds() throws Exception {
        // Update 1, create for id 42, create new book with title "abc"
        String expectedMessage = "Exception while fetching data (/book) : UPDATE data objects must include ids";
        runErrorComparisonTest("rootCollectionMissingIds", expectedMessage);
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
    public void testNestedCollectionMissingIds() throws Exception {
        String expectedMessage = "Exception while fetching data (/author) : UPDATE data objects must include ids";
        runErrorComparisonTest("nestedCollectionMissingIds", expectedMessage);
    }

    @Test
    public void testCrossCyclicRelationships() throws Exception {
        runComparisonTest("crossCyclicRelationships");
    }

    @Test
    public void testNestedUpdates() throws Exception {
        runComparisonTest("nestedUpdates");
    }

    @Test
    public void testUpdateComplexGraph() throws Exception {
        runComparisonTest("updateComplexGraph");
    }

    // TODO: Reeanble when supporting arguments into computed attributes.
    @Test(enabled = false)
    public void testSetComputedAttribute() throws Exception {
        runComparisonTest("setComputedAttribute");
    }

    @Override
    public void runComparisonTest(String testName) throws Exception {
        super.runComparisonTest("update/" + testName);
    }

    @Override
    public void runErrorComparisonTest(String testName, String expectedMessage) throws Exception {
        super.runErrorComparisonTest("update/" + testName, expectedMessage);
    }
}
