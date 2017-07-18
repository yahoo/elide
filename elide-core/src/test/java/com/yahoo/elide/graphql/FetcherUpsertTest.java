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
public class FetcherUpsertTest extends PersistentResourceFetcherTest {
    /* ==================== */
    /* CREATING NEW OBJECTS */
    /* ==================== */
    @Test
    public void testCreateRootSingle() throws JsonProcessingException {
        String graphQLRequest =
                "mutation { " +
                    "book(op: UPSERT, data: {title: \"Book Numero Dos\"} ) { " +
                        "title " +
                    "} " +
                "}";
        String expectedResponse =
                "{" +
                    "\"book\":[{" +
                        "\"title\":\"Book Numero Dos\"" +
                    "}]" +
                "}";

        assertQueryEquals(graphQLRequest, expectedResponse);
    }

    @Test
    public void testCreateRootCollection() throws JsonProcessingException {
        String graphQLRequest =
                "mutation { " +
                    "book(op: UPSERT, data: [{title: \"Book Numero Dos\"},{title:\"Book Numero Tres\"}] ) { " +
                        "title " +
                    "} " +
                "}";
        String expectedResponse =
                "{" +
                    "\"book\":[{" +
                        "\"title\":\"Book Numero Dos\"" +
                    "},{" +
                        "\"title\":\"Book Numero Tres\"" +
                    "}]" +
                "}";

        assertQueryEquals(graphQLRequest, expectedResponse);
    }

    @Test
    public void testCreateNestedSingle() throws JsonProcessingException {
        String graphQLRequest =
                "mutation { " +
                    "author(ids: [\"1\"]) { " +
                        "id " +
                        "books(op: UPSERT, data: {title: \"Book Numero Dos\"}) { " +
                            "title " +
                        "} " +
                    "} " +
                "} ";
        String expectedResponse =
                "{" +
                    "\"author\":[{" +
                        "\"id\":\"1\"," +
                        "\"books\":[{" +
                            "\"title\":\"Book Numero Dos\"" +
                        "}]" +
                    "}]" +
                "}";

        assertQueryEquals(graphQLRequest, expectedResponse);
    }

    @Test
    public void testCreateNestedCollection() throws JsonProcessingException {
        String graphQLRequest =
                "mutation { " +
                    "author(ids: [\"1\"]) { " +
                        "id " +
                        "books(op: UPSERT, data: [{title: \"Book Numero Dos\"}, {title: \"Book Numero Tres\"}]) { " +
                            "title " +
                        "} " +
                    "} " +
                "} ";
        String expectedResponse =
                "{" +
                    "\"author\":[{" +
                        "\"id\":\"1\"," +
                        "\"books\":[{" +
                            "\"title\":\"Book Numero Dos\"" +
                        "},{" +
                            "\"title\":\"Book Numero Tres\"" +
                        "}]" +
                    "}]" +
                "}";

        assertQueryEquals(graphQLRequest, expectedResponse);
    }

    /* ========================= */
    /* UPDATING EXISTING OBJECTS */
    /* ========================= */
    @Test
    public void testRootSingleWithId() throws JsonProcessingException {
        String graphQLRequest =
                "mutation { " +
                    "author(op:UPSERT, data: {id: \"1\", name: \"abc\" }) { " +
                        "id, " +
                        "name " +
                    "} " +
                "}";
        String expectedResponse =
                "{" +
                    "\"author\":[{" +
                        "\"id\":\"1\"," +
                        "\"name\":\"abc\"" +
                    "}]" +
                "}";

        assertQueryEquals(graphQLRequest, expectedResponse);

        // Ensure we don't accidentally null values
        graphQLRequest =
                "{ " +
                    "author(id: \"1\") { " +
                        "id, " +
                        "type, " +
                        "books { " +
                            "id " +
                        "} " +
                    "} " +
                "}";
        expectedResponse =
                "{" +
                    "\"author\":[{" +
                        "\"id\":\"1\"," +
                        "\"type\":\"EXCLUSIVE\"," +
                        "\"books\":[{" +
                            "\"id\":\"1\"" +
                        "},{" +
                            "\"id\":\"2\"" +
                        "}]" +
                    "}]" +
                "}";

        assertQueryEquals(graphQLRequest, expectedResponse);
    }

    @Test
    public void testRootSingleWithList() throws JsonProcessingException {
        String graphQLRequest =
                "mutation { " +
                    "book(op:UPSERT, data: [{id: \"1\", title: \"abc\"}, {id: \"2\", title: \"xyz\"}]) { " +
                        "id, " +
                        "title " +
                    "} " +
                "}";
        String expectedResponse =
                "{" +
                    "\"book\":[{" +
                        "\"id\":\"1\"," +
                        "\"title\":\"abc\"" +
                    "},{" +
                        "\"id\":\"2\"," +
                        "\"title\":\"xyz\"" +
                    "}]" +
                "}";

        assertQueryEquals(graphQLRequest, expectedResponse);
    }

    @Test
    public void testRootCollectionMixedIds() throws JsonProcessingException {
        // Update 1, create for id 42, create new book with title "abc"
        String graphQLRequest =
                "mutation { " +
                    "book(op:UPSERT, data: [{id: \"1\", title: \"my id\"}, {id: \"42\", title: \"xyz\"}, {title: \"abc\"}]) { " +
                        "id, " +
                        "title " +
                    "} " +
                "}";
        String expectedResponse =
                "{" +
                    "\"book\":[{" +
                        "\"id\":\"0\"," +
                        "\"title\":\"abc\"" +
                    "},{" +
                        "\"id\":\"1\"," +
                        "\"title\":\"my id\"" +
                    "},{" +
                        "\"id\":\"42\"," +
                        "\"title\":\"xyz\"" +
                    "}]" +
                "}";

        assertQueryEquals(graphQLRequest, expectedResponse);
    }

    @Test
    public void testNestedSingleUpdate() throws JsonProcessingException {
        String graphQLRequest =
                "mutation { " +
                    "author(id: \"1\") { " +
                        "id " +
                        "books(op:UPSERT, data: {id: \"1\", title: \"abc\"}) { " +
                            "id, " +
                            "title " +
                        "} " +
                    "} " +
                "}";
        String expectedResponse =
                "{" +
                    "\"author\":[{" +
                        "\"id\":\"1\"," +
                        "\"books\":[{" +
                            "\"id\":\"1\"," +
                            "\"title\":\"abc\"" +
                        "}]" +
                    "}]" +
                "}";

        assertQueryEquals(graphQLRequest, expectedResponse);
    }

    @Test
    public void testNestedCollection() {
        String graphQLRequest =
                "mutation { " +
                    "author(id: \"1\") { " +
                        "id " +
                        "books(op:UPSERT, data: [{id: \"1\", title: \"abc\"}, {id: \"2\", title: \"xyz\"}]) { " +
                            "id, " +
                            "title " +
                        "} " +
                    "} " +
                "}";
        String expectedResponse =
                "{" +
                    "\"author\":[{" +
                        "\"id\":\"1\"," +
                        "\"books\":[{" +
                            "\"id\":\"1\"," +
                            "\"title\":\"abc\"" +
                        "},{" +
                            "\"id\":\"2\"," +
                            "\"title\":\"xyz\"" +
                        "}]" +
                    "}]" +
                "}";

        assertQueryEquals(graphQLRequest, expectedResponse);
    }
}
