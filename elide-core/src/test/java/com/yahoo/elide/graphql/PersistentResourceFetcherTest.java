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

import java.util.LinkedHashSet;

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
        String graphQLRequest = "{ book(id: '1') { id title } }";
        GraphQLContext context = new GraphQLContext(null, null, dictionary, new LinkedHashSet<>(), new LinkedHashSet<>());
        ExecutionResult result = api.execute(graphQLRequest, context);
        Assert.assertEquals(result.getErrors().size(), 0);
        Assert.assertEquals(result.getData(), "");
    }

    @Test
    public void testFetchRootCollection() {
        String graphQLRequest = "{ book { id title genre language } }";
        GraphQLContext context = new GraphQLContext(null, null, dictionary, new LinkedHashSet<>(), new LinkedHashSet<>());
        ExecutionResult result = api.execute(graphQLRequest, context);
        Assert.assertEquals(result.getErrors().size(), 0);
        Assert.assertEquals(result.getData(), "");
    }

    @Test
    public void testFetchNestedCollection() {
        String graphQLRequest = "{ author { id name books { id title genre language } } }";
        GraphQLContext context = new GraphQLContext(null, null, dictionary, new LinkedHashSet<>(), new LinkedHashSet<>());
        ExecutionResult result = api.execute(graphQLRequest, context);
        Assert.assertEquals(result.getErrors().size(), 0);
        Assert.assertEquals(result.getData(), "");
    }
}
