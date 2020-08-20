/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.testhelpers.graphql;

import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.QUOTE_VALUE;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.argument;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.arguments;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.document;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.field;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.query;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.selection;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.selections;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.variableDefinition;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.variableDefinitions;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;

import org.junit.jupiter.api.Test;

public class GraphQLDSLTest {

    @Test
    public void verifyBasicRequest() {
        String expected = "{book {edges {node {id title}}}}";
        String actual = document(
                selection(
                        field(
                                "book",
                                selections(
                                        field("id"),
                                        field("title")
                                )
                        )
                )
        ).toQuery();
        assertEquals(expected, actual);
    }

    @Test
    public void verifyMultipleTopLevelEntitiesSelection() {
        String expected = "{book {edges {node {user1SecretField}}} book {edges {node {id title}}}}";
        String actual = document(
                selections(
                        field(
                                "book",
                                selection(
                                        field("user1SecretField")
                                )
                        ),
                        field(
                                "book",
                                selections(
                                        field("id"),
                                        field("title")
                                )
                        )
                )
        ).toQuery();

        assertEquals(expected, actual);
    }

    @Test
    public void verifyRequestWithRelationship() {
        String expected = "{book {edges {node {id title authors {edges {node {name}}}}}}}";
        String actual = document(
                selections(
                        field(
                                "book",
                                selections(
                                        field("id"),
                                        field("title"),
                                        field(
                                                "authors",
                                                selection(
                                                        field("name")
                                                )
                                        )
                                )
                        )
                )
        ).toQuery();

        assertEquals(expected, actual);
    }

    @Test
    public void verifyRequestWithSingleStringArgument() {
        String expected = "{book(sort: \"-id\") {edges {node {id title}}}}";
        String actual = document(
                selections(
                        field(
                                "book",
                                argument(
                                        argument("sort", "-id", QUOTE_VALUE)
                                ),
                                selections(
                                        field("id"),
                                        field("title")
                                )
                        )
                )
        ).toQuery();

        assertEquals(expected, actual);
    }

    @Test
    public void verifyRequestWithMultipleStringArguments() {
        String expected = "{book(sort: \"-id\" id: \"5\") {edges {node {id title}}}}";
        String actual = document(
                selections(
                        field(
                                "book",
                                arguments(
                                        argument("sort", "-id", QUOTE_VALUE),
                                        argument("id", "5", QUOTE_VALUE)
                                ),
                                selections(
                                        field("id"),
                                        field("title")
                                )
                        )
                )
        ).toQuery();

        assertEquals(expected, actual);
    }

    @Test
    public void verifyRequestWithNonStringArguments() {
        String expected = "{book(ids: [1,2] map: {key:\"value\"}) {edges {node {id title}}}}";

        String actual = document(
                selections(
                        field(
                                "book",
                                arguments(
                                        argument("ids", new Long[]{ 1L, 2L }),
                                        argument("map", ImmutableMap.of("key", "value"))
                                ),
                                selections(
                                        field("id"),
                                        field("title")
                                )
                        )
                )
        ).toQuery();

        assertEquals(expected, actual);
    }

    @Test
    public void verifyRequestWithQuotedArgument() {
        String expected = "{book(desc: \"has \\\"quotes\\\"\") {edges {node {id title}}}}";

        String actual = document(
                selections(
                        field(
                                "book",
                                argument(
                                        argument("desc", "has \"quotes\"", QUOTE_VALUE)
                                ),
                                selections(
                                        field("id"),
                                        field("title")
                                )
                        )
                )
        ).toQuery();

        assertEquals(expected, actual);
    }

    @Test
    public void verifyRequestWithVariable() {
        String expected = "query myQuery($bookId: [String]) {book(ids: $bookId) {edges {node {id title authors {edges"
                + " {node {name}}}}}}}";

        String actual = document(
                query(
                        "myQuery",
                        variableDefinitions(
                                variableDefinition("bookId", "[String]")
                        ),
                        selections(
                                field(
                                        "book",
                                        arguments(
                                                argument("ids", "$bookId")
                                        ),
                                        selections(
                                                field("id"),
                                                field("title"),
                                                field(
                                                        "authors",
                                                        selection(
                                                                field("name")
                                                        )
                                                )
                                        )
                                )
                        )
                )
        ).toQuery();

        assertEquals(expected, actual);
    }

    /**************************************** Response DSL ****************************************/

    @Test
    public void verifyBasicResponse() {
        String expected = "{\"data\":"
                + "{\"book\":"
                + "{\"edges\":"
                + "[{\"node\":"
                + "{\"id\":\"1\","
                + "\"title\":\"My first book\","
                + "\"authors\":{\"edges\":[{\"node\":{"
                + "\"name\":\"Ricky Carmichael\"}}]}}}]}}}";

        String actual = document(
                selection(
                        field(
                                "book",
                                selections(
                                        field("id", "1"),
                                        field("title", "My first book"),
                                        field(
                                                "authors",
                                                selection(
                                                        field("name", "Ricky Carmichael")
                                                )
                                        )
                                )
                        )
                )
        ).toResponse();

        assertEquals(expected, actual);
    }

    @Test
    public void verifyMultipleEntityInstancesResponse() {
        String expected = "{\"data\":{\"book\":{\"edges\":[{\"node\":{\"id\":\"3\",\"title\":\"Doctor Zhivago\","
                + "\"publisher\":{\"edges\":[{\"node\":{\"id\":\"2\"}}]}}},{\"node\":{\"id\":\"1\",\"title\":\"Libro "
                + "Uno\",\"publisher\":{\"edges\":[{\"node\":{\"id\":\"1\"}}]}}},{\"node\":{\"id\":\"2\","
                + "\"title\":\"Libro Dos\",\"publisher\":{\"edges\":[{\"node\":{\"id\":\"1\"}}]}}}]}}}";

        String actual = document(
                selection(
                        field(
                                "book",
                                selections(
                                        field("id", "3"),
                                        field("title", "Doctor Zhivago"),
                                        field(
                                                "publisher",
                                                selection(
                                                        field("id", "2")
                                                )
                                        )
                                ),
                                selections(
                                        field("id", "1"),
                                        field("title", "Libro Uno"),
                                        field(
                                                "publisher",
                                                selection(
                                                        field("id", "1")
                                                )
                                        )
                                ),
                                selections(
                                        field("id", "2"),
                                        field("title", "Libro Dos"),
                                        field(
                                                "publisher",
                                                selection(
                                                        field("id", "1")
                                                )
                                        )
                                )
                        )
                )
        ).toResponse();

        assertEquals(expected, actual);
    }


    @Test
    public void verifyJsonStringField() {
        String jsonField = new Gson().toJson(ImmutableMap.of("key", "value"));
        String jsonNode = new Gson().toJson(ImmutableMap.of("json", jsonField));
        String expected = "{\"data\":{\"container\":{\"edges\":[{\"node\":" + jsonNode + "}]}}}";

        String actual = document(
                selection(
                        field(
                                "container",
                                selections(
                                        field("json", jsonField)
                                )
                        )
                )
        ).toResponse();

        assertEquals(expected, actual);
    }
}
