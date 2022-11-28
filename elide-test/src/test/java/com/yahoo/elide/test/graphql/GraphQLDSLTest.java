/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.test.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

public class GraphQLDSLTest {

    @Test
    public void verifyBasicRequest() {
        String expected = "{book {edges {node {id title}}}}";
        String actual = GraphQLDSL.document(
                GraphQLDSL.selection(
                        GraphQLDSL.field(
                                "book",
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("id"),
                                        GraphQLDSL.field("title")
                                )
                        )
                )
        ).toQuery();
        assertEquals(expected, actual);
    }

    @Test
    public void verifyFieldAliases() {
        String expected = "{book {edges {node {id shortTitle: title(format: short) longTitle: title(format: long)}}}}";
        String actual = GraphQLDSL.document(
                GraphQLDSL.selection(
                        GraphQLDSL.field(
                                "book",
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("id"),
                                        GraphQLDSL.field("title", "shortTitle", GraphQLDSL.arguments(
                                                GraphQLDSL.argument("format", "short")
                                        )),
                                        GraphQLDSL.field("title", "longTitle", GraphQLDSL.arguments(
                                                GraphQLDSL.argument("format", "long")
                                        ))
                                )
                        )
                )
        ).toQuery();
        assertEquals(expected, actual);
    }

    @Test
    public void verifyMultipleTopLevelEntitiesSelection() {
        String expected = "{book {edges {node {user1SecretField}}} book {edges {node {id title}}}}";
        String actual = GraphQLDSL.document(
                GraphQLDSL.selections(
                        GraphQLDSL.field(
                                "book",
                                GraphQLDSL.selection(
                                        GraphQLDSL.field("user1SecretField")
                                )
                        ),
                        GraphQLDSL.field(
                                "book",
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("id"),
                                        GraphQLDSL.field("title")
                                )
                        )
                )
        ).toQuery();

        assertEquals(expected, actual);
    }

    @Test
    public void verifyRequestWithRelationship() {
        String expected = "{book {edges {node {id title authors {edges {node {name}}}}}}}";
        String actual = GraphQLDSL.document(
                GraphQLDSL.selections(
                        GraphQLDSL.field(
                                "book",
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("id"),
                                        GraphQLDSL.field("title"),
                                        GraphQLDSL.field(
                                                "authors",
                                                GraphQLDSL.selection(
                                                        GraphQLDSL.field("name")
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
        String actual = GraphQLDSL.document(
                GraphQLDSL.selections(
                        GraphQLDSL.field(
                                "book",
                                GraphQLDSL.argument(
                                        GraphQLDSL.argument("sort", "-id", GraphQLDSL.QUOTE_VALUE)
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("id"),
                                        GraphQLDSL.field("title")
                                )
                        )
                )
        ).toQuery();

        assertEquals(expected, actual);
    }

    @Test
    public void verifyRequestWithMultipleStringArguments() {
        String expected = "{book(sort: \"-id\" id: \"5\") {edges {node {id title}}}}";
        String actual = GraphQLDSL.document(
                GraphQLDSL.selections(
                        GraphQLDSL.field(
                                "book",
                                GraphQLDSL.arguments(
                                        GraphQLDSL.argument("sort", "-id", GraphQLDSL.QUOTE_VALUE),
                                        GraphQLDSL.argument("id", "5", GraphQLDSL.QUOTE_VALUE)
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("id"),
                                        GraphQLDSL.field("title")
                                )
                        )
                )
        ).toQuery();

        assertEquals(expected, actual);
    }

    @Test
    public void verifyRequestWithNonStringArguments() {
        String expected = "{book(ids: [1,2] map: {key:\"value\"}) {edges {node {id title}}}}";

        String actual = GraphQLDSL.document(
                GraphQLDSL.selections(
                        GraphQLDSL.field(
                                "book",
                                GraphQLDSL.arguments(
                                        GraphQLDSL.argument("ids", new Long[]{ 1L, 2L }),
                                        GraphQLDSL.argument("map", ImmutableMap.of("key", "value"))
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("id"),
                                        GraphQLDSL.field("title")
                                )
                        )
                )
        ).toQuery();

        assertEquals(expected, actual);
    }

    @Test
    public void verifyRequestWithQuotedArgument() {
        String expected = "{book(desc: \"has \\\"quotes\\\"\") {edges {node {id title}}}}";

        String actual = GraphQLDSL.document(
                GraphQLDSL.selections(
                        GraphQLDSL.field(
                                "book",
                                GraphQLDSL.argument(
                                        GraphQLDSL.argument("desc", "has \"quotes\"", GraphQLDSL.QUOTE_VALUE)
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("id"),
                                        GraphQLDSL.field("title")
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

        String actual = GraphQLDSL.document(
                GraphQLDSL.query(
                        "myQuery",
                        GraphQLDSL.variableDefinitions(
                                GraphQLDSL.variableDefinition("bookId", "[String]")
                        ),
                        GraphQLDSL.selections(
                                GraphQLDSL.field(
                                        "book",
                                        GraphQLDSL.arguments(
                                                GraphQLDSL.argument("ids", "$bookId")
                                        ),
                                        GraphQLDSL.selections(
                                                GraphQLDSL.field("id"),
                                                GraphQLDSL.field("title"),
                                                GraphQLDSL.field(
                                                        "authors",
                                                        GraphQLDSL.selection(
                                                                GraphQLDSL.field("name")
                                                        )
                                                )
                                        )
                                )
                        )
                )
        ).toQuery();

        assertEquals(expected, actual);
    }

    /**************************************** Response DSL. ****************************************/

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

        String actual = GraphQLDSL.document(
                GraphQLDSL.selection(
                        GraphQLDSL.field(
                                "book",
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("id", "1"),
                                        GraphQLDSL.field("title", "My first book"),
                                        GraphQLDSL.field(
                                                "authors",
                                                GraphQLDSL.selection(
                                                        GraphQLDSL.field("name", "Ricky Carmichael")
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

        String actual = GraphQLDSL.document(
                GraphQLDSL.selection(
                        GraphQLDSL.field(
                                "book",
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("id", "3"),
                                        GraphQLDSL.field("title", "Doctor Zhivago"),
                                        GraphQLDSL.field(
                                                "publisher",
                                                GraphQLDSL.selection(
                                                        GraphQLDSL.field("id", "2")
                                                )
                                        )
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("id", "1"),
                                        GraphQLDSL.field("title", "Libro Uno"),
                                        GraphQLDSL.field(
                                                "publisher",
                                                GraphQLDSL.selection(
                                                        GraphQLDSL.field("id", "1")
                                                )
                                        )
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("id", "2"),
                                        GraphQLDSL.field("title", "Libro Dos"),
                                        GraphQLDSL.field(
                                                "publisher",
                                                GraphQLDSL.selection(
                                                        GraphQLDSL.field("id", "1")
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

        String actual = GraphQLDSL.document(
                GraphQLDSL.selection(
                        GraphQLDSL.field(
                                "container",
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("json", jsonField)
                                )
                        )
                )
        ).toResponse();

        assertEquals(expected, actual);
    }
}
