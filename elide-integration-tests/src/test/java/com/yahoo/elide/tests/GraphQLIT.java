/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.tests;

import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.argument;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.arguments;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.arrayValueWithVariable;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.document;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.entity;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.enumValue;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.field;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.objectValueWithVariable;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.responseField;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.responseSelections;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.selection;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.selections;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.stringValue;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.typedOperation;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.variableDefinition;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.variableDefinitions;

import com.yahoo.elide.contrib.testhelpers.graphql.elements.TypedOperation;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.HttpStatus;
import com.yahoo.elide.initialization.IntegrationTest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.ValidatableResponse;

import org.junit.jupiter.api.BeforeEach;
import org.testng.Assert;
import org.testng.annotations.Test;

import example.Author;
import example.Book;
import groovy.json.internal.IO;
import lombok.Builder;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BinaryOperator;

import javax.ws.rs.core.MediaType;

/**
 * GraphQL integration tests.
 * <p>
 * CAUTION: Test beans, such as {@link Book} and {@link Author} MUST NOT be decorated by {@link Builder}, otherwise test
 * will fail at {@link DataStoreTransaction#createNewObject(Class)}, because the {@link Builder} hides the no-args
 * constructor.
 */
public class GraphQLIT extends IntegrationTest {

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    @Test(priority = 1)
    public void createBookAndAuthor() throws IOException {
        Book book = new Book();
        book.setId(1);
        book.setTitle("1984");

        Author author = new Author();
        author.setId(1L);
        author.setName("George Orwell");

        String graphQLQuery = document(
                typedOperation(
                        TypedOperation.OperationType.MUTATION,
                        selection(
                                entity(
                                        "book",
                                        arguments(
                                                argument("op", enumValue("UPSERT")),
                                                argument("data", objectValueWithVariable(book))
                                        ),
                                        selections(
                                                field("id"),
                                                field("title"),
                                                field(
                                                        "authors",
                                                        arguments(
                                                                argument("op", enumValue("UPSERT")),
                                                                argument("data", objectValueWithVariable(author))
                                                        ),
                                                        selections(
                                                                field("id"),
                                                                field("name")
                                                        )
                                                )
                                        )
                                )
                        )
                )
        ).toQuery();

        String expectedResponse = document(
                selection(
                        responseField(
                                "book",
                                selections(
                                        responseField("id", "1"),
                                        responseField("title", "1984"),
                                        responseField(
                                                "authors",
                                                selections(
                                                        responseField("id", "1"),
                                                        responseField("name", "George Orwell")
                                                )
                                        )
                                )
                        )
                )
        ).toResponse();

        runQueryWithExpectedResult(graphQLQuery, expectedResponse);
    }

    @Test(priority = 2)
    public void createNewBooksAndAuthor() throws IOException {
        Book book = new Book();
        book.setId(2);
        book.setTitle("$bookName");

        Author author = new Author();
        author.setId(2L);
        author.setName("$authorName");

        String graphQLRequest = document(
                typedOperation(
                        TypedOperation.OperationType.MUTATION,
                        "myMutation",
                        variableDefinitions(
                                variableDefinition("bookName", "String"),
                                variableDefinition("authorName", "String")
                        ),
                        selection(
                                entity(
                                        "book",
                                        arguments(
                                                argument("op", enumValue("UPSERT")),
                                                argument("data", objectValueWithVariable(book))
                                        ),
                                        selections(
                                                field("id"),
                                                field("title"),
                                                field(
                                                        "authors",
                                                        arguments(
                                                                argument("op", enumValue("UPSERT")),
                                                                argument("data", objectValueWithVariable(author))
                                                        ),
                                                        selections(
                                                                field("id"),
                                                                field("name")
                                                        )
                                                )
                                        )
                                )
                        )
                )
        ).toQuery();

        String expected = document(
                selection(
                        responseField(
                                "book",
                                selections(
                                        responseField("id", "2"),
                                        responseField("title", "Grapes of Wrath"),
                                        responseField(
                                                "authors",
                                                selections(
                                                        responseField("id", "2"),
                                                        responseField("name", "John Setinbeck")
                                                )
                                        )
                                )
                        )
                )
        ).toResponse();

        Map<String, Object> variables = new HashMap<>();
        variables.put("bookName", "Grapes of Wrath");
        variables.put("authorName", "John Setinbeck");

        runQueryWithExpectedResult(graphQLRequest, variables, expected);
    }

