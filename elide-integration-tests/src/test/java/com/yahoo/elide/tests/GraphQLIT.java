/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.tests;

import static com.yahoo.elide.core.dictionary.EntityDictionary.NO_VERSION;
import static com.yahoo.elide.test.graphql.GraphQLDSL.UNQUOTED_VALUE;
import static com.yahoo.elide.test.graphql.GraphQLDSL.argument;
import static com.yahoo.elide.test.graphql.GraphQLDSL.arguments;
import static com.yahoo.elide.test.graphql.GraphQLDSL.document;
import static com.yahoo.elide.test.graphql.GraphQLDSL.field;
import static com.yahoo.elide.test.graphql.GraphQLDSL.mutation;
import static com.yahoo.elide.test.graphql.GraphQLDSL.query;
import static com.yahoo.elide.test.graphql.GraphQLDSL.selection;
import static com.yahoo.elide.test.graphql.GraphQLDSL.selections;
import static com.yahoo.elide.test.graphql.GraphQLDSL.variableDefinition;
import static com.yahoo.elide.test.graphql.GraphQLDSL.variableDefinitions;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;

import com.yahoo.elide.core.exceptions.HttpStatus;
import com.yahoo.elide.initialization.GraphQLIntegrationTest;
import com.yahoo.elide.test.graphql.VariableFieldSerializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import example.Currency;
import example.Price;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MediaType;

/**
 * GraphQL integration tests.
 */
public class GraphQLIT extends GraphQLIntegrationTest {

    private static class Book {
        @Getter
        @Setter
        private long id;

        @Getter
        @Setter
        @JsonSerialize(using = VariableFieldSerializer.class, as = String.class)
        private String title;

        @Getter
        @Setter
        private Price price;

        private Collection<example.Author> authors = new ArrayList<>();
    }

    private static class Author {
        @Getter
        @Setter
        private Long id;

        @Getter
        @Setter
        @JsonSerialize(using = VariableFieldSerializer.class, as = String.class)
        private String name;
    }

