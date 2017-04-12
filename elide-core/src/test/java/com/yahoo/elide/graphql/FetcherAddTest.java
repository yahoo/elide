/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.testng.annotations.Test;

/**
 * Test the Add operation.
 */
public class FetcherAddTest extends AbstractPersistentResourceFetcherTest {
    @Test
    public void testCreateRootSingle() throws JsonProcessingException {
        String graphQLRequest = "mutation { book(op: ADD, data: { title: \"Book Numero Dos\" } ) { title } }";
        String expectedResponse = "{\"book\":[{\"title\":\"Book Numero Dos\"}]}";

        assertQueryEquals(graphQLRequest, expectedResponse);
    }

    @Test
    public void testCreateRootCollection() {

    }

    @Test
    public void testCreateNestedSingle() {

    }

    @Test
    public void testCreateNestedCollection() {

    }

}