    @Test(priority = 3)
    public void fetchCollection() throws IOException {
        String graphQLRequest = document(
               selection(
                       entity(
                               "book",
                               selections(
                                       field("id"),
                                       field("title"),
                                       field(
                                               "authors",
                                               selections(
                                                       field("id"),
                                                       field("name")
                                               )
                                       )
                               )
                       )
               )
        ).toQuery();

        String expected = document(
                selections(
                        responseField(
                                "book",
                                selections(
                                        responseField("id", "1"),
                                        responseField("title", "1984"),
                                        responseField(
                                                "authors",
                                                selections(
                                                        responseField("id", "1"),
                                                        responseField("name", "George Orwell")
                                                )
                                        )
                                ),
                                selections(
                                        responseField("id", "2"),
                                        responseField("title", "Grapes of Wrath"),
                                        responseField(
                                                "authors",
                                                selections(
                                                        responseField("id", "2"),
                                                        responseField("name", "John Setinbeck")
                                                )
                                        )
                                )
                        )
                )
        ).toResponse();

        runQueryWithExpectedResult(graphQLRequest, expected);
    }


    @Test(priority = 4)
    public void fetchRootSingle() throws IOException {
        String graphQLRequest = document(
                selection(
                        entity(
                                "book",
                                argument(
                                        argument(
                                                "ids",
                                                arrayValueWithVariable(
                                                        stringValue("1")
                                                )
                                        )
                                ),
                                selections(
                                        field("id"),
                                        field("title")
                                )
                        )
                )
        ).toQuery();

        String expectedResponse = document(
                selection(
                        responseField(
                                "book",
                                selections(
                                        responseField("id", "1"),
                                        responseField("title", "1984")
                                )
                        )
                )
        ).toResponse();

        runQueryWithExpectedResult(graphQLRequest, expectedResponse);
    }

    @Test(priority = 5)
    public void runUpdateAndFetchDifferentTransactionsBatch() throws IOException {
        Book book = new Book();
        book.setId(999);
        book.setTitle("my book created in batch!");

        String graphQLRequest1 = document(
                typedOperation(
                        TypedOperation.OperationType.MUTATION,
                        selection(
                                entity(
                                        "book",
                                        arguments(
                                                argument("op", enumValue("UPSERT")),
                                                argument("data", objectValueWithVariable(book))
                                        ),
                                        selections(
                                                field("id"),
                                                field("title")
                                        )
                                )
                        )
                )
        ).toQuery();

        String graphQLRequest2 = document(
                selection(
                        entity(
                                "book",
                                argument(argument("ids", stringValue("4"))),
                                selections(
                                        field("id"),
                                        field("title")
                                )
                        )
                )
        ).toQuery();

        String expectedResponse = document(
                selection(
                        responseField(
                                "book",
                                selections(
                                        responseField("id", "4"),
                                        responseField("title", "my book created in batch!")
                                )
                        )
                ),
                selection(
                        responseField(
                                "book",
                                selections(
                                        responseField("id", "4"),
                                        responseField("title", "my book created in batch!")
                                )
                        )
                )
        ).toResponse();

        compareJsonObject(
                runQuery(toJsonArray(toJsonNode(graphQLRequest1), toJsonNode(graphQLRequest2))),
                expectedResponse
        );
    }

    @Test(priority = 6)
    public void runMultipleRequestsSameTransaction() throws IOException {
        // This test demonstrates that multiple roots can be manipulated within a _single_ transaction
        String graphQLRequest = document(
                selections(
                        entity(
                                "book",
                                argument(
                                        argument(
                                                "ids",
                                                arrayValueWithVariable(stringValue("1"))
                                        )
                                ),
                                selections(
                                        field("id"),
                                        field("title"),
                                        field(
                                                "authors",
                                                selections(
                                                        field("id"),
                                                        field("name")
                                                )
                                        )
                                )
                        ),
                        entity(
                                "author",
                                selections(
                                        field("id"),
                                        field("name")
                                )
                        )
                )
        ).toQuery();

        String expectedResponse = document(
                responseSelections(
                        responseField(
                                "book",
                                selections(
                                        responseField("id", "1"),
                                        responseField("title", "1984"),
                                        responseField(
                                                "authors",
                                                selections(
                                                        responseField("id", "1"),
                                                        responseField("name", "George Orwell")
                                                )
                                        )
                                )
                        ),
                        responseField(
                                "author",
                                selections(
                                        responseField("id", "1"),
                                        responseField("name", "George Orwell")
                                ),
                                selections(
                                        responseField("id", "2"),
                                        responseField("name", "John Setinbeck")
                                )
                        )
                )
        ).toResponse();

        runQueryWithExpectedResult(graphQLRequest, expectedResponse);
    }