    @BeforeEach
    public void createBookAndAuthor() throws IOException {
        // before each test, create a new book and a new author
        Book book = new Book();
        book.setId(1);
        book.setTitle("1984");
        Price price = new Price();
        price.setTotal(BigDecimal.valueOf(10.0));
        price.setCurrency(new Currency("USD"));
        book.setPrice(price);

        Author author = new Author();
        author.setId(1L);
        author.setName("George Orwell");

        String graphQLQuery = document(
                mutation(
                        selection(
                                field(
                                        "book",
                                        arguments(
                                                argument("op", "UPSERT"),
                                                argument("data", book)
                                        ),
                                        selections(
                                                field("id"),
                                                field("title"),
                                                field(
                                                        "authors",
                                                        arguments(
                                                                argument("op", "UPSERT"),
                                                                argument("data", author)
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
                        field(
                                "book",
                                selections(
                                        field("id", "1"),
                                        field("title", "1984"),
                                        field(
                                                "authors",
                                                selections(
                                                        field("id", "1"),
                                                        field("name", "George Orwell")
                                                )
                                        )
                                )
                        )
                )
        ).toResponse();

        runQueryWithExpectedResult(graphQLQuery, expectedResponse);
    }

    @Test
    public void createWithVariables() throws IOException {
        // create a second book using variable
        Book book = new Book();
        book.setId(2);
        book.setTitle("$bookName");

        Author author = new Author();
        author.setId(2L);
        author.setName("$authorName");

        String graphQLRequest = document(
                mutation(
                        "myMutation",
                        variableDefinitions(
                                variableDefinition("bookName", "String"),
                                variableDefinition("authorName", "String")
                        ),
                        selection(
                                field(
                                        "book",
                                        arguments(
                                                argument("op", "UPSERT"),
                                                argument("data", book, UNQUOTED_VALUE)
                                        ),
                                        selections(
                                                field("id"),
                                                field("title"),
                                                field(
                                                        "authors",
                                                        arguments(
                                                                argument("op", "UPSERT"),
                                                                argument("data", author, UNQUOTED_VALUE)
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
                        field(
                                "book",
                                selections(
                                        field("id", "2"),
                                        field("title", "Grapes of Wrath"),
                                        field(
                                                "authors",
                                                selections(
                                                        field("id", "2"),
                                                        field("name", "John Setinbeck")
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

    @Test
    public void fetchCollection() throws IOException {
        // create a second book
        createWithVariables();

        String graphQLRequest = document(
                selection(
                        field(
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
                        field(
                                "book",
                                selections(
                                        field("id", "1"),
                                        field("title", "1984"),
                                        field(
                                                "authors",
                                                selections(
                                                        field("id", "1"),
                                                        field("name", "George Orwell")
                                                )
                                        )
                                ),
                                selections(
                                        field("id", "2"),
                                        field("title", "Grapes of Wrath"),
                                        field(
                                                "authors",
                                                selections(
                                                        field("id", "2"),
                                                        field("name", "John Setinbeck")
                                                )
                                        )
                                )
                        )
                )
        ).toResponse();

        runQueryWithExpectedResult(graphQLRequest, expected);
    }

    @Test
    public void testFilterByComplexAttribute() throws IOException {
        String graphQLRequest = document(
                selection(
                        field(
                                "book",
                                arguments(
                                        argument("filter", "\"price.total>=5\"")
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
                        field(
                                "book",
                                selections(
                                        field("id", "1"),
                                        field("title", "1984")
                                )
                        )
                )
        ).toResponse();

        runQueryWithExpectedResult(graphQLRequest, expectedResponse);

        graphQLRequest = document(
                selection(
                        field(
                                "book",
                                arguments(
                                        argument("filter", "\"price.total<=5\"")
                                ),
                                selections(
                                        field("id"),
                                        field("title")
                                )
                        )
                )
        ).toQuery();

        expectedResponse = "{\"data\": {\"book\": {\"edges\": []}}}";

        runQueryWithExpectedResult(graphQLRequest, expectedResponse);
    }

    @Test
    public void testFilterByNestedComplexAttribute() throws IOException {
        String graphQLRequest = document(
                selection(
                        field(
                                "book",
                                arguments(
                                        argument("filter", "\"price.currency.isoCode==USD\"")
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
                        field(
                                "book",
                                selections(
                                        field("id", "1"),
                                        field("title", "1984")
                                )
                        )
                )
        ).toResponse();

        runQueryWithExpectedResult(graphQLRequest, expectedResponse);

        graphQLRequest = document(
                selection(
                        field(
                                "book",
                                arguments(
                                        argument("filter", "\"price.currency.isoCode==ABC\"")
                                ),
                                selections(
                                        field("id"),
                                        field("title")
                                )
                        )
                )
        ).toQuery();

        expectedResponse = "{\"data\": {\"book\": {\"edges\": []}}}";

        runQueryWithExpectedResult(graphQLRequest, expectedResponse);
    }

    @Test
    public void testInvalidFetch() throws IOException {
        Book book = new Book();

        String graphQLRequest = document(
                selection(
                        field(
                                "book",
                                arguments(
                                        argument("op", "FETCH"),
                                        argument("data", book)
                                ),
                                selections(
                                        field("id"),
                                        field("title")
                                )
                        )
                )
        ).toQuery();

        String expected = "{\"data\":{\"book\":null},\"errors\":[{\"message\":\"Exception while fetching data "
                + "(/book) : FETCH must not include data\","
                + "\"locations\":[{\"line\":1,\"column\":2}],\"path\":[\"book\"],"
                + "\"extensions\":{\"classification\":\"DataFetchingException\"}}]}";

        runQueryWithExpectedResult(graphQLRequest, expected);
    }

    @Test
    public void fetchRootSingle() throws IOException {
        String graphQLRequest = document(
                selection(
                        field(
                                "book",
                                argument(
                                        argument(
                                                "ids",
                                                Arrays.asList("1")
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
                        field(
                                "book",
                                selections(
                                        field("id", "1"),
                                        field("title", "1984")
                                )
                        )
                )
        ).toResponse();

        runQueryWithExpectedResult(graphQLRequest, expectedResponse);
    }

    @Test
    public void runUpdateAndFetchDifferentTransactionsBatch() throws IOException {
        Book book = new Book();
        book.setId(2);
        book.setTitle("my book created in batch!");

        String graphQLRequest1 = document(
                mutation(
                        selection(
                                field(
                                        "book",
                                        arguments(
                                                argument("op", "UPSERT"),
                                                argument("data", book)
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
                        field(
                                "book",
                                argument(argument("ids", "\"2\"")),
                                selections(
                                        field("id"),
                                        field("title")
                                )
                        )
                )
        ).toQuery();

        String expectedResponse = document(
                selection(
                        field(
                                "book",
                                selections(
                                        field("id", "2"),
                                        field("title", "my book created in batch!")
                                )
                        )
                ),
                selections(
                        field(
                                "book",
                                selections(
                                        field("id", "2"),
                                        field("title", "my book created in batch!")
                                )
                        )
                )
        ).toResponse();

        compareJsonObject(
                runQuery(toJsonArray(toJsonNode(graphQLRequest1), toJsonNode(graphQLRequest2)), NO_VERSION),
                expectedResponse
        );
    }

    @Test
    public void runMultipleRequestsSameTransactionWithAliases() throws IOException {
        // This test demonstrates that multiple roots can be manipulated within a _single_ transaction
        String graphQLRequest = document(
                selections(
                        field(
                                "firstAuthorCollection",
                                "author",
                                selections(
                                        field("id"),
                                        field("name")
                                )
                        ),
                        field(
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
                selections(
                        field(
                                "firstAuthorCollection",
                                selections(
                                        field("id", "1"),
                                        field("name", "George Orwell")
                                )
                        ),
                        field(
                                "secondAuthorCollection",
                                selections(
                                        field("id", "1"),
                                        field("name", "George Orwell")
                                )
                        )
                )
        ).toResponse();

        runQueryWithExpectedResult(graphQLRequest, expectedResponse);
    }

    @Tag("skipInMemory") //Elide doesn't support to-many filter joins in memory yet.
    @ParameterizedTest
    @ValueSource(strings = {
            "\"books.title==\\\"1984\\\"\"",
            "\"books.id=isnull=false\"",
            "\"books.title=in=(\\\"1984\\\")\""})
    public void runManyToManyFilter(String filter) throws IOException {
        String graphQLRequest = document(
            query(
                selections(
                        field(
                                "author",
                                arguments(
                                        argument("filter", filter)
                                ),
                                selections(
                                        field("id"),
                                        field("name"),
                                        field(
                                                "books",
                                                selections(
                                                        field("id"),
                                                        field("title")
                                                )
                                        )
                                        )
                        )
                )
            )
        ).toQuery();

        String expectedResponse = document(
            selection(
                    field(
                            "author",
                            selections(
                                    field("id", "1"),
                                    field("name", "George Orwell"),
                                    field(
                                            "books",
                                            selections(
                                                    field("id", "1"),
                                                    field("title", "1984")
                                            )
                                    )
                            )
                    )
            )
        ).toResponse();

        runQueryWithExpectedResult(graphQLRequest, expectedResponse);
    }

    @Test
    public void testTypeIntrospection() throws Exception {
        String graphQLRequest = "{"
                + "__type(name: \"Book\") {"
                + "   name"
                + "   fields {"
                + "     name"
                + "   }"
                + "}"
                + "}";

        String query = toJsonQuery(graphQLRequest, new HashMap<>());

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .body(query)
            .post("/graphQL")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .body("data.__type.fields.name", containsInAnyOrder("id", "awards", "chapterCount",
                    "editorName", "genre", "language", "publishDate", "title", "authors", "chapters", "price",
                    "editor", "publisher"));
    }

    @Test
    public void testVersionedTypeIntrospection() throws Exception {
        String graphQLRequest = "{"
                + "__type(name: \"Book\") {"
                + "   name"
                + "   fields {"
                + "     name"
                + "   }"
                + "}"
                + "}";

        String query = toJsonQuery(graphQLRequest, new HashMap<>());

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header("ApiVersion", "1.0")
                .body(query)
                .post("/graphQL")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("data.__type.fields.name", containsInAnyOrder("id", "name", "publishDate"));
    }

    @Test
    @Tag("skipInMemory") //Skipping because storage for in-memory store is
    //broken out by class instead of a common underlying database table.
    public void fetchCollectionVersioned() throws IOException {

        String graphQLRequest = document(
                selection(
                        field(
                                "book",
                                selections(
                                        field("id"),
                                        field("name")
                                )
                        )
                )
        ).toQuery();

        String expected = document(
                selections(
                        field(
                                "book",
                                selections(
                                        field("id", "1"),
                                        field("name", "1984")
                                )
                        )
                )
        ).toResponse();

        runQueryWithExpectedResult(graphQLRequest, null, expected, "1.0");
    }

    @Test
    public void testInvalidApiVersion() throws IOException {

        String graphQLRequest = document(
                selection(
                        field(
                                "book",
                                selections(
                                        field("id"),
                                        field("name")
                                )
                        )
                )
        ).toQuery();

        String expected = "{\"errors\":[{\"message\":\"Invalid operation: Invalid API Version\"}]}";

        String query = toJsonQuery(graphQLRequest, new HashMap<>());

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header("ApiVersion", "2.0")
                .body(query)
                .post("/graphQL")
                .then()
                .body(equalTo(expected))
                .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    @Test
    public void testMissingVersionedModel() throws IOException {

        String graphQLRequest = document(
                selection(
                        field(
                                "parent",
                                selections(
                                        field("id")
                                )
                        )
                )
        ).toQuery();

        String expected = "{\"errors\":[{\"message\":\"Bad Request Body&#39;Unknown entity {parent}.&#39;\"}]}";

        runQueryWithExpectedResult(graphQLRequest, null, expected, "1.0");
    }

    private String toJsonArray(JsonNode... nodes) throws IOException {
        ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
        for (JsonNode node : nodes) {
            arrayNode.add(node);
        }
        return mapper.writeValueAsString(arrayNode);
    }
}
