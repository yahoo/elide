/*
 * Copyright 2017, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql;

import org.testng.annotations.Test;

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

    // FIXME: Remove stack traces from error handler...
    @Test(enabled = false)
    public void testReplaceWithIdsFails() throws Exception {
        String expectedMessage = "Exception while fetching data: javax.ws.rs.BadRequestException: REPLACE "
                + "must not include ids argument";
        runErrorComparisonTest("replaceWithIdsFails", expectedMessage);
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
