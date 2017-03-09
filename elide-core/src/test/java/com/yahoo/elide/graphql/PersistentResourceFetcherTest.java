/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql;

import graphql.ExecutionResult;
import graphql.GraphQL;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Test our data fetcher.
 */
public class PersistentResourceFetcherTest extends AbstractGraphQLTest {
    protected GraphQL api;

    @BeforeMethod
    public void setup() {
        PersistentResourceFetcher fetcher = new PersistentResourceFetcher();
        ModelBuilder builder = new ModelBuilder(dictionary, fetcher);
        api = new GraphQL(builder.build());
    }

    @Test
    public void testFetchRootObject() {
        String graphQLRequest = "{ book { title } }";
        ExecutionResult result = api.execute(graphQLRequest);
        Assert.assertEquals(result.getData(), "");
    }
}
