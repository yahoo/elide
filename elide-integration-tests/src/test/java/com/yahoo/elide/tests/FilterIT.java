/*
 * Copyright 2018, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.tests;

import com.yahoo.elide.core.HttpStatus;
import com.yahoo.elide.initialization.IntegrationTest;
import com.yahoo.elide.utils.JsonParser;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.restassured.RestAssured;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class FilterIT extends IntegrationTest {

    private static final String JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION = "application/vnd.api+json; ext=jsonpatch";

    private final JsonParser jsonParser = new JsonParser();
    private final ObjectMapper mapper = new ObjectMapper();

    private JsonNode books;
    private String asimovId;
    private JsonNode asimovBooks;
    private String thomasHarrisId;
    private JsonNode thomasHarrisBooks;
    private String nullNedId;
    private JsonNode nullNedBooks;
    private String orsonCardId;
    private String hemingwayId;

    private Set<Integer> bookIds = new HashSet<>();
    private Set<Integer> authorIds = new HashSet<>();

    private String getAuthorId(JsonNode author, String name) {
        if (author.get("attributes").get("name").asText().equals(name)) {
            return author.get("id").asText();
        }
        return null;
    }


    @BeforeEach
    void setup() throws IOException {

        given()
                .contentType(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .accept(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .body(jsonParser.getJson("/FilterIT/book_author_publisher_patch1.json"))
                .patch("/")
                .then()
                .statusCode(HttpStatus.SC_OK);

        given()
                .contentType(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .accept(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .body(jsonParser.getJson("/FilterIT/book_author_publisher_patch2.json"))
                .patch("/")
                .then()
                .statusCode(HttpStatus.SC_OK);

        given()
                .contentType(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .accept(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .body(jsonParser.getJson("/FilterIT/book_author_publisher_patch3.json"))
                .patch("/")
                .then()
                .statusCode(HttpStatus.SC_OK);

        given()
                .contentType(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .accept(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .body(jsonParser.getJson("/FilterIT/book_author_publisher_patch4.json"))
                .patch("/")
                .then()
                .statusCode(HttpStatus.SC_OK);

        given()
                .contentType(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .accept(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .body(jsonParser.getJson("/FilterIT/book_author_publisher_patch5.json"))
                .patch("/")
                .then()
                .statusCode(HttpStatus.SC_OK);

        books = mapper.readTree(RestAssured.get("/book").asString());
        JsonNode authors = mapper.readTree(RestAssured.get("/author").asString());

        for (JsonNode author : authors.get("data")) {
            authorIds.add(author.get("id").asInt());
            hemingwayId = (getAuthorId(author, "Ernest Hemingway") == null ? hemingwayId : getAuthorId(author, "Ernest Hemingway"));
            asimovId = (getAuthorId(author, "Isaac Asimov") == null ? asimovId : getAuthorId(author, "Isaac Asimov"));
            nullNedId = (getAuthorId(author, "Null Ned") == null ? nullNedId : getAuthorId(author, "Null Ned"));
            orsonCardId = (getAuthorId(author, "Orson Scott Card") == null ? orsonCardId : getAuthorId(author, "Orson Scott Card"));
            thomasHarrisId = (getAuthorId(author, "Thomas Harris") == null ? thomasHarrisId : getAuthorId(author, "Thomas Harris"));
        }

        for (JsonNode book : books.get("data")) {
            bookIds.add(book.get("id").asInt());
        }

        assertNotNull(asimovId);
        assertNotNull(nullNedId);
        assertNotNull(thomasHarrisId);

        asimovBooks = mapper.readTree(RestAssured.get(String.format("/author/%s/books", asimovId)).asString());
        nullNedBooks = mapper.readTree(RestAssured.get(String.format("/author/%s/books", nullNedId)).asString());
        thomasHarrisBooks = mapper.readTree(RestAssured.get(String.format("/author/%s/books", thomasHarrisId)).asString());
    }

    @Test
    void testRootFilterInvalidField() {
        /* Test Default */
        given()
                .get("/book?filter[book.name]=Ignored")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST);

        /* Test RSQL typed */
        given()
                .get("/book?filter[book]=name==Ignored")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST);

        /* Test RSQL global */
        given()
                .get("/book?filter=name==Ignored")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    @Test
    void testRootFilterInvalidEntity() {
        /* Test Default */
        given()
                .get("/book?filter[bank.title]=Ignored")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST);

        /* Test RSQL typed */
        given()
                .get("/book?filter[bank]=title==Ignored")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    @Test
    void testRootInvalidOperator() {
        /* Test Default */
        given()
                .get("/book?filter[book.title][invalid]=Ignored")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST);

        /* Test RSQL Typed */
        given()
                .get("/book?filter[book]=title=invalid=Ignored")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST);

        /* Test RSQL Global */
        given()
                .get("/book?filter=title=invalid=Ignored")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    @Test
    void testFilterInvalidField() {
        /* Test Default */
        given()
                .get("/author/3/book?filter[book.name]=Ignored")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST);

        /* Test RSQL Typed */
        given()
                .get("/author/3/book?filter[book]=name==Ignored")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    @Test
    void testFilterInvalidEntity() {
        /* Test Default */
        given()
                .get("/author/3/book?filter[bank.title]=Ignored")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST);

        /* Test RSQL Typed */
        given()
                .get("/author/3/book?filter[bank]=title==Ignored")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    @Test
    void testInvalidOperator() {
        /* Test Default */
        given()
                .get("/author/3/book?filter[book.title][invalid]=Ignored")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST);

        /* Test RSQL Typed */
        given()
                .get("/author/3/book?filter[book]=title=invalid=Ignored")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    @Test
    void testRootFilterImplicitSingle() throws IOException {
        int scienceFictionBookCount = 0;
        for (JsonNode node : books.get("data")) {
            if (node.get("attributes").get("genre").asText().equalsIgnoreCase("Science Fiction")) {
                scienceFictionBookCount += 1;
            }
        }

        assertTrue(scienceFictionBookCount > 0);

        /* Test Default */
        JsonNode scienceFictionBooks = mapper.readTree(
                RestAssured
                        .get("/book?filter[book.genre]=Science Fiction")
                        .asString()
        );

        assertEquals(scienceFictionBookCount, scienceFictionBooks.get("data").size());

        /* Test RSQL Typed */
        scienceFictionBooks = mapper.readTree(
                RestAssured
                        .get("/book?filter[book]=genre=='Science Fiction'")
                        .asString()
        );

        assertEquals(scienceFictionBookCount, scienceFictionBooks.get("data").size());

        /* Test RSQL Global */
        scienceFictionBooks = mapper.readTree(
                RestAssured
                        .get("/book?filter=genre=='Science Fiction'")
                        .asString()
        );

        assertEquals(scienceFictionBookCount, scienceFictionBooks.get("data").size());
    }

    @Test
    void testRootFilterInSingle() throws IOException {
        int literaryFictionBookCount = 0;
        for (JsonNode node : books.get("data")) {
            if (node.get("attributes").get("genre").asText().equalsIgnoreCase("Literary Fiction")) {
                literaryFictionBookCount += 1;
            }
        }

        assertTrue(literaryFictionBookCount > 0);

        /* Test Default */
        JsonNode literaryFictionBooks = mapper.readTree(
                RestAssured
                        .get("/book?filter[book.genre][in]=Literary Fiction")
                        .asString()
        );

        assertEquals(literaryFictionBookCount, literaryFictionBooks.get("data").size());

        /* Test RSQL Typed */
        literaryFictionBooks = mapper.readTree(
                RestAssured
                        .get("/book?filter[book]=genre=in='Literary Fiction'")
                        .asString()
        );

        assertEquals(literaryFictionBookCount, literaryFictionBooks.get("data").size());

        /* Test RSQL Global */
        literaryFictionBooks = mapper.readTree(
                RestAssured
                        .get("/book?filter=genre=in='Literary Fiction'")
                        .asString()
        );

        assertEquals(literaryFictionBookCount, literaryFictionBooks.get("data").size());
    }

    @Test
    void testRootFilterNotInSingle() throws IOException {
        int nonLiteraryFictionBookCount = 0;
        for (JsonNode node : books.get("data")) {
            if (!node.get("attributes").get("genre").isNull()
                    && !node.get("attributes").get("genre").asText().equalsIgnoreCase("Literary Fiction")) {
                nonLiteraryFictionBookCount += 1;
            }
        }

        assertTrue(nonLiteraryFictionBookCount > 0);

        /* Test Default */
        JsonNode nonLiteraryFictionBooks = mapper.readTree(
                RestAssured
                        .get("/book?filter[book.genre][not]=Literary Fiction")
                        .asString()
        );

        assertEquals(nonLiteraryFictionBookCount, nonLiteraryFictionBooks.get("data").size());

        /* Test RSQL typed */
        nonLiteraryFictionBooks = mapper.readTree(
                RestAssured.get("/book?filter[book]=genre!='Literary Fiction'").asString());

        assertEquals(nonLiteraryFictionBookCount, nonLiteraryFictionBooks.get("data").size());

        /* Test RSQL global */
        nonLiteraryFictionBooks = mapper.readTree(
                RestAssured
                        .get("/book?filter=genre!='Literary Fiction'")
                        .asString()
        );

        assertEquals(nonLiteraryFictionBookCount, nonLiteraryFictionBooks.get("data").size());
    }

    @Test
    void testRootFilterNotInMultiple() throws IOException {
        int nonFictionBookCount = 0;
        for (JsonNode node : books.get("data")) {
            if (!node.get("attributes").get("genre").isNull()
                    && !node.get("attributes").get("genre").asText().equalsIgnoreCase("Literary Fiction")
                    && !node.get("attributes").get("genre").asText().equalsIgnoreCase("Science Fiction")) {
                nonFictionBookCount += 1;
            }
        }

        assertTrue(nonFictionBookCount > 0);

        /* Test default */
        JsonNode nonFictionBooks = mapper.readTree(
                RestAssured
                        .get("/book?filter[book.genre][not]=Literary Fiction,Science Fiction")
                        .asString()
        );

        assertEquals(nonFictionBookCount, nonFictionBooks.get("data").size());

        /* Test RSQL typed */
        nonFictionBooks = mapper.readTree(
                RestAssured
                        .get("/book?filter[book]=genre=out=('Literary Fiction','Science Fiction')")
                        .asString()
        );

        assertEquals(nonFictionBookCount, nonFictionBooks.get("data").size());

        /* Test RSQL global */
        nonFictionBooks = mapper.readTree(
                RestAssured
                        .get("/book?filter=genre=out=('Literary Fiction','Science Fiction')")
                        .asString()
        );

        assertEquals(nonFictionBookCount, nonFictionBooks.get("data").size());
    }

    @Test
    void testRootFilterInMultipleSingle() throws IOException {
        int literaryAndScienceFictionBookCount = 0;
        for (JsonNode node : books.get("data")) {
            if (node.get("attributes").get("genre").asText().equalsIgnoreCase("Literary Fiction")
                    || node.get("attributes").get("genre").asText().equalsIgnoreCase("Science Fiction")) {
                literaryAndScienceFictionBookCount += 1;
            }
        }

        assertTrue(literaryAndScienceFictionBookCount > 0);

        /* Test Default */
        JsonNode literaryAndScienceFictionBooks = mapper.readTree(
                RestAssured
                        .get("/book?filter[book.genre][in]=Literary Fiction,Science Fiction")
                        .asString()
        );

        assertEquals(literaryAndScienceFictionBookCount, literaryAndScienceFictionBooks.get("data").size());

        /* Test RSQL typed */
        literaryAndScienceFictionBooks = mapper.readTree(
                RestAssured
                        .get("/book?filter[book]=genre=in=('Literary Fiction','Science Fiction')")
                        .asString()
        );

        assertEquals(literaryAndScienceFictionBookCount, literaryAndScienceFictionBooks.get("data").size());

        /* Test RSQL global */
        literaryAndScienceFictionBooks = mapper.readTree(
                RestAssured
                        .get("/book?filter=genre=in=('Literary Fiction','Science Fiction')")
                        .asString()
        );

        assertEquals(literaryAndScienceFictionBookCount, literaryAndScienceFictionBooks.get("data").size());
    }

    @Test
    void testRootFilterPostfix() throws IOException {
        int genreEndsWithFictionBookCount = 0;
        for (JsonNode node : books.get("data")) {
            if (node.get("attributes").get("genre").asText().toLowerCase().endsWith("fiction")) {
                genreEndsWithFictionBookCount += 1;
            }
        }

        assertTrue(genreEndsWithFictionBookCount > 0);

        /* Test Default */
        JsonNode genreEndsWithFictionBooks = mapper.readTree(
                RestAssured
                        .get("/book?filter[book.genre][postfix]=Fiction")
                        .asString()
        );

        assertEquals(genreEndsWithFictionBookCount, genreEndsWithFictionBooks.get("data").size());

        /* Test RSQL Typed */
        genreEndsWithFictionBooks = mapper.readTree(
                RestAssured
                        .get("/book?filter[book]=genre==*Fiction")
                        .asString()
        );

        assertEquals(genreEndsWithFictionBookCount, genreEndsWithFictionBooks.get("data").size());

        /* Test RSQL Global */
        genreEndsWithFictionBooks = mapper.readTree(
                RestAssured
                        .get("/book?filter=genre==*Fiction")
                        .asString()
        );

        assertEquals(genreEndsWithFictionBookCount, genreEndsWithFictionBooks.get("data").size());
    }

    @Test
    void testRootFilterPrefix() throws IOException {
        int titleStartsWithTheBookCount = 0;
        for (JsonNode node : books.get("data")) {
            if (node.get("attributes").get("title").asText().toLowerCase().startsWith("the")) {
                titleStartsWithTheBookCount += 1;
            }
        }

        assertTrue(titleStartsWithTheBookCount > 0);

        /* Test Default */
        JsonNode titleStartsWithTheBooks = mapper.readTree(
                RestAssured
                        .get("/book?filter[book.title][prefix]=The")
                        .asString()
        );

        assertEquals(titleStartsWithTheBookCount, titleStartsWithTheBooks.get("data").size());

        /* Test RSQL Typed */
        titleStartsWithTheBooks = mapper.readTree(
                RestAssured
                        .get("/book?filter[book]=title==The*")
                        .asString()
        );

        assertEquals(titleStartsWithTheBookCount, titleStartsWithTheBooks.get("data").size());

        /* Test RSQL Global  */
        titleStartsWithTheBooks = mapper.readTree(
                RestAssured
                        .get("/book?filter=title==The*")
                        .asString()
        );

        assertEquals(titleStartsWithTheBookCount, titleStartsWithTheBooks.get("data").size());
    }

    @Test
    void testRootFilterPrefixWithSpecialChars() throws IOException {
        int titleStartsWithTheBookCount = 0;
        for (JsonNode node : books.get("data")) {
            if (node.get("attributes").get("title").asText().toLowerCase().startsWith("i'm")) {
                titleStartsWithTheBookCount += 1;
            }
        }

        assertTrue(titleStartsWithTheBookCount > 0);

        /* Test Default */
        JsonNode titleStartsWithTheBooks = mapper.readTree(
                RestAssured
                        .get("/book?filter[book.title][prefix]=i'm")
                        .asString()
        );

        assertEquals(titleStartsWithTheBookCount, titleStartsWithTheBooks.get("data").size());

        /* Test RSQL Typed */
        titleStartsWithTheBooks = mapper.readTree(
                RestAssured
                        .get("/book?filter[book]=title=='i\\'m*'")
                        .asString()
        );

        assertEquals(titleStartsWithTheBookCount, titleStartsWithTheBooks.get("data").size());

        /* Test RSQL Global */
        titleStartsWithTheBooks = mapper.readTree(
                RestAssured
                        .get("/book?filter=title=='i\\'m*'")
                        .asString()
        );

        assertEquals(titleStartsWithTheBookCount, titleStartsWithTheBooks.get("data").size());
    }

    @Test
    void testRootFilterInfix() throws IOException {
        int titleContainsTheBookCount = 0;
        for (JsonNode node : books.get("data")) {
            if (node.get("attributes").get("title").asText().toLowerCase().contains("the")) {
                titleContainsTheBookCount += 1;
            }
        }

        assertTrue(titleContainsTheBookCount > 0);

        /* Test Default */
        JsonNode titleContainsTheBooks = mapper.readTree(
                RestAssured
                        .get("/book?filter[book.title][infix]=the")
                        .asString()
        );

        assertEquals(titleContainsTheBookCount, titleContainsTheBooks.get("data").size());

        /* Test RSQL Typed */
        titleContainsTheBooks = mapper.readTree(
                RestAssured
                        .get("/book?filter[book]=title==*the*")
                        .asString()
        );

        assertEquals(titleContainsTheBookCount, titleContainsTheBooks.get("data").size());

        /* Test RSQL Global */
        titleContainsTheBooks = mapper.readTree(
                RestAssured
                        .get("/book?filter=title==*the*")
                        .asString()
        );

        assertEquals(titleContainsTheBookCount, titleContainsTheBooks.get("data").size());
    }

    @Test
    void testRootFilterWithInclude() throws IOException {
        Set<String> authorIdsOfLiteraryFiction = new HashSet<>();

        for (JsonNode book : books.get("data")) {
            if (book.get("attributes").get("genre").asText().equals("Literary Fiction")) {
                for (JsonNode author : book.get("relationships").get("authors").get("data")) {
                    authorIdsOfLiteraryFiction.add(author.get("id").asText());
                }
            }
        }

        assertTrue(authorIdsOfLiteraryFiction.size() > 0);

        /* Test Default */
        JsonNode result = mapper.readTree(
                RestAssured
                        .get("/book?include=authors&filter[book.genre]=Literary Fiction")
                        .asString()
        );

        for (JsonNode author : result.get("included")) {
            assertTrue(authorIdsOfLiteraryFiction.contains(author.get("id").asText()));
        }

        /* Test RSQL Typed */
        result = mapper.readTree(
                RestAssured
                        .get("/book?include=authors&filter[book]=genre=='Literary Fiction'")
                        .asString()
        );

        for (JsonNode author : result.get("included")) {
            assertTrue(authorIdsOfLiteraryFiction.contains(author.get("id").asText()));
        }

        /* Test RSQL Global */
        result = mapper.readTree(
                RestAssured
                        .get("/book?include=authors&filter=genre=='Literary Fiction'")
                        .asString()
        );

        for (JsonNode author : result.get("included")) {
            assertTrue(authorIdsOfLiteraryFiction.contains(author.get("id").asText()));
        }
    }

    @Test
    void testRootFilterIsNull() throws IOException {
        Set<JsonNode> bookIdsWithNullGenre = new HashSet<>();

        for (JsonNode book : books.get("data")) {
            if (book.get("attributes").get("genre").isNull()) {
                bookIdsWithNullGenre.add(book.get("id"));
            }
        }

        assertTrue(bookIdsWithNullGenre.size() > 0);

        /* Test Default */
        JsonNode result = mapper.readTree(
                RestAssured
                        .get("/book?filter[book.genre][isnull]")
                        .asString()
        );

        assertEquals(result.get("data").size(), bookIdsWithNullGenre.size());

        for (JsonNode book : result.get("data")) {
            assertTrue(book.get("attributes").get("genre").isNull());
            assertTrue(bookIdsWithNullGenre.contains(book.get("id")));
        }

        /* Test RSQL Typed */
        /* param = true */
        result = mapper.readTree(
                RestAssured
                        .get("book?filter[book]=genre=isnull=true")
                        .asString()
        );

        assertEquals(result.get("data").size(), bookIdsWithNullGenre.size());

        for (JsonNode book : result.get("data")) {
            assertTrue(book.get("attributes").get("genre").isNull());
            assertTrue(bookIdsWithNullGenre.contains(book.get("id")));
        }

        /* param = 1 */
        result = mapper.readTree(
                RestAssured
                        .get("book?filter[book]=genre=isnull=1")
                        .asString()
        );

        assertEquals(result.get("data").size(), bookIdsWithNullGenre.size());

        for (JsonNode book : result.get("data")) {
            assertTrue(book.get("attributes").get("genre").isNull());
            assertTrue(bookIdsWithNullGenre.contains(book.get("id")));
        }

        /* Test RSQL Global */
        /* param = true */
        result = mapper.readTree(
                RestAssured
                        .get("book?filter=genre=isnull=true")
                        .asString()
        );

        assertEquals(result.get("data").size(), bookIdsWithNullGenre.size());

        for (JsonNode book : result.get("data")) {
            assertTrue(book.get("attributes").get("genre").isNull());
            assertTrue(bookIdsWithNullGenre.contains(book.get("id")));
        }

        /* param = 1 */
        result = mapper.readTree(
                RestAssured
                        .get("book?filter=genre=isnull=1")
                        .asString()
        );

        assertEquals(result.get("data").size(), bookIdsWithNullGenre.size());

        for (JsonNode book : result.get("data")) {
            assertTrue(book.get("attributes").get("genre").isNull());
            assertTrue(bookIdsWithNullGenre.contains(book.get("id")));
        }
    }

    @Test
    void testRootFilterIsNotNull() throws IOException {
        Set<JsonNode> bookIdsWithNonNullGenre = new HashSet<>();

        for (JsonNode book : books.get("data")) {
            if (!book.get("attributes").get("genre").isNull()) {
                bookIdsWithNonNullGenre.add(book.get("id"));
            }
        }

        assertTrue(bookIdsWithNonNullGenre.size() > 0);

        /* Test Default */
        JsonNode result = mapper.readTree(
                RestAssured
                        .get("/book?filter[book.genre][notnull]")
                        .asString()
        );

        assertEquals(result.get("data").size(), bookIdsWithNonNullGenre.size());

        for (JsonNode book : result.get("data")) {
            assertFalse(book.get("attributes").get("genre").isNull());
            assertTrue(bookIdsWithNonNullGenre.contains(book.get("id")));
        }

        /* Test RSQL Typed */
        /* param = false */
        result = mapper.readTree(
                RestAssured
                        .get("book?filter[book]=genre=isnull=false")
                        .asString()
        );

        assertEquals(result.get("data").size(), bookIdsWithNonNullGenre.size());

        for (JsonNode book : result.get("data")) {
            assertFalse(book.get("attributes").get("genre").isNull());
            assertTrue(bookIdsWithNonNullGenre.contains(book.get("id")));
        }

        /* param = 0 */
        result = mapper.readTree(
                RestAssured
                        .get("book?filter[book]=genre=isnull=0")
                        .asString()
        );

        assertEquals(result.get("data").size(), bookIdsWithNonNullGenre.size());

        for (JsonNode book : result.get("data")) {
            assertFalse(book.get("attributes").get("genre").isNull());
            assertTrue(bookIdsWithNonNullGenre.contains(book.get("id")));
        }

        /* Test RSQL Global */
        /* param = false */
        result = mapper.readTree(
                RestAssured
                        .get("book?filter=genre=isnull=false")
                        .asString()
        );

        assertEquals(result.get("data").size(), bookIdsWithNonNullGenre.size());

        for (JsonNode book : result.get("data")) {
            assertFalse(book.get("attributes").get("genre").isNull());
            assertTrue(bookIdsWithNonNullGenre.contains(book.get("id")));
        }

        /* param = 0 */
        result = mapper.readTree(
                RestAssured
                        .get("book?filter=genre=isnull=0")
                        .asString()
        );

        assertEquals(result.get("data").size(), bookIdsWithNonNullGenre.size());

        for (JsonNode book : result.get("data")) {
            assertFalse(book.get("attributes").get("genre").isNull());
            assertTrue(bookIdsWithNonNullGenre.contains(book.get("id")));
        }
    }

    @Test
    void testNonRootFilterImplicitSingle() throws IOException {
        int asimovScienceFictionBookCount = 0;
        for (JsonNode node : asimovBooks.get("data")) {
            if (node.get("attributes").get("genre").asText().equals("Science Fiction")) {
                asimovScienceFictionBookCount += 1;
            }
        }

        assertTrue(asimovScienceFictionBookCount > 0);

        /* Test Default */
        JsonNode asimovScienceFictionBooks = mapper.readTree(
                RestAssured
                        .get(String.format("/author/%s/books?filter[book.genre]=Science Fiction", asimovId))
                        .asString()
        );

        assertEquals(asimovScienceFictionBookCount, asimovScienceFictionBooks.get("data").size());

        /* Test RSQL Typed */
        asimovScienceFictionBooks = mapper.readTree(
                RestAssured
                        .get(String.format("/author/%s/books?filter[book]=genre=='Science Fiction'", asimovId))
                        .asString()
        );

        assertEquals(asimovScienceFictionBookCount, asimovScienceFictionBooks.get("data").size());
    }

    @Test
    void testNonRootFilterInSingle() throws IOException {
        int asimovHistoryBookCount = 0;
        for (JsonNode node : asimovBooks.get("data")) {
            if (node.get("attributes").get("genre").asText().equals("History")) {
                asimovHistoryBookCount += 1;
            }
        }

        assertTrue(asimovHistoryBookCount > 0);

        /* Test Default */
        JsonNode asimovHistoryBooks = mapper.readTree(
                RestAssured
                        .get(String.format("/author/%s/books?filter[book.genre]=History", asimovId))
                        .asString()
        );

        assertEquals(asimovHistoryBookCount, asimovHistoryBooks.get("data").size());

        /* Test RSQL Typed */
        asimovHistoryBooks = mapper.readTree(
                RestAssured
                        .get(String.format("/author/%s/books?filter[book]=genre==History", asimovId))
                        .asString()
        );

        assertEquals(asimovHistoryBookCount, asimovHistoryBooks.get("data").size());
    }

    @Test
    void testNonRootFilterNotInSingle() throws IOException {
        int nonHistoryBookCount = 0;
        for (JsonNode node : asimovBooks.get("data")) {
            if (!node.get("attributes").get("genre").isNull()
                    && !node.get("attributes").get("genre").asText().equals("History")) {
                nonHistoryBookCount += 1;
            }
        }

        assertTrue(nonHistoryBookCount > 0);

        /* Test Default */
        JsonNode nonHistoryBooks = mapper.readTree(
                RestAssured
                        .get(String.format("/author/%s/books?filter[book.genre][not]=History", asimovId))
                        .asString()
        );

        assertEquals(nonHistoryBookCount, nonHistoryBooks.get("data").size());

        /* Test RSQL Typed */
        nonHistoryBooks = mapper.readTree(
                RestAssured
                        .get(String.format("/author/%s/books?filter[book]=genre!=History", asimovId))
                        .asString()
        );

        assertEquals(nonHistoryBookCount, nonHistoryBooks.get("data").size());
    }

    @Test
    void testNonRootFilterPostfix() throws IOException {
        int genreEndsWithFictionBookCount = 0;
        for (JsonNode node : asimovBooks.get("data")) {
            if (node.get("attributes").get("genre").asText().endsWith("Fiction")) {
                genreEndsWithFictionBookCount += 1;
            }
        }

        assertTrue(genreEndsWithFictionBookCount > 0);

        /* Test Default */
        JsonNode genreEndsWithFictionBooks = mapper.readTree(
                RestAssured
                        .get(String.format("/author/%s/books?filter[book.genre][postfix]=Fiction", asimovId))
                        .asString()
        );

        assertEquals(genreEndsWithFictionBookCount, genreEndsWithFictionBooks.get("data").size());

        /* Test RSQL Typed */
        genreEndsWithFictionBooks = mapper.readTree(
                RestAssured
                        .get(String.format("/author/%s/books?filter[book]=genre==*Fiction", asimovId))
                        .asString()
        );

        assertEquals(genreEndsWithFictionBookCount, genreEndsWithFictionBooks.get("data").size());
    }

    @Test
    void testNonRootFilterPostfixInsensitive() throws IOException {
        int editorEdBooks = 0;
        for (JsonNode node : nullNedBooks.get("data")) {
            if (node.get("attributes").get("editorName").asText().endsWith("d")) {
                editorEdBooks += 1;
            }
        }

        assertTrue(editorEdBooks > 0);

        /* Test Default */
        JsonNode editorNameEndsWithd = mapper.readTree(
                RestAssured
                        .get(String.format("/author/%s/books?filter[book.editorName][postfix]=D", nullNedId))
                        .asString()
        );

        assertEquals(0, editorNameEndsWithd.get("data").size());

        editorNameEndsWithd = mapper.readTree(
                RestAssured
                        .get(String.format("/author/%s/books?filter[book.editorName][postfixi]=D", nullNedId))
                        .asString()
        );

        assertEquals(editorEdBooks, editorNameEndsWithd.get("data").size());

        /* Test RSQL Typed */
        editorNameEndsWithd = mapper.readTree(
                RestAssured
                        .get(String.format("/author/%s/books?filter[book]=editorName==*D", nullNedId))
                        .asString()
        );

        assertEquals(editorEdBooks, editorNameEndsWithd.get("data").size());
    }

    @Test
    void testNonRootFilterPrefixInsensitive() throws IOException {
        int editorEdBooks = 0;
        for (JsonNode node : nullNedBooks.get("data")) {
            if (node.get("attributes").get("editorName").asText().startsWith("E")) {
                editorEdBooks += 1;
            }
        }

        assertTrue(editorEdBooks > 0);

        /* Test Default */
        JsonNode editorNameStartsWithE = mapper.readTree(
                RestAssured
                        .get(String.format("/author/%s/books?filter[book.editorName][prefix]=e", nullNedId))
                        .asString()
        );

        assertEquals(0, editorNameStartsWithE.get("data").size());

        editorNameStartsWithE = mapper.readTree(
                RestAssured
                        .get(String.format("/author/%s/books?filter[book.editorName][prefixi]=e", nullNedId))
                        .asString()
        );

        assertEquals(editorEdBooks, editorNameStartsWithE.get("data").size());

        /* Test RSQL Typed */
        editorNameStartsWithE = mapper.readTree(
                RestAssured
                        .get(String.format("/author/%s/books?filter[book]=editorName==e*", nullNedId))
                        .asString()
        );

        assertEquals(editorEdBooks, editorNameStartsWithE.get("data").size());
    }

    @Test
    void testNonRootFilterInfixInsensitive() throws IOException {
        int editorEditBooks = 0;
        for (JsonNode node : nullNedBooks.get("data")) {
            if (node.get("attributes").get("editorName").asText().contains("Ed")) {
                editorEditBooks += 1;
            }
        }

        assertTrue(editorEditBooks > 0);

        /* Test Default */
        JsonNode editorNameContainsEd = mapper.readTree(
                RestAssured
                        .get(String.format("/author/%s/books?filter[book.editorName][infix]=eD", nullNedId))
                        .asString()
        );

        assertEquals(0, editorNameContainsEd.get("data").size());

        editorNameContainsEd = mapper.readTree(
                RestAssured
                        .get(String.format("/author/%s/books?filter[book.editorName][infixi]=eD", nullNedId))
                        .asString()
        );

        assertEquals(editorEditBooks, editorNameContainsEd.get("data").size());

        /* Test RSQL Typed */
        editorNameContainsEd = mapper.readTree(
                RestAssured
                        .get(String.format("/author/%s/books?filter[book]=editorName==*eD*", nullNedId))
                        .asString()
        );

        assertEquals(editorEditBooks, editorNameContainsEd.get("data").size());
    }

    @Test
    void testNonRootFilterPrefix() throws IOException {
        int titleStartsWithTheBookCount = 0;
        for (JsonNode node : asimovBooks.get("data")) {
            if (node.get("attributes").get("title").asText().startsWith("The")) {
                titleStartsWithTheBookCount += 1;
            }
        }

        assertTrue(titleStartsWithTheBookCount > 0);

        /* Test Default */
        JsonNode titleStartsWithTheBooks = mapper.readTree(
                RestAssured
                        .get(String.format("/author/%s/books?filter[book.title][prefix]=The", asimovId))
                        .asString()
        );

        assertEquals(titleStartsWithTheBookCount, titleStartsWithTheBooks.get("data").size());

        /* Test RSQL Typed */
        titleStartsWithTheBooks = mapper.readTree(
                RestAssured
                        .get(String.format("/author/%s/books?filter[book]=title==The*", asimovId))
                        .asString()
        );

        assertEquals(titleStartsWithTheBookCount, titleStartsWithTheBooks.get("data").size());
    }

    @Test
    void testNonRootFilterPrefixWithSpecialChars() throws IOException {
        int titleStartsWithTheBookCount = 0;
        for (JsonNode node : thomasHarrisBooks.get("data")) {
            if (node.get("attributes").get("title").asText().startsWith("I'm")) {
                titleStartsWithTheBookCount += 1;
            }
        }

        assertTrue(titleStartsWithTheBookCount > 0);

        /* Test Default */
        JsonNode titleStartsWithTheBooks = mapper.readTree(
                RestAssured
                        .get(String.format("/author/%s/books?filter[book.title][prefix]=I'm", thomasHarrisId))
                        .asString()
        );

        assertEquals(titleStartsWithTheBookCount, titleStartsWithTheBooks.get("data").size());

        /* Test RSQL Typed */
        titleStartsWithTheBooks = mapper.readTree(
                RestAssured
                        .get(String.format("/author/%s/books?filter[book]=title=='I\\'m*'", thomasHarrisId))
                        .asString()
        );

        assertEquals(titleStartsWithTheBookCount, titleStartsWithTheBooks.get("data").size());
    }

    @Test
    void testNonRootFilterInfix() throws IOException {
        int titleContainsTheBookCount = 0;
        for (JsonNode node : asimovBooks.get("data")) {
            if (node.get("attributes").get("title").asText().toLowerCase().contains("the")) {
                titleContainsTheBookCount += 1;
            }
        }

        assertTrue(titleContainsTheBookCount > 0);

        /* Test Default */
        JsonNode titleContainsTheBooks = mapper.readTree(
                RestAssured.get(String.format("/author/%s/books?filter[book.title][infix]=the", asimovId)).asString());

        assertEquals(titleContainsTheBookCount, titleContainsTheBooks.get("data").size());

        /* Test RSQL Typed */
        titleContainsTheBooks = mapper.readTree(
                RestAssured.get(String.format("/author/%s/books?filter[book]=title==*the*", asimovId)).asString());

        assertEquals(titleContainsTheBookCount, titleContainsTheBooks.get("data").size());
    }

    @Test
    void testNonRootFilterWithInclude() throws IOException {
        Set<String> authorIdsOfScienceFiction = new HashSet<>();

        for (JsonNode book : asimovBooks.get("data")) {
            if (book.get("attributes").get("genre").asText().equals("Science Fiction")) {
                for (JsonNode author : book.get("relationships").get("authors").get("data")) {
                    authorIdsOfScienceFiction.add(author.get("id").asText());
                }
            }
        }

        assertTrue(authorIdsOfScienceFiction.size() > 0);

        /* Test Default */
        JsonNode result = mapper.readTree(
                RestAssured
                        .get(String.format("/author/%s/books?include=authors&filter[book.genre]=Science Fiction", asimovId))
                        .asString()
        );

        for (JsonNode author : result.get("included")) {
            assertTrue(authorIdsOfScienceFiction.contains(author.get("id").asText()));
        }

        /* Test RSQL Typed */
        result = mapper.readTree(
                RestAssured
                        .get(String.format("/author/%s/books?include=authors&filter[book]=genre=='Science Fiction'", asimovId))
                        .asString());

        for (JsonNode author : result.get("included")) {
            assertTrue(authorIdsOfScienceFiction.contains(author.get("id").asText()));
        }
    }

    @Test
    void testNonRootFilterIsNull() throws IOException {
        Set<JsonNode> bookIdsWithNullGenre = new HashSet<>();

        for (JsonNode book : nullNedBooks.get("data")) {
            if (book.get("attributes").get("genre").isNull()) {
                bookIdsWithNullGenre.add(book.get("id"));
            }
        }

        assertTrue(bookIdsWithNullGenre.size() > 0);

        /* Test Default */
        JsonNode result = mapper.readTree(
                RestAssured
                        .get(String.format("/author/%s/books?filter[book.genre][isnull]", nullNedId))
                        .asString()
        );

        assertEquals(result.get("data").size(), bookIdsWithNullGenre.size());

        for (JsonNode book : result.get("data")) {
            assertTrue(book.get("attributes").get("genre").isNull());
            assertTrue(bookIdsWithNullGenre.contains(book.get("id")));
        }

        /* Test RSQL Typed */
        /* param = true */
        result = mapper.readTree(
                RestAssured
                        .get(String.format("/author/%s/books?filter[book]=genre=isnull=true", nullNedId))
                        .asString()
        );

        assertEquals(result.get("data").size(), bookIdsWithNullGenre.size());

        for (JsonNode book : result.get("data")) {
            assertTrue(book.get("attributes").get("genre").isNull());
            assertTrue(bookIdsWithNullGenre.contains(book.get("id")));
        }

        /* param = 1 */
        result = mapper.readTree(
                RestAssured
                        .get(String.format("/author/%s/books?filter[book]=genre=isnull=1", nullNedId))
                        .asString()
        );

        assertEquals(result.get("data").size(), bookIdsWithNullGenre.size());

        for (JsonNode book : result.get("data")) {
            assertTrue(book.get("attributes").get("genre").isNull());
            assertTrue(bookIdsWithNullGenre.contains(book.get("id")));
        }
    }

    @Test
    void testNonRootFilterIsNotNull() throws IOException {
        Set<JsonNode> bookIdsWithNonNullGenre = new HashSet<>();

        for (JsonNode book : nullNedBooks.get("data")) {
            if (!book.get("attributes").get("genre").isNull()) {
                bookIdsWithNonNullGenre.add(book.get("id"));
            }
        }

        assertTrue(bookIdsWithNonNullGenre.size() > 0);

        /* Test Default */
        JsonNode result = mapper.readTree(
                RestAssured
                        .get(String.format("/author/%s/books?filter[book.genre][notnull]", nullNedId))
                        .asString()
        );

        assertEquals(result.get("data").size(), bookIdsWithNonNullGenre.size());

        for (JsonNode book : result.get("data")) {
            assertFalse(book.get("attributes").get("genre").isNull());
            assertTrue(bookIdsWithNonNullGenre.contains(book.get("id")));
        }

        /* Test RSQL Typed */
        /* param = false */
        result = mapper.readTree(
                RestAssured
                        .get(String.format("/author/%s/books?filter[book]=genre=isnull=false", nullNedId))
                        .asString()
        );

        assertEquals(result.get("data").size(), bookIdsWithNonNullGenre.size());

        for (JsonNode book : result.get("data")) {
            assertFalse(book.get("attributes").get("genre").isNull());
            assertTrue(bookIdsWithNonNullGenre.contains(book.get("id")));
        }

        /* param = 0 */
        result = mapper.readTree(
                RestAssured
                        .get(String.format("/author/%s/books?filter[book]=genre=isnull=0", nullNedId))
                        .asString()
        );

        assertEquals(result.get("data").size(), bookIdsWithNonNullGenre.size());

        for (JsonNode book : result.get("data")) {
            assertFalse(book.get("attributes").get("genre").isNull());
            assertTrue(bookIdsWithNonNullGenre.contains(book.get("id")));
        }
    }

    @Test
    void testPublishDateGreaterThanFilter() throws IOException {
        Set<JsonNode> bookIdsWithNonNullGenre = new HashSet<>();
        long publishDate;

        for (JsonNode book : nullNedBooks.get("data")) {
            if (!book.get("attributes").get("genre").isNull()) {
                bookIdsWithNonNullGenre.add(book.get("id"));
            }
        }

        assertTrue(bookIdsWithNonNullGenre.size() > 0);

        /* Test Default */
        JsonNode result = mapper.readTree(
                RestAssured
                        .get("/book?filter[book.publishDate][gt]=1")
                        .asString()
        );

        assertEquals(result.get("data").size(), 1);

        for (JsonNode book : result.get("data")) {
            publishDate = book.get("attributes").get("publishDate").asLong();
            assertTrue(publishDate > 1L);
        }

        /* Test RSQL Typed */
        result = mapper.readTree(
                RestAssured
                        .get("/book?filter[book]=publishDate>1")
                        .asString()
        );

        assertEquals(result.get("data").size(), 1);

        for (JsonNode book : result.get("data")) {
            publishDate = book.get("attributes").get("publishDate").asLong();
            assertTrue(publishDate > 1L);
        }
    }

    @Test
    void testPublishDateGreaterThanFilterSubRecord() throws IOException {
        long publishDate;

        /* Test Default */
        JsonNode result = mapper.readTree(
                RestAssured
                        .get(String.format("/author/%s/books?filter[book.publishDate][gt]=1454638927411", orsonCardId))
                        .asString()
        );

        assertTrue(result.get("data").size() > 0);

        for (JsonNode book : result.get("data")) {
            publishDate = book.get("attributes").get("publishDate").asLong();
            assertTrue(publishDate > 1454638927411L);
        }

        /* Test RSQL Typed */
        result = mapper.readTree(
                RestAssured
                        .get(String.format("/author/%s/books?filter[book]=publishDate>1454638927411", orsonCardId))
                        .asString()
        );

        assertTrue(result.get("data").size() > 0);

        for (JsonNode book : result.get("data")) {
            publishDate = book.get("attributes").get("publishDate").asLong();
            assertTrue(publishDate > 1454638927411L);
        }
    }

    @Test
    void testPublishDateLessThanOrEqualsFilterSubRecord() throws IOException {
        long publishDate;

        /* Test Default */
        JsonNode result = mapper.readTree(
                RestAssured
                        .get(String.format("/author/%s/books?filter[book.publishDate][le]=1454638927412", orsonCardId))
                        .asString()
        );

        assertEquals(result.get("data").size(), 1);

        for (JsonNode book : result.get("data")) {
            publishDate = book.get("attributes").get("publishDate").asLong();
            assertTrue(publishDate <= 1454638927412L);
        }

        /* Test RSQL Typed */
        result = mapper.readTree(
                RestAssured
                        .get(String.format("/author/%s/books?filter[book]=publishDate<=1454638927412", orsonCardId))
                        .asString()
        );

        assertEquals(result.get("data").size(), 1);

        for (JsonNode book : result.get("data")) {
            publishDate = book.get("attributes").get("publishDate").asLong();
            assertTrue(publishDate <= 1454638927412L);
        }
    }

    @Test
    void testPublishDateLessThanOrEqual() throws IOException {
        long publishDate;

        /* Test Default */
        JsonNode result = mapper.readTree(
                RestAssured
                        .get("book?filter[book.publishDate][le]=1454638927412")
                        .asString()
        );

        assertTrue(result.get("data").size() > 0);

        for (JsonNode book : result.get("data")) {
            publishDate = book.get("attributes").get("publishDate").asLong();
            assertTrue(publishDate <= 1454638927412L);
        }

        /* Test RSQL Typed */
        result = mapper.readTree(
                RestAssured
                        .get("book?filter[book]=publishDate<=1454638927412")
                        .asString()
        );

        assertTrue(result.get("data").size() > 0);

        for (JsonNode book : result.get("data")) {
            publishDate = book.get("attributes").get("publishDate").asLong();
            assertTrue(publishDate <= 1454638927412L);
        }

        /* Test RSQL Global */
        result = mapper.readTree(
                RestAssured
                        .get("book?filter=publishDate<=1454638927412")
                        .asString()
        );

        assertTrue(result.get("data").size() > 0);

        for (JsonNode book : result.get("data")) {
            publishDate = book.get("attributes").get("publishDate").asLong();
            assertTrue(publishDate <= 1454638927412L);
        }
    }

    @Test
    void testPublishDateLessThanFilter() throws IOException {
        long publishDate;

        /* Test Default */
        JsonNode result = mapper.readTree(
                RestAssured
                        .get("/book?filter[book.publishDate][lt]=1454638927411")
                        .asString()
        );

        assertTrue(result.get("data").size() > 0);

        for (JsonNode book : result.get("data")) {
            publishDate = book.get("attributes").get("publishDate").asLong();
            assertTrue(publishDate < 1454638927411L);
        }

        /* RSQL Typed */
        result = mapper.readTree(
                RestAssured
                        .get("/book?filter[book]=publishDate<1454638927411")
                        .asString()
        );

        assertTrue(result.get("data").size() > 0);

        for (JsonNode book : result.get("data")) {
            publishDate = book.get("attributes").get("publishDate").asLong();
            assertTrue(publishDate < 1454638927411L);
        }

        /* RSQL Global */
        result = mapper.readTree(
                RestAssured
                        .get("/book?filter=publishDate<1454638927411")
                        .asString()
        );

        assertTrue(result.get("data").size() > 0);

        for (JsonNode book : result.get("data")) {
            publishDate = book.get("attributes").get("publishDate").asLong();
            assertTrue(publishDate < 1454638927411L);
        }
    }

    /**
     * Verifies that issue 508 is closed.
     */
    @Test
    void testIssue508() throws IOException {
        JsonNode result = mapper.readTree(
                RestAssured
                        .get("book?filter=(authors.name=='Thomas Harris',publisher.name=='Default Publisher')&page[totals]")
                        .asString()
        );
        assertEquals(2, result.get("data").size());

        JsonNode pageNode = result.get("meta").get("page");
        assertNotNull(pageNode);
        assertEquals(pageNode.get("totalRecords").asInt(), 2);

        result = mapper.readTree(
                RestAssured
                        .get("book?filter=(authors.name=='Thomas Harris')&page[totals]")
                        .asString()
        );
        assertEquals(1, result.get("data").size());

        pageNode = result.get("meta").get("page");
        assertNotNull(pageNode);
        assertEquals(pageNode.get("totalRecords").asInt(), 1);

        result = mapper.readTree(
                RestAssured
                        .get("book?filter=(publisher.name=='Default Publisher')&page[totals]")
                        .asString()
        );
        assertEquals(1, result.get("data").size());

        pageNode = result.get("meta").get("page");
        assertNotNull(pageNode);
        assertEquals(pageNode.get("totalRecords").asInt(), 1);
    }

    @Test
    void testGetBadRelationshipNameWithNestedFieldFilter() throws IOException {
        /* Test Default */
        JsonNode result = mapper.readTree(
                RestAssured
                        .get("book?filter[book.author12.name]=Null Ned")
                        .asString()
        );

        assertEquals(result.get("errors").get(0).asText(),
                "InvalidPredicateException: Unknown field in filter: author12\n" +
                        "Invalid query parameter: filter[book.author12.name]");

        /* Test RSQL Global */
        result = mapper.readTree(
                RestAssured
                        .get("book?filter=author12.name=='Null Ned'")
                        .asString()
        );

        assertEquals(result.get("errors").get(0).asText(),
                "InvalidPredicateException: Invalid filter format: filter\n" +
                        "No such association author12 for type book\n" +
                        "Invalid filter format: filter\n" +
                        "Invalid query parameter: filter");
    }

    @Test
    void testGetBooksFilteredByAuthors() throws IOException {
        /* Test Default */
        JsonNode result = mapper.readTree(
                RestAssured
                        .get("book?filter[book.authors.name]=Null Ned")
                        .asString()
        );

        assertEquals(result.get("data").size(), nullNedBooks.get("data").size());

        for (JsonNode book : result.get("data")) {
            String authorId = book.get("relationships").get("authors").get("data").get(0).get("id").asText();
            assertEquals(authorId, nullNedId);
        }

        /* Test RSQL Global */
        result = mapper.readTree(
                RestAssured
                        .get("book?filter=authors.name=='Null Ned'")
                        .asString()
        );

        assertEquals(result.get("data").size(), nullNedBooks.get("data").size());

        for (JsonNode book : result.get("data")) {
            String authorId = book.get("relationships").get("authors").get("data").get(0).get("id").asText();
            assertEquals(authorId, nullNedId);
        }
    }

    @Test
    void testGetBooksFilteredByAuthorsId() throws IOException {
        String nullNedIdStr = String.valueOf(nullNedId);
        /* Test Default */
        JsonNode result = mapper.readTree(
                RestAssured
                        .get("book?filter[book.authors.id]=" + nullNedIdStr)
                        .asString()
        );

        assertEquals(result.get("data").size(), nullNedBooks.get("data").size());

        for (JsonNode book : result.get("data")) {
            String authorId = book.get("relationships").get("authors").get("data").get(0).get("id").asText();
            assertEquals(authorId, nullNedId);
        }

        /* Test RSQL Global */
        result = mapper.readTree(
                RestAssured
                        .get("book?filter=authors.id==" + nullNedIdStr)
                        .asString()
        );

        assertEquals(result.get("data").size(), nullNedBooks.get("data").size());

        for (JsonNode book : result.get("data")) {
            String authorId = book.get("relationships").get("authors").get("data").get(0).get("id").asText();
            assertEquals(authorId, nullNedId);
        }
    }

    @Test
    void testGetBooksFilteredByAuthorAndTitle() throws IOException {
        /* Test Default */
        JsonNode result = mapper.readTree(
                RestAssured
                        .get("book?filter[book.authors.name]=Null Ned&filter[book.title]=Life with Null Ned")
                        .asString()
        );

        assertEquals(result.get("data").size(), 1);
        assertEquals(result.get("data").get(0).get("attributes").get("title").asText(), "Life with Null Ned");
        assertEquals(result.get("data").get(0).get("relationships").get("authors").get("data").get(0).get("id").asText(), nullNedId);

        /* Test RSQL Global */
        result = mapper.readTree(
                RestAssured
                        .get("book?filter=authors.name=='Null Ned';title=='Life with Null Ned'")
                        .asString()
        );

        assertEquals(result.get("data").size(), 1);
        assertEquals(result.get("data").get(0).get("attributes").get("title").asText(), "Life with Null Ned");
        assertEquals(result.get("data").get(0).get("relationships").get("authors").get("data").get(0).get("id").asText(), nullNedId);
    }

    @Test
    void testFilterAuthorsByBookChapterTitle() throws IOException {
        /* Test Default */
        JsonNode result = mapper.readTree(
                RestAssured
                        .get("/author?sort=-name&filter[author.books.chapters.title][in]=Viva la Roma!,Mamma mia I wantz some pizza!")
                        .asString()
        );
        assertEquals(result.get("data").size(), 2);

        for (JsonNode author : result.get("data")) {
            String name = author.get("attributes").get("name").asText();
            assertTrue(name.equals("Isaac Asimov") || name.equals("Null Ned"));
        }

        /* Test RSQL Global */
        result = mapper.readTree(
                RestAssured
                        .get("/author?filter=books.chapters.title=in=('Viva la Roma!','Mamma mia I wantz some pizza!')")
                        .asString()
        );
        assertEquals(result.get("data").size(), 2);

        for (JsonNode author : result.get("data")) {
            String name = author.get("attributes").get("name").asText();
            assertTrue(name.equals("Isaac Asimov") || name.equals("Null Ned"));
        }
    }

    @Test
    void testFilterAuthorBookByPublisher() throws IOException {
        /* Test default */
        JsonNode result = mapper.readTree(
                RestAssured
                        .get(String.format("/author/%s/books?filter[book.publisher.name]=Default publisher", hemingwayId))
                        .asString()
        );
        JsonNode data = result.get("data");
        assertEquals(data.size(), 1);

        for (JsonNode book : data) {
            String name = book.get("attributes").get("title").asText();
            assertEquals("The Old Man and the Sea", name);
        }

        /* Test RSQL */
        result = mapper.readTree(
                RestAssured
                        .get(String.format("/author/%s/books?filter[book]=publisher.name=='Default publisher'", hemingwayId))
                        .asString()
        );
        data = result.get("data");
        assertEquals(data.size(), 1);

        for (JsonNode book : data) {
            String name = book.get("attributes").get("title").asText();
            assertEquals("The Old Man and the Sea", name);
        }
    }

    /**
     * Tests a computed relationship filter.
     */
    @Test
    void testFilterBookByEditor() {
        RestAssured
                .given()
                .get("/book?filter[book]=editor.firstName=='John'")
                .then()
                .statusCode(org.apache.http.HttpStatus.SC_OK)
                .body("data.attributes.title", contains("The Old Man and the Sea"))
                .body("data", hasSize(1));

        RestAssured
                .given()
                .get("/book?filter[book]=editor.firstName=='Foobar'")
                .then()
                .statusCode(org.apache.http.HttpStatus.SC_OK)
                .body("data", hasSize(0));
    }

    /**
     * Tests a computed attribute filter.
     */
    @Test
    void testFilterEditorByFullName() {
        RestAssured
                .given()
                .get("/editor?filter[editor]=fullName=='John Doe'")
                .then()
                .statusCode(org.apache.http.HttpStatus.SC_OK)
                .body("data.attributes.firstName", contains("John"))
                .body("data.attributes.lastName", contains("Doe"))
                .body("data", hasSize(1));

        RestAssured
                .given()
                .get("/editor?filter[editor]=fullName=='Foobar'")
                .then()
                .statusCode(org.apache.http.HttpStatus.SC_OK)
                .body("data", hasSize(0));
    }

    @Test
    void testFailFilterAuthorBookByChapter() throws IOException {
        /* Test default */
        JsonNode result = mapper.readTree(
                RestAssured
                        .get(String.format("/author/%s/books?filter[book.chapters.title]=doesn't matter", hemingwayId))
                        .asString()
        );
        assertNotNull(result.get("errors"));

        /* Test RSQL */
        result = mapper.readTree(
                RestAssured
                        .get(String.format("/author/%s/books?filter[book]=chapters.title=='Borked'", hemingwayId))
                        .asString()
        );
        assertNotNull(result.get("errors"));
    }

    @Test
    void testGetBadRelationshipRoot() throws IOException {
        /* Test Default */
        JsonNode result = mapper.readTree(
                RestAssured
                        .get("/author?filter[idontexist.books.title][in]=Viva la Roma!,Mamma mia I wantz some pizza!")
                        .asString()
        );
        assertEquals(result.get("errors").get(0).asText(),
                "InvalidPredicateException: Unknown entity in filter: idontexist\n" +
                        "Invalid query parameter: filter[idontexist.books.title][in]");

        /* Test RSQL Global */
        result = mapper.readTree(
                RestAssured
                        .get("/author?filter=idontexist.books.title=in=('Viva la Roma!','Mamma mia I wantz some pizza!')")
                        .asString()
        );
        assertEquals(result.get("errors").get(0).asText(),
                "InvalidPredicateException: Invalid filter format: filter\n" +
                        "No such association idontexist for type author\n" +
                        "Invalid filter format: filter\n" +
                        "Invalid query parameter: filter");
    }

    @Test
    void testGetBadRelationshipIntermediate() throws IOException {
        /* Test Default */
        JsonNode result = mapper.readTree(
                RestAssured
                        .get("/author?filter[author.idontexist.title][in]=Viva la Roma!,Mamma mia I wantz some pizza!")
                        .asString()
        );
        assertEquals(result.get("errors").get(0).asText(),
                "InvalidPredicateException: Unknown field in filter: idontexist\n" +
                        "Invalid query parameter: filter[author.idontexist.title][in]");

        /* Test RSQL Global */
        result = mapper.readTree(
                RestAssured
                        .get("/author?filter=idontexist.title=in=('Viva la Roma!','Mamma mia I wantz some pizza!')")
                        .asString()
        );
        assertEquals(result.get("errors").get(0).asText(),
                "InvalidPredicateException: Invalid filter format: filter\n" +
                        "No such association idontexist for type author\n" +
                        "Invalid filter format: filter\n" +
                        "Invalid query parameter: filter");
    }

    @Test
    void testGetBadRelationshipLeaf() throws IOException {
        /* Test Default */
        JsonNode result = mapper.readTree(
                RestAssured
                        .get("/author?filter[author.books.idontexist][in]=Viva la Roma!,Mamma mia I wantz some pizza!")
                        .asString()
        );
        assertEquals(result.get("errors").get(0).asText(),
                "InvalidPredicateException: Unknown field in filter: idontexist\n" +
                        "Invalid query parameter: filter[author.books.idontexist][in]");

        /* Test RSQL Global */
        result = mapper.readTree(
                RestAssured
                        .get("/author?filter=books.idontexist=in=('Viva la Roma!','Mamma mia I wantz some pizza!')")
                        .asString()
        );
        assertEquals(result.get("errors").get(0).asText(),
                "InvalidPredicateException: Invalid filter format: filter\n" +
                        "No such association idontexist for type book\n" +
                        "Invalid filter format: filter\n" +
                        "Invalid query parameter: filter");
    }

    /*
     * Verify that a combination of filters and order by generate working SQL.
     */
    @Test
    void testFilterWithSort() throws IOException {
        JsonNode result = mapper.readTree(
                RestAssured
                        .get(String.format("/author/%s/books?filter[book.title][notnull]=true&sort=title", asimovId))
                        .asString()
        );
        JsonNode data = result.get("data");
        assertEquals(data.size(), 2);
    }

    @AfterAll
    void cleanUp() {
        for (int id : authorIds) {
            RestAssured
                    .given()
                    .accept(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                    .delete("/author/" + id);
        }
        for (int id : bookIds) {
            RestAssured
                    .given()
                    .accept(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                    .delete("/book/" + id);
        }
    }
}