    @Test(priority = 7)
    public void runMultipleRequestsSameTransactionMutation() throws IOException {
        // This test demonstrates that multiple roots can be manipulated within a _single_ transaction
        // and results are consistent across a mutation.
        Author author = new Author();
        author.setId(3L);
        author.setName("Stephen King");

        String graphQLRequest = document(
                typedOperation(
                        TypedOperation.OperationType.MUTATION,
                        selections(
                                entity(
                                        "book",
                                        argument(
                                                argument(
                                                        "ids",
                                                        arrayValueWithVariable(stringValue("1"))
                                                )
                                        ),
                                        selections(
                                                field("id"),
                                                field("title"),
                                                field(
                                                        "authors",
                                                        arguments(
                                                                argument("op", enumValue("UPSERT")),
                                                                argument("data", objectValueWithVariable(author))
                                                        ),
                                                        selections(
                                                                field("id"),
                                                                field("name")
                                                        )
                                                )
                                        )
                                ),
                                entity(
                                        "author",
                                        selections(
                                                field("id"),
                                                field("name")
                                        )
                                )
                        )
                )
        ).toQuery();

        String expectedResponse = document(
                responseSelections(
                        responseField(
                                "book",
                                selections(
                                        responseField("id", "1"),
                                        responseField("title", "1984"),
                                        responseField(
                                                "authors",
                                                selections(
                                                        responseField("id", "6"),
                                                        responseField("name", "Stephen King")
                                                )
                                        )
                                )
                        ),
                        responseField(
                                "author",
                                selections(
                                        responseField("id", "1"),
                                        responseField("name", "George Orwell")
                                ),
                                selections(
                                        responseField("id", "2"),
                                        responseField("name", "John Setinbeck")
                                ),
                                selections(
                                        responseField("id", "4"),
                                        responseField("name", "Stephen King")
                                )
                        )
                )
        ).toResponse();

        runQueryWithExpectedResult(graphQLRequest, expectedResponse);
    }

    @Test(priority = 6)
    public void runMultipleRequestsSameTransactionWithAliases() throws IOException {
        // This test demonstrates that multiple roots can be manipulated within a _single_ transaction
        String graphQLRequest = document(
                selections(
                        entity(
                                "firstAuthorCollection",
                                "author",
                                selections(
                                        field("id"),
                                        field("name")
                                )
                        ),
                        entity(
                                "secondAuthorCollection",
                                "author",
                                selections(
                                        field("id"),
                                        field("name")
                                )
                        )
                )
        ).toQuery();

        String expectedResponse = document(
                responseSelections(
                        responseField(
                                "firstAuthorCollection",
                                selections(
                                        responseField("id", "1"),
                                        responseField("name", "George Orwell")
                                ),
                                selections(
                                        responseField("id", "2"),
                                        responseField("name", "John Setinbeck")
                                )
                        ),
                        responseField(
                                "secondAuthorCollection",
                                selections(
                                        responseField("id", "1"),
                                        responseField("name", "George Orwell")
                                ),
                                selections(
                                        responseField("id", "2"),
                                        responseField("name", "John Setinbeck")
                                )
                        )
                )
        ).toResponse();

        runQueryWithExpectedResult(graphQLRequest, expectedResponse);
    }

    private void runQueryWithExpectedResult(
            String graphQLQuery,
            Map<String, Object> variables,
            String expected
    ) throws IOException {
        compareJsonObject(runQuery(graphQLQuery, variables), expected);
    }

    private void runQueryWithExpectedResult(String graphQLQuery, String expected) throws IOException {
        runQueryWithExpectedResult(graphQLQuery, null, expected);
    }

    private void compareJsonObject(ValidatableResponse response, String expected) throws IOException {
        JsonNode responseNode = JSON_MAPPER.readTree(response.extract().body().asString());
        JsonNode expectedNode = JSON_MAPPER.readTree(expected);
        Assert.assertEquals(responseNode, expectedNode);
    }

    private ValidatableResponse runQuery(String query, Map<String, Object> variables) throws IOException {
        return runQuery(toJsonQuery(query, variables));
    }

    private ValidatableResponse runQuery(String query) {
        ValidatableResponse response = RestAssured.given()
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(query)
                .post("/graphQL")
                .then()
                .statusCode(HttpStatus.SC_OK);

        return RestAssured.given()
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(query)
                .post("/graphQL")
                .then()
                .statusCode(HttpStatus.SC_OK);
    }

    private String toJsonArray(JsonNode... nodes) throws IOException {
        ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
        for(JsonNode node : nodes) {
            arrayNode.add(node);
        }
        return JSON_MAPPER.writeValueAsString(arrayNode);
    }

    private String toJsonQuery(String query, Map<String, Object> variables) throws IOException {
        return JSON_MAPPER.writeValueAsString(toJsonNode(query, variables));
    }

    private JsonNode toJsonNode(String query) {
        return toJsonNode(query, null);
    }

    private JsonNode toJsonNode(String query, Map<String, Object> variables) {
        ObjectNode graphqlNode = JsonNodeFactory.instance.objectNode();
        graphqlNode.put("query", query);
        if (variables != null) {
            graphqlNode.set("variables", JSON_MAPPER.valueToTree(variables));
        }
        return graphqlNode;
    }


}
