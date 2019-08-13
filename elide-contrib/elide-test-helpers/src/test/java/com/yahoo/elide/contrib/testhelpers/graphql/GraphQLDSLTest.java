/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.testhelpers.graphql;

import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.argument;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.arguments;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.document;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.entity;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.field;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.responseField;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.selection;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.selections;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.typedOperation;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.stringValue;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.variableDefinition;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.variableDefinitions;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.variableValue;

import com.yahoo.elide.contrib.testhelpers.graphql.elements.TypedOperation;

import org.testng.Assert;
import org.testng.annotations.Test;

public class GraphQLDSLTest {

    @Test
    public void verifyBasicRequest() {
        String expected = "{book {edges {node {id title}}}}";
        String actual = document(
                selection(
                        entity(
                                "book",
                                selections(
                                        field("id"),
                                        field("title")
                                )
                        )
                )
        ).toQuery();

        Assert.assertEquals(actual, expected);
    }

    @Test
    public void verifyMultipleTopLevelEntitiesSelection() {
        String expected = "{book {edges {node {user1SecretField}}} book {edges {node {id title}}}}";
        String actual = document(
                selections(
                        entity(
                                "book",
                                selection(
                                        field("user1SecretField")
                                )
                        ),
                        entity(
                                "book",
                                selections(
                                        field("id"),
                                        field("title")
                                )
                        )
                )
        ).toQuery();

        Assert.assertEquals(actual, expected);
    }

    @Test
    public void verifyRequestWithRelationship() {
        String expected = "{book {edges {node {id title authors {edges {node {name}}}}}}}";
        String actual = document(
                selections(
                        entity(
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

        Assert.assertEquals(actual, expected);
    }

    @Test
    public void verifyRequestWithSingleStringArgument() {
        String expected = "{book(sort: \"-id\") {edges {node {id title}}}}";
        String actual = document(
                selections(
                        entity(
                                "book",
                                argument(
                                        argument(
                                                "sort",
                                                stringValue("-id")
                                        )
                                ),
                                selections(
                                        field("id"),
                                        field("title")
                                )
                        )
                )
        ).toQuery();

        Assert.assertEquals(actual, expected);
    }

    @Test
    public void verifyRequestWithMultipleStringArguments() {
        String expected = "{book(sort: \"-id\" id: \"5\") {edges {node {id title}}}}";
        String actual = document(
                selections(
                        entity(
                                "book",
                                arguments(
                                        argument(
                                                "sort",
                                                stringValue("-id")
                                        ),
                                        argument(
                                                "id",
                                                stringValue("5")
                                        )
                                ),
                                selections(
                                        field("id"),
                                        field("title")
                                )
                        )
                )
        ).toQuery();

        Assert.assertEquals(actual, expected);
    }

    @Test
    public void verifyRequestWithVariable(){
        String expected = "query myQuery($bookId: [String]) {book(ids: $bookId) {edges {node {id title authors {edges" +
                " {node {name}}}}}}}";

        String actual = document(
                typedOperation(
                        TypedOperation.OperationType.QUERY,
                        "myQuery",
                        variableDefinitions(
                                variableDefinition("bookId", "[String]")
                        ),
                        selections(
                                entity(
                                        "book",
                                        arguments(
                                                argument("ids", variableValue("bookId"))
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

        Assert.assertEquals(actual, expected);
    }

    /**************************************** Response DSL ****************************************/

    @Test
    public void verifyBasicResponse() {
        String expected = "{\"data\":" +
                "{\"book\":" +
                "{\"edges\":" +
                "[{\"node\":" +
                "{\"id\":\"1\"," +
                "\"title\":\"My first book\"," +
                "\"authors\":{\"edges\":[{\"node\":{" +
                "\"name\":\"Ricky Carmichael\"}}]}}}]}}}";

        String actual = document(
                selection(
                        responseField(
                                "book",
                                selections(
                                        responseField("id", "1"),
                                        responseField("title", "My first book"),
                                        responseField(
                                                "authors",
                                                selection(
                                                        responseField("name", "Ricky Carmichael")
                                                )
                                        )
                                )
                        )
                )
        ).toResponse();

        Assert.assertEquals(actual, expected);
    }

    @Test
    public void verifyMultipleEntityInstancesResponse() {
        String expected = "{\"data\":{\"book\":{\"edges\":[{\"node\":{\"id\":\"3\",\"title\":\"Doctor Zhivago\"," +
                "\"publisher\":{\"edges\":[{\"node\":{\"id\":\"2\"}}]}}},{\"node\":{\"id\":\"1\",\"title\":\"Libro " +
                "Uno\",\"publisher\":{\"edges\":[{\"node\":{\"id\":\"1\"}}]}}},{\"node\":{\"id\":\"2\"," +
                "\"title\":\"Libro Dos\",\"publisher\":{\"edges\":[{\"node\":{\"id\":\"1\"}}]}}}]}}}";

        String actual = document(
                selection(
                        responseField(
                                "book",
                                selections(
                                        responseField("id", "3"),
                                        responseField("title", "Doctor Zhivago"),
                                        responseField(
                                                "publisher",
                                                selection(
                                                        responseField("id", "2")
                                                )
                                        )
                                ),
                                selections(
                                        responseField("id", "1"),
                                        responseField("title", "Libro Uno"),
                                        responseField(
                                                "publisher",
                                                selection(
                                                        responseField("id", "1")
                                                )
                                        )
                                ),
                                selections(
                                        responseField("id", "2"),
                                        responseField("title", "Libro Dos"),
                                        responseField(
                                                "publisher",
                                                selection(
                                                        responseField("id", "1")
                                                )
                                        )
                                )
                        )
                )
        ).toResponse();

        Assert.assertEquals(actual, expected);
    }
}
