/*
 * Copyright 2018, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.tests;

import static com.yahoo.elide.Elide.JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION;
import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yahoo.elide.core.exceptions.HttpStatus;
import com.yahoo.elide.core.utils.JsonParser;
import com.yahoo.elide.initialization.IntegrationTest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

public class FilterIT extends IntegrationTest {

    private final JsonParser jsonParser = new JsonParser();

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
    void setup() throws JsonProcessingException {

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

        books = getAsNode("/book");
        JsonNode authors = getAsNode("/author");

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

        asimovBooks = getAsNode(String.format("/author/%s/books", asimovId));
        nullNedBooks = getAsNode(String.format("/author/%s/books", nullNedId));
        thomasHarrisBooks = getAsNode(String.format("/author/%s/books", thomasHarrisId));
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
    void testRootComplexAttribute() throws Exception {
        JsonNode priceyBooks = getAsNode("/book?filter[book]=price.total>=10");

        assertEquals(2, priceyBooks.get("data").size());

        priceyBooks = getAsNode("/book?filter[book]=price.total<5");

        assertEquals(0, priceyBooks.get("data").size());
    }

    @Test
    void testRootNestedComplexAttribute() throws Exception {
        JsonNode priceyBooks = getAsNode("/book?filter[book]=price.currency.isoCode==AED");

        assertEquals(1, priceyBooks.get("data").size());

        priceyBooks = getAsNode("/book?filter[book]=price.currency.isoCode==ABC");

        assertEquals(0, priceyBooks.get("data").size());
    }

    @Test
    void testRootFilterImplicitSingle() throws JsonProcessingException {
        int scienceFictionBookCount = 0;
        for (JsonNode node : books.get("data")) {
            if (node.get("attributes").get("genre").asText().equalsIgnoreCase("Science Fiction")) {
                scienceFictionBookCount += 1;
            }
        }

        assertTrue(scienceFictionBookCount > 0);

        /* Test Default */
        JsonNode scienceFictionBooks = getAsNode("/book?filter[book.genre]=Science Fiction");

        assertEquals(scienceFictionBookCount, scienceFictionBooks.get("data").size());

        /* Test RSQL Typed */
        scienceFictionBooks = getAsNode("/book?filter[book]=genre=='Science Fiction'");

        assertEquals(scienceFictionBookCount, scienceFictionBooks.get("data").size());

        /* Test RSQL Global */
        scienceFictionBooks = getAsNode("/book?filter=genre=='Science Fiction'");

        assertEquals(scienceFictionBookCount, scienceFictionBooks.get("data").size());
    }

    @Test
    void testRootFilterInSingle() throws JsonProcessingException {
        int literaryFictionBookCount = 0;
        for (JsonNode node : books.get("data")) {
            if (node.get("attributes").get("genre").asText().equalsIgnoreCase("Literary Fiction")) {
                literaryFictionBookCount += 1;
            }
        }

        assertTrue(literaryFictionBookCount > 0);

        /* Test Default */
        JsonNode literaryFictionBooks = getAsNode("/book?filter[book.genre][in]=Literary Fiction");

        assertEquals(literaryFictionBookCount, literaryFictionBooks.get("data").size());

        /* Test RSQL Typed */
        literaryFictionBooks = getAsNode("/book?filter[book]=genre=in='Literary Fiction'");

        assertEquals(literaryFictionBookCount, literaryFictionBooks.get("data").size());

        /* Test RSQL Global */
        literaryFictionBooks = getAsNode("/book?filter=genre=in='Literary Fiction'");

        assertEquals(literaryFictionBookCount, literaryFictionBooks.get("data").size());

        literaryFictionBooks = getAsNode("/book?filter=genre=ini='literary FICTION'");

        assertEquals(literaryFictionBookCount, literaryFictionBooks.get("data").size());
    }

    @Test
    @Tag("skipInMemory")
    void testRootFilterNotInSingle() throws JsonProcessingException {
        int nonLiteraryFictionBookCount = 0;
        for (JsonNode node : books.get("data")) {
            if (!node.get("attributes").get("genre").isNull()
                    && !node.get("attributes").get("genre").asText().equalsIgnoreCase("Literary Fiction")) {
                nonLiteraryFictionBookCount += 1;
            }
        }

        assertTrue(nonLiteraryFictionBookCount > 0);

        /* Test Default */
        JsonNode nonLiteraryFictionBooks = getAsNode("/book?filter[book.genre][not]=Literary Fiction");

        assertEquals(nonLiteraryFictionBookCount, nonLiteraryFictionBooks.get("data").size());

        /* Test RSQL typed */
        nonLiteraryFictionBooks = getAsNode("/book?filter[book]=genre!='Literary Fiction'");

        assertEquals(nonLiteraryFictionBookCount, nonLiteraryFictionBooks.get("data").size());

        /* Test RSQL global */
        nonLiteraryFictionBooks = getAsNode("/book?filter=genre!='Literary Fiction'");

        assertEquals(nonLiteraryFictionBookCount, nonLiteraryFictionBooks.get("data").size());
    }

    @Test
    @Tag("skipInMemory")
    void testRootFilterNotInMultiple() throws JsonProcessingException {
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
        JsonNode nonFictionBooks = getAsNode("/book?filter[book.genre][not]=Literary Fiction,Science Fiction");

        assertEquals(nonFictionBookCount, nonFictionBooks.get("data").size());

        /* Test RSQL typed */
        nonFictionBooks = getAsNode("/book?filter[book]=genre=out=('Literary Fiction','Science Fiction')");

        assertEquals(nonFictionBookCount, nonFictionBooks.get("data").size());

        /* Test RSQL global */
        nonFictionBooks = getAsNode("/book?filter=genre=out=('Literary Fiction','Science Fiction')");

        assertEquals(nonFictionBookCount, nonFictionBooks.get("data").size());
    }

    @Test
    void testRootFilterInMultipleSingle() throws JsonProcessingException {
        int literaryAndScienceFictionBookCount = 0;
        for (JsonNode node : books.get("data")) {
            if (node.get("attributes").get("genre").asText().equalsIgnoreCase("Literary Fiction")
                    || node.get("attributes").get("genre").asText().equalsIgnoreCase("Science Fiction")) {
                literaryAndScienceFictionBookCount += 1;
            }
        }

        assertTrue(literaryAndScienceFictionBookCount > 0);

        /* Test Default */
        JsonNode literaryAndScienceFictionBooks = getAsNode("/book?filter[book.genre][in]=Literary Fiction,Science Fiction");

        assertEquals(literaryAndScienceFictionBookCount, literaryAndScienceFictionBooks.get("data").size());

        /* Test RSQL typed */
        literaryAndScienceFictionBooks = getAsNode("/book?filter[book]=genre=in=('Literary Fiction','Science Fiction')");

        assertEquals(literaryAndScienceFictionBookCount, literaryAndScienceFictionBooks.get("data").size());

        /* Test RSQL global */
        literaryAndScienceFictionBooks = getAsNode("/book?filter=genre=in=('Literary Fiction','Science Fiction')");

        assertEquals(literaryAndScienceFictionBookCount, literaryAndScienceFictionBooks.get("data").size());

        literaryAndScienceFictionBooks = getAsNode("/book?filter=genre=ini=('LITERARY FICTION','SCIENCE FICTION')");

        assertEquals(literaryAndScienceFictionBookCount, literaryAndScienceFictionBooks.get("data").size());
    }

    @Test
    void testNonRootCollectionError() {
        given()
                .get("/publisher")
                .then()
                .statusCode(HttpStatus.SC_NOT_FOUND)
                .body("errors.detail", contains("Unknown collection publisher"));
    }

    @Test
    void testNonRootEntityError() {
        given()
                .get("/publisher/1")
                .then()
                .statusCode(HttpStatus.SC_NOT_FOUND)
                .body("errors.detail", contains("Unknown collection publisher"));
    }

    @Test
    void testRootFilterPostfix() throws JsonProcessingException {
        int genreEndsWithFictionBookCount = 0;
        for (JsonNode node : books.get("data")) {
            if (node.get("attributes").get("genre").asText().toLowerCase(Locale.ENGLISH).endsWith("fiction")) {
                genreEndsWithFictionBookCount += 1;
            }
        }

        assertTrue(genreEndsWithFictionBookCount > 0);

        /* Test Default */
        JsonNode genreEndsWithFictionBooks = getAsNode("/book?filter[book.genre][postfix]=Fiction");

        assertEquals(genreEndsWithFictionBookCount, genreEndsWithFictionBooks.get("data").size());

        /* Test RSQL Typed */
        genreEndsWithFictionBooks = getAsNode("/book?filter[book]=genre==*Fiction");

        assertEquals(genreEndsWithFictionBookCount, genreEndsWithFictionBooks.get("data").size());

        /* Test RSQL Global */
        genreEndsWithFictionBooks = getAsNode("/book?filter=genre==*Fiction");

        assertEquals(genreEndsWithFictionBookCount, genreEndsWithFictionBooks.get("data").size());
    }

    @Test
    void testRootFilterPrefix() throws JsonProcessingException {
        int titleStartsWithTheBookCount = 0;
        for (JsonNode node : books.get("data")) {
            if (node.get("attributes").get("title").asText().toLowerCase(Locale.ENGLISH).startsWith("the")) {
                titleStartsWithTheBookCount += 1;
            }
        }

        assertTrue(titleStartsWithTheBookCount > 0);

        /* Test Default */
        JsonNode titleStartsWithTheBooks = getAsNode("/book?filter[book.title][prefix]=The");

        assertEquals(titleStartsWithTheBookCount, titleStartsWithTheBooks.get("data").size());

        /* Test RSQL Typed */
        titleStartsWithTheBooks = getAsNode("/book?filter[book]=title==The*");

        assertEquals(titleStartsWithTheBookCount, titleStartsWithTheBooks.get("data").size());

        /* Test RSQL Global */
        titleStartsWithTheBooks = getAsNode("/book?filter=title==The*");

        assertEquals(titleStartsWithTheBookCount, titleStartsWithTheBooks.get("data").size());
    }

    @Test
    @Tag("skipInMemory")
    void testRootFilterPrefixWithSpecialChars() throws JsonProcessingException {
        int titleStartsWithTheBookCount = 0;
        for (JsonNode node : books.get("data")) {
            if (node.get("attributes").get("title").asText().toLowerCase(Locale.ENGLISH).startsWith("i'm")) {
                titleStartsWithTheBookCount += 1;
            }
        }

        assertTrue(titleStartsWithTheBookCount > 0);

        /* Test Default */
        JsonNode titleStartsWithTheBooks = getAsNode("/book?filter[book.title][prefix]=i'm");

        assertEquals(titleStartsWithTheBookCount, titleStartsWithTheBooks.get("data").size());

        /* Test RSQL Typed */
        titleStartsWithTheBooks = getAsNode("/book?filter[book]=title=='i\\'m*'");

        assertEquals(titleStartsWithTheBookCount, titleStartsWithTheBooks.get("data").size());

        /* Test RSQL Global */
        titleStartsWithTheBooks = getAsNode("/book?filter=title=='i\\'m*'");

        assertEquals(titleStartsWithTheBookCount, titleStartsWithTheBooks.get("data").size());
    }

    @Test
    @Tag("skipInMemory")
    void testRootFilterInfix() throws JsonProcessingException {
        int titleContainsTheBookCount = 0;
        for (JsonNode node : books.get("data")) {
            if (node.get("attributes").get("title").asText().toLowerCase(Locale.ENGLISH).contains("the")) {
                titleContainsTheBookCount += 1;
            }
        }

        assertTrue(titleContainsTheBookCount > 0);

        /* Test Default */
        JsonNode titleContainsTheBooks = getAsNode("/book?filter[book.title][infix]=the");

        assertEquals(titleContainsTheBookCount, titleContainsTheBooks.get("data").size());

        /* Test RSQL Typed */
        titleContainsTheBooks = getAsNode("/book?filter[book]=title==*the*");

        assertEquals(titleContainsTheBookCount, titleContainsTheBooks.get("data").size());

        /* Test RSQL Global */
        titleContainsTheBooks = getAsNode("/book?filter=title==*the*");

        assertEquals(titleContainsTheBookCount, titleContainsTheBooks.get("data").size());

        titleContainsTheBooks = getAsNode("/book?filter=title=ini=*THE*");

        assertEquals(titleContainsTheBookCount, titleContainsTheBooks.get("data").size());
    }

    @Test
    void testRootFilterWithInclude() throws JsonProcessingException {
        Set<String> authorIdsOfLiteraryFiction = new HashSet<>();

        for (JsonNode book : books.get("data")) {
            if (book.get("attributes").get("genre").asText().equals("Literary Fiction")) {
                for (JsonNode author : book.get("relationships").get("authors").get("data")) {
                    authorIdsOfLiteraryFiction.add(author.get("id").asText());
                }
            }
        }

        assertTrue(CollectionUtils.isNotEmpty(authorIdsOfLiteraryFiction));

        /* Test Default */
        JsonNode result = getAsNode("/book?include=authors&filter[book.genre]=Literary Fiction");

        for (JsonNode author : result.get("included")) {
            assertTrue(authorIdsOfLiteraryFiction.contains(author.get("id").asText()));
        }

        /* Test RSQL Typed */
        result = getAsNode("/book?include=authors&filter[book]=genre=='Literary Fiction'");

        for (JsonNode author : result.get("included")) {
            assertTrue(authorIdsOfLiteraryFiction.contains(author.get("id").asText()));
        }

        /* Test RSQL Global */
        result = getAsNode("/book?include=authors&filter=genre=='Literary Fiction'");

        for (JsonNode author : result.get("included")) {
            assertTrue(authorIdsOfLiteraryFiction.contains(author.get("id").asText()));
        }
    }

    @Test
    void testRootFilterIsNull() throws JsonProcessingException {
        Set<JsonNode> bookIdsWithNullGenre = new HashSet<>();

        for (JsonNode book : books.get("data")) {
            if (book.get("attributes").get("genre").isNull()) {
                bookIdsWithNullGenre.add(book.get("id"));
            }
        }

        assertTrue(CollectionUtils.isNotEmpty(bookIdsWithNullGenre));

        /* Test Default */
        JsonNode result = getAsNode("/book?filter[book.genre][isnull]");

        assertEquals(result.get("data").size(), bookIdsWithNullGenre.size());

        for (JsonNode book : result.get("data")) {
            assertTrue(book.get("attributes").get("genre").isNull());
            assertTrue(bookIdsWithNullGenre.contains(book.get("id")));
        }

        /* Test RSQL Typed */
        /* param = true */
        result = getAsNode("book?filter[book]=genre=isnull=true");

        assertEquals(result.get("data").size(), bookIdsWithNullGenre.size());

        for (JsonNode book : result.get("data")) {
            assertTrue(book.get("attributes").get("genre").isNull());
            assertTrue(bookIdsWithNullGenre.contains(book.get("id")));
        }

        /* param = 1 */
        result = getAsNode("book?filter[book]=genre=isnull=1");

        assertEquals(result.get("data").size(), bookIdsWithNullGenre.size());

        for (JsonNode book : result.get("data")) {
            assertTrue(book.get("attributes").get("genre").isNull());
            assertTrue(bookIdsWithNullGenre.contains(book.get("id")));
        }

        /* Test RSQL Global */
        /* param = true */
        result = getAsNode("book?filter=genre=isnull=true");

        assertEquals(result.get("data").size(), bookIdsWithNullGenre.size());

        for (JsonNode book : result.get("data")) {
            assertTrue(book.get("attributes").get("genre").isNull());
            assertTrue(bookIdsWithNullGenre.contains(book.get("id")));
        }

        /* param = 1 */
        result = getAsNode("book?filter=genre=isnull=1");

        assertEquals(result.get("data").size(), bookIdsWithNullGenre.size());

        for (JsonNode book : result.get("data")) {
            assertTrue(book.get("attributes").get("genre").isNull());
            assertTrue(bookIdsWithNullGenre.contains(book.get("id")));
        }
    }

    @Test
    void testRootFilterIsNotNull() throws JsonProcessingException {
        Set<JsonNode> bookIdsWithNonNullGenre = new HashSet<>();

        for (JsonNode book : books.get("data")) {
            if (!book.get("attributes").get("genre").isNull()) {
                bookIdsWithNonNullGenre.add(book.get("id"));
            }
        }

        assertTrue(CollectionUtils.isNotEmpty(bookIdsWithNonNullGenre));

        /* Test Default */
        JsonNode result = getAsNode("/book?filter[book.genre][notnull]");

        assertEquals(result.get("data").size(), bookIdsWithNonNullGenre.size());

        for (JsonNode book : result.get("data")) {
            assertFalse(book.get("attributes").get("genre").isNull());
            assertTrue(bookIdsWithNonNullGenre.contains(book.get("id")));
        }

        /* Test RSQL Typed */
        /* param = false */
        result = getAsNode("book?filter[book]=genre=isnull=false");

        assertEquals(result.get("data").size(), bookIdsWithNonNullGenre.size());

        for (JsonNode book : result.get("data")) {
            assertFalse(book.get("attributes").get("genre").isNull());
            assertTrue(bookIdsWithNonNullGenre.contains(book.get("id")));
        }

        /* param = 0 */
        result = getAsNode("book?filter[book]=genre=isnull=0");

        assertEquals(result.get("data").size(), bookIdsWithNonNullGenre.size());

        for (JsonNode book : result.get("data")) {
            assertFalse(book.get("attributes").get("genre").isNull());
            assertTrue(bookIdsWithNonNullGenre.contains(book.get("id")));
        }

        /* Test RSQL Global */
        /* param = false */
        result = getAsNode("book?filter=genre=isnull=false");

        assertEquals(result.get("data").size(), bookIdsWithNonNullGenre.size());

        for (JsonNode book : result.get("data")) {
            assertFalse(book.get("attributes").get("genre").isNull());
            assertTrue(bookIdsWithNonNullGenre.contains(book.get("id")));
        }

        /* param = 0 */
        result = getAsNode("book?filter=genre=isnull=0");

        assertEquals(result.get("data").size(), bookIdsWithNonNullGenre.size());

        for (JsonNode book : result.get("data")) {
            assertFalse(book.get("attributes").get("genre").isNull());
            assertTrue(bookIdsWithNonNullGenre.contains(book.get("id")));
        }
    }

    @Test
    void testNonRootFilterImplicitSingle() throws JsonProcessingException {
        int asimovScienceFictionBookCount = 0;
        for (JsonNode node : asimovBooks.get("data")) {
            if (node.get("attributes").get("genre").asText().equals("Science Fiction")) {
                asimovScienceFictionBookCount += 1;
            }
        }

        assertTrue(asimovScienceFictionBookCount > 0);

        /* Test Default */
        JsonNode asimovScienceFictionBooks = getAsNode(String.format("/author/%s/books?filter[book.genre]=Science Fiction", asimovId));

        assertEquals(asimovScienceFictionBookCount, asimovScienceFictionBooks.get("data").size());

        /* Test RSQL Typed */
        asimovScienceFictionBooks = getAsNode(String.format("/author/%s/books?filter[book]=genre=='Science Fiction'", asimovId));

        assertEquals(asimovScienceFictionBookCount, asimovScienceFictionBooks.get("data").size());
    }

    @Test
    void testNonRootFilterInSingle() throws JsonProcessingException {
        int asimovHistoryBookCount = 0;
        for (JsonNode node : asimovBooks.get("data")) {
            if (node.get("attributes").get("genre").asText().equals("History")) {
                asimovHistoryBookCount += 1;
            }
        }

        assertTrue(asimovHistoryBookCount > 0);

        /* Test Default */
        JsonNode asimovHistoryBooks = getAsNode(String.format("/author/%s/books?filter[book.genre]=History", asimovId));

        assertEquals(asimovHistoryBookCount, asimovHistoryBooks.get("data").size());

        /* Test RSQL Typed */
        asimovHistoryBooks = getAsNode(String.format("/author/%s/books?filter[book]=genre==History", asimovId));

        assertEquals(asimovHistoryBookCount, asimovHistoryBooks.get("data").size());
    }

    @Test
    void testNonRootFilterNotInSingle() throws JsonProcessingException {
        int nonHistoryBookCount = 0;
        for (JsonNode node : asimovBooks.get("data")) {
            if (!node.get("attributes").get("genre").isNull()
                    && !node.get("attributes").get("genre").asText().equals("History")) {
                nonHistoryBookCount += 1;
            }
        }

        assertTrue(nonHistoryBookCount > 0);

        /* Test Default */
        JsonNode nonHistoryBooks = getAsNode(String.format("/author/%s/books?filter[book.genre][not]=History", asimovId));

        assertEquals(nonHistoryBookCount, nonHistoryBooks.get("data").size());

        /* Test RSQL Typed */
        nonHistoryBooks = getAsNode(String.format("/author/%s/books?filter[book]=genre!=History", asimovId));

        assertEquals(nonHistoryBookCount, nonHistoryBooks.get("data").size());
    }

    @Test
    void testNonRootFilterPostfix() throws JsonProcessingException {
        int genreEndsWithFictionBookCount = 0;
        for (JsonNode node : asimovBooks.get("data")) {
            if (node.get("attributes").get("genre").asText().endsWith("Fiction")) {
                genreEndsWithFictionBookCount += 1;
            }
        }

        assertTrue(genreEndsWithFictionBookCount > 0);

        /* Test Default */
        JsonNode genreEndsWithFictionBooks = getAsNode(String.format("/author/%s/books?filter[book.genre][postfix]=Fiction", asimovId));

        assertEquals(genreEndsWithFictionBookCount, genreEndsWithFictionBooks.get("data").size());

        /* Test RSQL Typed */
        genreEndsWithFictionBooks = getAsNode(String.format("/author/%s/books?filter[book]=genre==*Fiction", asimovId));

        assertEquals(genreEndsWithFictionBookCount, genreEndsWithFictionBooks.get("data").size());
    }

    @Test
    void testNonRootFilterPostfixInsensitive() throws JsonProcessingException {
        int editorEdBooks = 0;
        for (JsonNode node : nullNedBooks.get("data")) {
            if (node.get("attributes").get("editorName").asText().endsWith("d")) {
                editorEdBooks += 1;
            }
        }

        assertTrue(editorEdBooks > 0);

        /* Test Default */
        JsonNode editorNameEndsWithd = getAsNode(String.format("/author/%s/books?filter[book.editorName][postfix]=D", nullNedId));

        assertEquals(0, editorNameEndsWithd.get("data").size());

        editorNameEndsWithd = getAsNode(String.format("/author/%s/books?filter[book.editorName][postfixi]=D", nullNedId));

        assertEquals(editorEdBooks, editorNameEndsWithd.get("data").size());

        /* Test RSQL Typed */
        editorNameEndsWithd = getAsNode(String.format("/author/%s/books?filter[book]=editorName==*D", nullNedId));

        assertEquals(0, editorNameEndsWithd.get("data").size());

        editorNameEndsWithd = getAsNode(String.format("/author/%s/books?filter[book]=editorName=ini=*D", nullNedId));

        assertEquals(editorEdBooks, editorNameEndsWithd.get("data").size());
    }

    @Test
    void testNonRootFilterPrefixInsensitive() throws JsonProcessingException {
        int editorEdBooks = 0;
        for (JsonNode node : nullNedBooks.get("data")) {
            if (node.get("attributes").get("editorName").asText().startsWith("E")) {
                editorEdBooks += 1;
            }
        }

        assertTrue(editorEdBooks > 0);

        /* Test Default */
        JsonNode editorNameStartsWithE = getAsNode(String.format("/author/%s/books?filter[book.editorName][prefix]=e", nullNedId));

        assertEquals(0, editorNameStartsWithE.get("data").size());

        editorNameStartsWithE = getAsNode(String.format("/author/%s/books?filter[book.editorName][prefixi]=e", nullNedId));

        assertEquals(editorEdBooks, editorNameStartsWithE.get("data").size());

        /* Test RSQL Typed */

        editorNameStartsWithE = getAsNode(String.format("/author/%s/books?filter[book]=editorName==e*", nullNedId));

        assertEquals(0, editorNameStartsWithE.get("data").size());

        editorNameStartsWithE = getAsNode(String.format("/author/%s/books?filter[book]=editorName=ini=e*", nullNedId));

        assertEquals(editorEdBooks, editorNameStartsWithE.get("data").size());
    }

    @Test
    void testNonRootFilterInfixInsensitive() throws JsonProcessingException {
        int editorEditBooks = 0;
        for (JsonNode node : nullNedBooks.get("data")) {
            if (node.get("attributes").get("editorName").asText().contains("Ed")) {
                editorEditBooks += 1;
            }
        }

        assertTrue(editorEditBooks > 0);

        /* Test Default */
        JsonNode editorNameContainsEd = getAsNode(String.format("/author/%s/books?filter[book.editorName][infix]=eD", nullNedId));

        assertEquals(0, editorNameContainsEd.get("data").size());

        editorNameContainsEd = getAsNode(String.format("/author/%s/books?filter[book.editorName][infixi]=eD", nullNedId));

        assertEquals(editorEditBooks, editorNameContainsEd.get("data").size());

        /* Test RSQL Typed */
        editorNameContainsEd = getAsNode(String.format("/author/%s/books?filter[book]=editorName==*eD*", nullNedId));

        assertEquals(0, editorNameContainsEd.get("data").size());

        editorNameContainsEd = getAsNode(String.format("/author/%s/books?filter[book]=editorName=ini=*eD*", nullNedId));

        assertEquals(editorEditBooks, editorNameContainsEd.get("data").size());
    }

    @Test
    void testNonRootFilterPrefix() throws JsonProcessingException {
        int titleStartsWithTheBookCount = 0;
        for (JsonNode node : asimovBooks.get("data")) {
            if (node.get("attributes").get("title").asText().startsWith("The")) {
                titleStartsWithTheBookCount += 1;
            }
        }

        assertTrue(titleStartsWithTheBookCount > 0);

        /* Test Default */
        JsonNode titleStartsWithTheBooks = getAsNode(String.format("/author/%s/books?filter[book.title][prefix]=The", asimovId));

        assertEquals(titleStartsWithTheBookCount, titleStartsWithTheBooks.get("data").size());

        /* Test RSQL Typed */
        titleStartsWithTheBooks = getAsNode(String.format("/author/%s/books?filter[book]=title==The*", asimovId));

        assertEquals(titleStartsWithTheBookCount, titleStartsWithTheBooks.get("data").size());
    }

    @Test
    void testNonRootFilterPrefixWithSpecialChars() throws JsonProcessingException {
        int titleStartsWithTheBookCount = 0;
        for (JsonNode node : thomasHarrisBooks.get("data")) {
            if (node.get("attributes").get("title").asText().startsWith("I'm")) {
                titleStartsWithTheBookCount += 1;
            }
        }

        assertTrue(titleStartsWithTheBookCount > 0);

        /* Test Default */
        JsonNode titleStartsWithTheBooks = getAsNode(String.format("/author/%s/books?filter[book.title][prefix]=I'm", thomasHarrisId));

        assertEquals(titleStartsWithTheBookCount, titleStartsWithTheBooks.get("data").size());

        /* Test RSQL Typed */
        titleStartsWithTheBooks = getAsNode(String.format("/author/%s/books?filter[book]=title=='I\\'m*'", thomasHarrisId));

        assertEquals(titleStartsWithTheBookCount, titleStartsWithTheBooks.get("data").size());
    }

    @Test
    @Tag("skipInMemory")
    void testNonRootFilterInfix() throws JsonProcessingException {
        int titleContainsTheBookCount = 0;
        for (JsonNode node : asimovBooks.get("data")) {
            if (node.get("attributes").get("title").asText().toLowerCase(Locale.ENGLISH).contains("the")) {
                titleContainsTheBookCount += 1;
            }
        }

        assertTrue(titleContainsTheBookCount > 0);

        /* Test Default */
        JsonNode titleContainsTheBooks = getAsNode(String.format("/author/%s/books?filter[book.title][infix]=the", asimovId));

        assertEquals(titleContainsTheBookCount, titleContainsTheBooks.get("data").size());

        /* Test RSQL Typed */
        titleContainsTheBooks = getAsNode(String.format("/author/%s/books?filter[book]=title==*the*", asimovId));

        assertEquals(titleContainsTheBookCount, titleContainsTheBooks.get("data").size());
    }

    @Test
    void testNonRootFilterWithInclude() throws JsonProcessingException {
        Set<String> authorIdsOfScienceFiction = new HashSet<>();

        for (JsonNode book : asimovBooks.get("data")) {
            if (book.get("attributes").get("genre").asText().equals("Science Fiction")) {
                for (JsonNode author : book.get("relationships").get("authors").get("data")) {
                    authorIdsOfScienceFiction.add(author.get("id").asText());
                }
            }
        }

        assertTrue(CollectionUtils.isNotEmpty(authorIdsOfScienceFiction));

        /* Test Default */
        JsonNode result = getAsNode(String.format("/author/%s/books?include=authors&filter[book.genre]=Science Fiction", asimovId));

        for (JsonNode author : result.get("included")) {
            assertTrue(authorIdsOfScienceFiction.contains(author.get("id").asText()));
        }

        /* Test RSQL Typed */
        result = getAsNode(String.format("/author/%s/books?include=authors&filter[book]=genre=='Science Fiction'", asimovId));

        for (JsonNode author : result.get("included")) {
            assertTrue(authorIdsOfScienceFiction.contains(author.get("id").asText()));
        }
    }

    @Test
    void testNonRootFilterIsNull() throws JsonProcessingException {
        Set<JsonNode> bookIdsWithNullGenre = new HashSet<>();

        for (JsonNode book : nullNedBooks.get("data")) {
            if (book.get("attributes").get("genre").isNull()) {
                bookIdsWithNullGenre.add(book.get("id"));
            }
        }

        assertTrue(CollectionUtils.isNotEmpty(bookIdsWithNullGenre));

        /* Test Default */
        JsonNode result = getAsNode(String.format("/author/%s/books?filter[book.genre][isnull]", nullNedId));

        assertEquals(result.get("data").size(), bookIdsWithNullGenre.size());

        for (JsonNode book : result.get("data")) {
            assertTrue(book.get("attributes").get("genre").isNull());
            assertTrue(bookIdsWithNullGenre.contains(book.get("id")));
        }

        /* Test RSQL Typed */
        /* param = true */
        result = getAsNode(String.format("/author/%s/books?filter[book]=genre=isnull=true", nullNedId));

        assertEquals(result.get("data").size(), bookIdsWithNullGenre.size());

        for (JsonNode book : result.get("data")) {
            assertTrue(book.get("attributes").get("genre").isNull());
            assertTrue(bookIdsWithNullGenre.contains(book.get("id")));
        }

        /* param = 1 */
        result = getAsNode(String.format("/author/%s/books?filter[book]=genre=isnull=1", nullNedId));

        assertEquals(result.get("data").size(), bookIdsWithNullGenre.size());

        for (JsonNode book : result.get("data")) {
            assertTrue(book.get("attributes").get("genre").isNull());
            assertTrue(bookIdsWithNullGenre.contains(book.get("id")));
        }
    }

    @Test
    void testNonRootFilterIsNotNull() throws JsonProcessingException {
        Set<JsonNode> bookIdsWithNonNullGenre = new HashSet<>();

        for (JsonNode book : nullNedBooks.get("data")) {
            if (!book.get("attributes").get("genre").isNull()) {
                bookIdsWithNonNullGenre.add(book.get("id"));
            }
        }

        assertTrue(CollectionUtils.isNotEmpty(bookIdsWithNonNullGenre));

        /* Test Default */
        JsonNode result = getAsNode(String.format("/author/%s/books?filter[book.genre][notnull]", nullNedId));

        assertEquals(result.get("data").size(), bookIdsWithNonNullGenre.size());

        for (JsonNode book : result.get("data")) {
            assertFalse(book.get("attributes").get("genre").isNull());
            assertTrue(bookIdsWithNonNullGenre.contains(book.get("id")));
        }

        /* Test RSQL Typed */
        /* param = false */
        result = getAsNode(String.format("/author/%s/books?filter[book]=genre=isnull=false", nullNedId));

        assertEquals(result.get("data").size(), bookIdsWithNonNullGenre.size());

        for (JsonNode book : result.get("data")) {
            assertFalse(book.get("attributes").get("genre").isNull());
            assertTrue(bookIdsWithNonNullGenre.contains(book.get("id")));
        }

        /* param = 0 */
        result = getAsNode(String.format("/author/%s/books?filter[book]=genre=isnull=0", nullNedId));

        assertEquals(result.get("data").size(), bookIdsWithNonNullGenre.size());

        for (JsonNode book : result.get("data")) {
            assertFalse(book.get("attributes").get("genre").isNull());
            assertTrue(bookIdsWithNonNullGenre.contains(book.get("id")));
        }
    }

    @Test
    void testPublishDateGreaterThanFilter() throws JsonProcessingException {
        Set<JsonNode> bookIdsWithNonNullGenre = new HashSet<>();
        long publishDate;

        for (JsonNode book : nullNedBooks.get("data")) {
            if (!book.get("attributes").get("genre").isNull()) {
                bookIdsWithNonNullGenre.add(book.get("id"));
            }
        }

        assertTrue(CollectionUtils.isNotEmpty(bookIdsWithNonNullGenre));

        /* Test Default */
        JsonNode result = getAsNode("/book?filter[book.publishDate][gt]=1");

        assertEquals(result.get("data").size(), 1);

        for (JsonNode book : result.get("data")) {
            publishDate = book.get("attributes").get("publishDate").asLong();
            assertTrue(publishDate > 1L);
        }

        /* Test RSQL Typed */
        result = getAsNode("/book?filter[book]=publishDate>1");

        assertEquals(result.get("data").size(), 1);

        for (JsonNode book : result.get("data")) {
            publishDate = book.get("attributes").get("publishDate").asLong();
            assertTrue(publishDate > 1L);
        }
    }

    @Test
    void testPublishDateEqualsFilter() throws JsonProcessingException {
        /* Test Default */
        JsonNode result = getAsNode("/book?filter[book.publishDate][in]=0,1454638927412");

        assertEquals(8, result.get("data").size());

        /* Test RSQL Typed */
        result = getAsNode("/book?filter[book]=publishDate=in=(0,1454638927412)");

        assertEquals(result.get("data").size(), 8);
    }

    @Test
    void testPublishDatePrefixFilter() throws JsonProcessingException {
        /* Test Default */
        /* publish date = 1454638927412 */
        JsonNode result = getAsNode("/book?filter[book.publishDate][prefix]=1");

        assertEquals(1, result.get("data").size());

        /* Test RSQL Typed */
        /* publish date = 1454638927412 */
        result = getAsNode("/book?filter[book]=publishDate==1*");

        assertEquals(result.get("data").size(), 1);
    }

    @Test
    void testPublishDateInfixFilter() throws JsonProcessingException {
        /* Test Default */
        /* publish date = 1454638927412 */
        JsonNode result = getAsNode("/book?filter[book.publishDate][infix]=389");

        assertEquals(1, result.get("data").size());

        /* Test RSQL Typed */
        /* publish date = 1454638927412 */
        result = getAsNode("/book?filter[book]=publishDate==*389*");

        assertEquals(result.get("data").size(), 1);
    }

    @Test
    void testPublishDatePostfixFilter() throws JsonProcessingException {
        /* Test Default */
        /* publish date = 1454638927412 */
        JsonNode result = getAsNode("/book?filter[book.publishDate][postfix]=412");

        assertEquals(1, result.get("data").size());

        /* Test RSQL Typed */
        /* publish date = 1454638927412 */
        result = getAsNode("/book?filter[book]=publishDate==*412");

        assertEquals(result.get("data").size(), 1);
    }

    @Test
    void testPublishDateGreaterThanFilterSubRecord() throws JsonProcessingException {
        long publishDate;

        /* Test Default */
        JsonNode result = getAsNode(String.format("/author/%s/books?filter[book.publishDate][gt]=1454638927411", orsonCardId));

        assertTrue(result.get("data").size() > 0);

        for (JsonNode book : result.get("data")) {
            publishDate = book.get("attributes").get("publishDate").asLong();
            assertTrue(publishDate > 1454638927411L);
        }

        /* Test RSQL Typed */
        result = getAsNode(String.format("/author/%s/books?filter[book]=publishDate>1454638927411", orsonCardId));

        assertTrue(result.get("data").size() > 0);

        for (JsonNode book : result.get("data")) {
            publishDate = book.get("attributes").get("publishDate").asLong();
            assertTrue(publishDate > 1454638927411L);
        }
    }

    @Test
    void testPublishDateLessThanOrEqualsFilterSubRecord() throws JsonProcessingException {
        long publishDate;

        /* Test Default */
        JsonNode result = getAsNode(String.format("/author/%s/books?filter[book.publishDate][le]=1454638927412", orsonCardId));

        assertEquals(result.get("data").size(), 1);

        for (JsonNode book : result.get("data")) {
            publishDate = book.get("attributes").get("publishDate").asLong();
            assertTrue(publishDate <= 1454638927412L);
        }

        /* Test RSQL Typed */
        result = getAsNode(String.format("/author/%s/books?filter[book]=publishDate<=1454638927412", orsonCardId));

        assertEquals(result.get("data").size(), 1);

        for (JsonNode book : result.get("data")) {
            publishDate = book.get("attributes").get("publishDate").asLong();
            assertTrue(publishDate <= 1454638927412L);
        }
    }

    @Test
    void testPublishDateLessThanOrEqual() throws JsonProcessingException {
        long publishDate;

        /* Test Default */
        JsonNode result = getAsNode("book?filter[book.publishDate][le]=1454638927412");

        assertTrue(result.get("data").size() > 0);

        for (JsonNode book : result.get("data")) {
            publishDate = book.get("attributes").get("publishDate").asLong();
            assertTrue(publishDate <= 1454638927412L);
        }

        /* Test RSQL Typed */
        result = getAsNode("book?filter[book]=publishDate<=1454638927412");

        assertTrue(result.get("data").size() > 0);

        for (JsonNode book : result.get("data")) {
            publishDate = book.get("attributes").get("publishDate").asLong();
            assertTrue(publishDate <= 1454638927412L);
        }

        /* Test RSQL Global */
        result = getAsNode("book?filter=publishDate<=1454638927412");

        assertTrue(result.get("data").size() > 0);

        for (JsonNode book : result.get("data")) {
            publishDate = book.get("attributes").get("publishDate").asLong();
            assertTrue(publishDate <= 1454638927412L);
        }
    }

    @Test
    void testPublishDateLessThanFilter() throws JsonProcessingException {
        long publishDate;

        /* Test Default */
        JsonNode result = getAsNode("/book?filter[book.publishDate][lt]=1454638927411");

        assertTrue(result.get("data").size() > 0);

        for (JsonNode book : result.get("data")) {
            publishDate = book.get("attributes").get("publishDate").asLong();
            assertTrue(publishDate < 1454638927411L);
        }

        /* RSQL Typed */
        result = getAsNode("/book?filter[book]=publishDate<1454638927411");

        assertTrue(result.get("data").size() > 0);

        for (JsonNode book : result.get("data")) {
            publishDate = book.get("attributes").get("publishDate").asLong();
            assertTrue(publishDate < 1454638927411L);
        }

        /* RSQL Global */
        result = getAsNode("/book?filter=publishDate<1454638927411");

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
    void testIssue508() throws JsonProcessingException {
        JsonNode result = getAsNode("book?filter=(authors.name=='Thomas Harris',publisher.name=='Default publisher')&page[totals]");

        assertEquals(2, result.get("data").size());

        JsonNode pageNode = result.get("meta").get("page");
        assertNotNull(pageNode);
        assertEquals(pageNode.get("totalRecords").asInt(), 2);

        result = getAsNode("book?filter=(authors.name=='Thomas Harris')&page[totals]");
        assertEquals(2, result.get("data").size());

        pageNode = result.get("meta").get("page");
        assertNotNull(pageNode);
        assertEquals(pageNode.get("totalRecords").asInt(), 2);

        result = getAsNode("book?filter=(publisher.name=='Default publisher')&page[totals]");
        assertEquals(1, result.get("data").size());

        pageNode = result.get("meta").get("page");
        assertNotNull(pageNode);
        assertEquals(pageNode.get("totalRecords").asInt(), 1);
    }

    @Test
    void testGetBadRelationshipNameWithNestedFieldFilter() throws JsonProcessingException {
        /* Test Default */
        JsonNode result = getAsNode(
                "book?filter[book.author12.name]=Null Ned", HttpStatus.SC_BAD_REQUEST);

        assertEquals(result.get("errors").get(0).get("detail").asText(),
                "Unknown field in filter: author12\n"
                        + "Invalid query parameter: filter[book.author12.name]");

        /* Test RSQL Global */
        result = getAsNode("book?filter=author12.name=='Null Ned'", HttpStatus.SC_BAD_REQUEST);

        assertEquals(result.get("errors").get(0).get("detail").asText(),
                "Invalid filter format: filter\n"
                        + "No such association author12 for type book\n"
                        + "Invalid filter format: filter\n"
                        + "Invalid query parameter: filter");
    }

    @Test
    void testGetBooksFilteredByAuthors() throws JsonProcessingException {
        /* Test Default */
        JsonNode result = getAsNode("book?filter[book.authors.name]=Null Ned");

        assertEquals(result.get("data").size(), nullNedBooks.get("data").size());

        for (JsonNode book : result.get("data")) {
            String authorId = book.get("relationships").get("authors").get("data").get(0).get("id").asText();
            assertEquals(authorId, nullNedId);
        }

        /* Test RSQL Global */
        result = getAsNode("book?filter=authors.name=='Null Ned'");

        assertEquals(result.get("data").size(), nullNedBooks.get("data").size());

        for (JsonNode book : result.get("data")) {
            String authorId = book.get("relationships").get("authors").get("data").get(0).get("id").asText();
            assertEquals(authorId, nullNedId);
        }
    }

    @Test
    void testGetBooksFilteredByAuthorsId() throws JsonProcessingException {
        String nullNedIdStr = String.valueOf(nullNedId);
        /* Test Default */
        JsonNode result = getAsNode("book?filter[book.authors.id]=" + nullNedIdStr);

        assertEquals(result.get("data").size(), nullNedBooks.get("data").size());

        for (JsonNode book : result.get("data")) {
            String authorId = book.get("relationships").get("authors").get("data").get(0).get("id").asText();
            assertEquals(authorId, nullNedId);
        }

        /* Test RSQL Global */
        result = getAsNode("book?filter=authors.id==" + nullNedIdStr);

        assertEquals(result.get("data").size(), nullNedBooks.get("data").size());

        for (JsonNode book : result.get("data")) {
            String authorId = book.get("relationships").get("authors").get("data").get(0).get("id").asText();
            assertEquals(authorId, nullNedId);
        }
    }

    @Test
    void testGetBooksFilteredByAuthorAndTitle() throws JsonProcessingException {
        /* Test Default */
        JsonNode result = getAsNode("book?filter[book.authors.name]=Null Ned&filter[book.title]=Life with Null Ned");

        assertEquals(result.get("data").size(), 1);
        assertEquals(result.get("data").get(0).get("attributes").get("title").asText(), "Life with Null Ned");
        assertEquals(
                result.get("data").get(0).get("relationships").get("authors").get("data").get(0).get("id").asText(),
                nullNedId);

        /* Test RSQL Global */
        result = getAsNode("book?filter=authors.name=='Null Ned';title=='Life with Null Ned'");

        assertEquals(result.get("data").size(), 1);
        assertEquals(result.get("data").get(0).get("attributes").get("title").asText(), "Life with Null Ned");
        assertEquals(
                result.get("data").get(0).get("relationships").get("authors").get("data").get(0).get("id").asText(),
                nullNedId);
    }

    @Test
    void testFilterAuthorsByBookChapterTitle() throws JsonProcessingException {
        /* Test Default */
        JsonNode result = getAsNode("/author?sort=-name&filter[author.books.chapters.title][in]=Viva la Roma!,Mamma mia I wantz some pizza!");

        assertEquals(result.get("data").size(), 2);

        for (JsonNode author : result.get("data")) {
            String name = author.get("attributes").get("name").asText();
            assertTrue(name.equals("Isaac Asimov") || name.equals("Null Ned"));
        }

        /* Test RSQL Global */
        result = getAsNode("/author?filter=books.chapters.title=in=('Viva la Roma!','Mamma mia I wantz some pizza!')");

        assertEquals(result.get("data").size(), 2);

        for (JsonNode author : result.get("data")) {
            String name = author.get("attributes").get("name").asText();
            assertTrue(name.equals("Isaac Asimov") || name.equals("Null Ned"));
        }
    }

    @Test
    void testFilterAuthorBookByPublisher() throws JsonProcessingException {
        /* Test default */
        JsonNode result = getAsNode(String.format("/author/%s/books?filter[book.publisher.name]=Default publisher", hemingwayId));
        JsonNode data = result.get("data");
        assertEquals(data.size(), 1);

        for (JsonNode book : data) {
            String name = book.get("attributes").get("title").asText();
            assertEquals("The Old Man and the Sea", name);
        }

        /* Test RSQL */
        result = getAsNode(String.format("/author/%s/books?filter[book]=publisher.name=='Default publisher'", hemingwayId));

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
        given()
                .get("/book?filter[book]=editor.firstName=='John'")
                .then()
                .statusCode(org.apache.http.HttpStatus.SC_OK)
                .body("data.attributes.title", contains("The Old Man and the Sea"))
                .body("data", hasSize(1));


        given()
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

        given()
                .get("/editor?filter[editor]=fullName=='John Doe'")
                .then()
                .statusCode(org.apache.http.HttpStatus.SC_OK)
                .body("data.attributes.firstName", contains("John"))
                .body("data.attributes.lastName", contains("Doe"))
                .body("data", hasSize(1));

        given()
                .get("/editor?filter[editor]=fullName=='Foobar'")
                .then()
                .statusCode(org.apache.http.HttpStatus.SC_OK)
                .body("data", hasSize(0));
    }

    @Test
    void testFilterByAuthorBookByChapter() {
        /* Test default */
        given()
                .get(String.format("/author/%s/books?filter[book.chapters.title]=Viva la Roma!", asimovId))
                .then()
                .statusCode(org.apache.http.HttpStatus.SC_OK)
                .body("data", hasSize(1));

        /* Test RSQL */
        given()
                .get(String.format("/author/%s/books?filter[book]=chapters.title=='Viva la Roma!'", asimovId))
                .then()
                .statusCode(org.apache.http.HttpStatus.SC_OK)
                .body("data", hasSize(1));

        /* Test default */
        given()
                .get(String.format("/author/%s/books?filter[book.chapters.title]=None", hemingwayId))
                .then()
                .statusCode(org.apache.http.HttpStatus.SC_OK)
                .body("data", hasSize(0));

        /* Test RSQL */
        given()
                .get(String.format("/author/%s/books?filter[book]=chapters.title=='None'", hemingwayId))
                .then()
                .statusCode(org.apache.http.HttpStatus.SC_OK)
                .body("data", hasSize(0));
    }

    @Test
    void testFilterBookByAuthorAddress() throws JsonProcessingException {
        /* Test default */
        JsonNode result = getAsNode("book?filter[book.authors.homeAddress]=main&include=authors");
        JsonNode data = result.get("data");
        assertEquals(0, data.size(), result.toString());

        /* Test RSQL */
        result = getAsNode("book?filter=authors.homeAddress=='main'");
        data = result.get("data");
        assertEquals(0, data.size(), result.toString());
    }


    @Test
    void testGetBadRelationshipRoot() throws JsonProcessingException {
        /* Test Default */
        JsonNode result = getAsNode(
                "/author?filter[idontexist.books.title][in]=Viva la Roma!,Mamma mia I wantz some pizza!",
                HttpStatus.SC_BAD_REQUEST);
        assertEquals(result.get("errors").get(0).get("detail").asText(),
                "Unknown entity in filter: idontexist\n"
                        + "Invalid query parameter: filter[idontexist.books.title][in]");

        /* Test RSQL Global */
        result = getAsNode(
                "/author?filter=idontexist.books.title=in=('Viva la Roma!','Mamma mia I wantz some pizza!')",
                HttpStatus.SC_BAD_REQUEST);
        assertEquals(result.get("errors").get(0).get("detail").asText(),
                "Invalid filter format: filter\n"
                        + "No such association idontexist for type author\n"
                        + "Invalid filter format: filter\n"
                        + "Invalid query parameter: filter");
    }

    @Test
    void testGetBadRelationshipIntermediate() throws JsonProcessingException {
        /* Test Default */
        JsonNode result = getAsNode(
                "/author?filter[author.idontexist.title][in]=Viva la Roma!,Mamma mia I wantz some pizza!",
                HttpStatus.SC_BAD_REQUEST);
        assertEquals(result.get("errors").get(0).get("detail").asText(),
                "Unknown field in filter: idontexist\n"
                        + "Invalid query parameter: filter[author.idontexist.title][in]");

        /* Test RSQL Global */
        result = getAsNode(
                "/author?filter=idontexist.title=in=('Viva la Roma!','Mamma mia I wantz some pizza!')",
                HttpStatus.SC_BAD_REQUEST);
        assertEquals(result.get("errors").get(0).get("detail").asText(),
                "Invalid filter format: filter\n"
                        + "No such association idontexist for type author\n"
                        + "Invalid filter format: filter\n"
                        + "Invalid query parameter: filter");
    }

    @Test
    void testGetBadRelationshipLeaf() throws JsonProcessingException {
        /* Test Default */
        JsonNode result = getAsNode(
                "/author?filter[author.books.idontexist][in]=Viva la Roma!,Mamma mia I wantz some pizza!",
                HttpStatus.SC_BAD_REQUEST);
        assertEquals(result.get("errors").get(0).get("detail").asText(),
                "Unknown field in filter: idontexist\n"
                        + "Invalid query parameter: filter[author.books.idontexist][in]");

        /* Test RSQL Global */
        result = getAsNode(
                "/author?filter=books.idontexist=in=('Viva la Roma!','Mamma mia I wantz some pizza!')",
                HttpStatus.SC_BAD_REQUEST);
        assertEquals(result.get("errors").get(0).get("detail").asText(),
                "Invalid filter format: filter\n"
                        + "No such association idontexist for type book\n"
                        + "Invalid filter format: filter\n"
                        + "Invalid query parameter: filter");
    }

    /*
     * Verify that a combination of filters and order by generate working SQL.
     */
    @Test
    void testFilterWithSort() throws JsonProcessingException {
        JsonNode result = getAsNode(String.format("/author/%s/books?filter[book.title][notnull]=true&sort=title", asimovId));
        JsonNode data = result.get("data");
        assertEquals(data.size(), 2);
    }

    @Test
    void testBetweenOperatorOnRoot() throws JsonProcessingException {
        JsonNode result = getAsNode("/book?filter[book.id][between]=2,4");
        JsonNode data = result.get("data");
        assertEquals(3, data.size());
        for (JsonNode book : data) {
            assertTrue(Arrays.asList(2, 3, 4).contains(book.get("id").asInt()));
        }

        result = getAsNode("/book?filter=id=notbetween=(2,4);id!=7");
        data = result.get("data");
        assertEquals(4, data.size());
        for (JsonNode book : data) {
            assertTrue(Arrays.asList(1, 5, 6, 8).contains(book.get("id").asInt()));
        }
    }

    @Test
    void testBetweenOperatorOnNonRoot() throws JsonProcessingException {
        JsonNode result = getAsNode(String.format("/author/%s/books?filter[book.id][notbetween]=6,7", nullNedId));
        JsonNode data = result.get("data");
        assertEquals(1, data.size());
        for (JsonNode book : data) {
            assertEquals(8, book.get("id").asInt());
        }

        result = getAsNode(String.format("/author/%s/books?filter[book]=id=between=(6,7)", nullNedId));
        data = result.get("data");
        assertEquals(1, data.size());
        for (JsonNode book : data) {
            assertEquals(7, book.get("id").asInt());
        }
    }


    @Test
    void testIsEmptyRelationshipOnRoot() throws JsonProcessingException {
        //Book has ToMany relationship with chapter
        Set<JsonNode> bookIdsWithEmptyChapters = new HashSet<>();
        JsonNode result;

        for (JsonNode book : books.get("data")) {
            if (book.get("relationships").get("chapters").get("data").isEmpty()) {
                bookIdsWithEmptyChapters.add(book.get("id"));
            }
        }

        assertTrue(CollectionUtils.isNotEmpty(bookIdsWithEmptyChapters));

        /* Test Default */
        result = getAsNode("/book?filter[book.chapters][isempty]");

        assertEquals(bookIdsWithEmptyChapters.size(), result.get("data").size());


        for (JsonNode book : result.get("data")) {
            assertTrue(book.get("relationships").get("chapters").get("data").isEmpty());
            assertTrue(bookIdsWithEmptyChapters.contains(book.get("id")));
        }

        /* Test RSQL Typed */
        /* param = true */
        result = getAsNode("/book?filter[book]=chapters=isempty=true");

        assertEquals(result.get("data").size(), bookIdsWithEmptyChapters.size());

        for (JsonNode book : result.get("data")) {
            assertTrue(book.get("relationships").get("chapters").get("data").isEmpty());
            assertTrue(bookIdsWithEmptyChapters.contains(book.get("id")));
        }

        /* param = 1 */
        // Global Expression
        result = getAsNode(String.format("/book?filter=chapters=isempty=1"));

        assertEquals(result.get("data").size(), bookIdsWithEmptyChapters.size());

        for (JsonNode book : result.get("data")) {
            assertTrue(book.get("relationships").get("chapters").get("data").isEmpty());
            assertTrue(bookIdsWithEmptyChapters.contains(book.get("id")));
        }

    }

    @Test
    void testNotEmptyRelationshipOnNonRoot() throws JsonProcessingException {
        //Book has ToMany relationship with chapter
        Set<JsonNode> bookIdsWithNonEmptyChapters = new HashSet<>();
        JsonNode result;
        for (JsonNode book : nullNedBooks.get("data")) {
            if (!book.get("relationships").get("chapters").get("data").isEmpty()) {
                bookIdsWithNonEmptyChapters.add(book.get("id"));
            }
        }

        assertTrue(CollectionUtils.isNotEmpty(bookIdsWithNonEmptyChapters));

        /* Test Default */
        result = getAsNode(String.format("/author/%s/books?filter[book.chapters][notempty]", nullNedId));

        assertEquals(bookIdsWithNonEmptyChapters.size(), result.get("data").size());


        for (JsonNode book : result.get("data")) {
            assertFalse(book.get("relationships").get("chapters").get("data").isEmpty());
            assertTrue(bookIdsWithNonEmptyChapters.contains(book.get("id")));
        }

        /* Test RSQL Typed */
        /* param = false */
        result = getAsNode(String.format("/author/%s/books?filter[book]=chapters=isempty=false", nullNedId));

        assertEquals(result.get("data").size(), bookIdsWithNonEmptyChapters.size());

        for (JsonNode book : result.get("data")) {
            assertFalse(book.get("relationships").get("chapters").get("data").isEmpty());
            assertTrue(bookIdsWithNonEmptyChapters.contains(book.get("id")));
        }

        /* param = 0 */
        result = getAsNode(String.format("/author/%s/books?filter[book]=chapters=isempty=0", nullNedId));

        assertEquals(result.get("data").size(), bookIdsWithNonEmptyChapters.size());

        for (JsonNode book : result.get("data")) {
            assertFalse(book.get("relationships").get("chapters").get("data").isEmpty());
            assertTrue(bookIdsWithNonEmptyChapters.contains(book.get("id")));
        }

    }

    @Test
    @Tag("excludeOnHibernate3")
    @Tag("excludeOnHibernate5")
    @Tag("excludeOnJPA")
    void testNotEmptyAttributeOnRoot() throws JsonProcessingException {
        Set<JsonNode> bookIdsWithNonEmptyAwards = new HashSet<>();
        JsonNode result;

        for (JsonNode book : books.get("data")) {
            if (!book.get("attributes").get("awards").isEmpty()) {
                bookIdsWithNonEmptyAwards.add(book.get("id"));
            }
        }

        assertTrue(CollectionUtils.isNotEmpty(bookIdsWithNonEmptyAwards));

        /* Test Default */
        result = getAsNode("/book?filter[book.awards][notempty]");

        assertEquals(bookIdsWithNonEmptyAwards.size(), result.get("data").size());


        for (JsonNode book : result.get("data")) {
            assertFalse(book.get("attributes").get("awards").isEmpty());
            assertTrue(bookIdsWithNonEmptyAwards.contains(book.get("id")));
        }

        /* Test RSQL Typed */
        /* param = true */
        //Typed Expression
        result = getAsNode("/book?filter[book]=awards=isempty=false");

        assertEquals(result.get("data").size(), bookIdsWithNonEmptyAwards.size());

        for (JsonNode book : result.get("data")) {
            assertFalse(book.get("attributes").get("awards").isEmpty());
            assertTrue(bookIdsWithNonEmptyAwards.contains(book.get("id")));
        }

        /* param = 1 */
        //Global Expression
        result = getAsNode(String.format("/book?filter=awards=isempty=0"));

        assertEquals(result.get("data").size(), bookIdsWithNonEmptyAwards.size());

        for (JsonNode book : result.get("data")) {
            assertFalse(book.get("attributes").get("awards").isEmpty());
            assertTrue(bookIdsWithNonEmptyAwards.contains(book.get("id")));
        }

    }

    @Test
    @Tag("excludeOnHibernate3")
    @Tag("excludeOnHibernate5")
    @Tag("excludeOnJPA")
    void testIsEmptyAttributesOnNonRoot() throws JsonProcessingException {
        Set<JsonNode> bookIdsWithEmptyAwards = new HashSet<>();
        JsonNode result;
        for (JsonNode book : nullNedBooks.get("data")) {
            if (book.get("attributes").get("awards").isEmpty()) {
                bookIdsWithEmptyAwards.add(book.get("id"));
            }
        }

        assertTrue(CollectionUtils.isNotEmpty(bookIdsWithEmptyAwards));

        /* Test Default */
        result = getAsNode(String.format("/author/%s/books?filter[book.awards][isempty]", nullNedId));

        assertEquals(bookIdsWithEmptyAwards.size(), result.get("data").size());


        for (JsonNode book : result.get("data")) {
            assertTrue(book.get("attributes").get("awards").isEmpty());
            assertTrue(bookIdsWithEmptyAwards.contains(book.get("id")));
        }

        /* Test RSQL Typed */
        /* param = false */
        result = getAsNode(String.format("/author/%s/books?filter[book]=awards=isempty=true", nullNedId));

        assertEquals(result.get("data").size(), bookIdsWithEmptyAwards.size());

        for (JsonNode book : result.get("data")) {
            assertTrue(book.get("attributes").get("awards").isEmpty());
            assertTrue(bookIdsWithEmptyAwards.contains(book.get("id")));
        }

        /* param = 0 */
        result = getAsNode(String.format("/author/%s/books?filter[book]=awards=isempty=1", nullNedId));

        assertEquals(result.get("data").size(), bookIdsWithEmptyAwards.size());

        for (JsonNode book : result.get("data")) {
            assertTrue(book.get("attributes").get("awards").isEmpty());
            assertTrue(bookIdsWithEmptyAwards.contains(book.get("id")));
        }

    }

    @Test
    void testExceptionOnEmptyOperator() throws JsonProcessingException {
        JsonNode result;
        // Typed Expression
        result = getAsNode(String.format("/author/%s/books?filter[book.authors.name][notempty]", nullNedId), HttpStatus.SC_BAD_REQUEST);
        assertEquals(
                "Invalid predicate: book.authors.name NOTEMPTY []\n"
                        + "Invalid query parameter: filter[book.authors.name][notempty]\n"
                        + "Invalid toMany join. toMany association has to be the target collection.book.authors.name NOTEMPTY []\n"
                        + "Invalid query parameter: filter[book.authors.name][notempty]",
                result.get("errors").get(0).get("detail").asText()
        );

        //RSQL
        result = getAsNode(String.format("/author/%s/books?filter[book]=authors.name=isempty=true", nullNedId), HttpStatus.SC_BAD_REQUEST);
        assertEquals(
                "Invalid filter format: filter[book]\n"
                        + "Invalid query parameter: filter[book]\n"
                        + "Invalid filter format: filter[book]\n"
                        + "Invalid association authors.name. toMany association has to be the target collection.",
                result.get("errors").get(0).get("detail").asText()
        );
    }

    @Test
    @Tag("excludeOnHibernate3")
    void testMemberOfOnAttributes() {
        String filterString = "Booker Prize";
        Set<String> awardBook = new HashSet<>();
        Set<String> nullNedAwardBook = new HashSet<>();


        // * Filter On Root Entity *
        for (JsonNode book : books.get("data")) {
            Iterator<JsonNode> awards = book.get("attributes").get("awards").elements();
            while (awards.hasNext()) {
                if (awards.next().asText().equals(filterString)) {
                    awardBook.add(book.get("id").asText());
                    break;
                }
            }
        }
        // Test Default filter type on Root Entity
        when()
                .get(String.format("/book?filter[book.awards][hasmember]=%s", filterString))
                .then()
                .body("data", hasSize(awardBook.size()),
                        "data.id", contains(awardBook.toArray())
                );


        // Test RSQL type filter on Root Entity
        when()
                .get(String.format("/book?filter[book]=awards=hasmember=\"%s\"", filterString))
                .then()
                .body("data", hasSize(awardBook.size()),
                        "data.id", contains(awardBook.toArray())
                );


        // * Filter On Non Root Entity *
        for (JsonNode book : nullNedBooks.get("data")) {
            Iterator<JsonNode> awards = book.get("attributes").get("awards").elements();
            while (awards.hasNext()) {
                if (awards.next().asText().equals(filterString)) {
                    nullNedAwardBook.add(book.get("id").asText());
                    break;
                }
            }
        }
// Disabled for bug in Hibernate - https://hibernate.atlassian.net/browse/HHH-13990
//        // Test Default filter type on NonRoot Entity
//        when()
//                .get(String.format("/author/%s/books?filter[book.awards][hasmember]=%s", nullNedId, filterString))
//                .then()
//                .body("data", hasSize(nullNedAwardBook.size()),
//                        "data.id", contains(nullNedAwardBook.toArray())
//                );
//
//
//
//        // Test RSQL type filter on NonRoot Entity
//        when()
//                .get(String.format("/author/%s/books?filter[book]=awards=hasmember=\"%s\"", nullNedId, filterString))
//                .then()
//                .body("data", hasSize(nullNedAwardBook.size()),
//                        "data.id", contains(nullNedAwardBook.toArray())
//                );
    }

    @Test
    @Tag("excludeOnHibernate3")
    void testMembertoOneRelationships() {
        String phoneNumber = "987-654-3210";
        Set<String> publisherBook = new HashSet<>();


        // * Filter On Root Entity *
        for (JsonNode book : books.get("data")) {
            int publisherId = book.get("relationships").get("publisher").get("data").get("id").asInt();
            if (publisherId == 1) {
                publisherBook.add(book.get("id").asText());
                break;
            }
        }
        // Test Default filter type on Root Entity
        when()
                .get(String.format("/book?filter[book.publisher.phoneNumbers][hasmember]=%s", phoneNumber))
                .then()
                .body("data", hasSize(publisherBook.size()),
                        "data.id", contains(publisherBook.toArray())
                );

        // Test RSQL type filter on Root Entity
        when()
                .get(String.format("/book?filter[book]=publisher.phoneNumbers=hasmember=\"%s\"", phoneNumber))
                .then()
                .body("data", hasSize(publisherBook.size()),
                        "data.id", contains(publisherBook.toArray())
                );

    }

    @Test
    void testRootMemberOfToManyRelationshipConjunction() {
        /* RSQL Global */
        when()
                .get("/book?filter=authors.id=hasmember=1;authors.id=hasmember=2")
                .then()
                .body("data", hasSize(1))
                .body("data.relationships.authors[0].data.id[0]", equalTo("1"))
                .body("data.relationships.authors[0].data.id[1]", equalTo("2"));
    }

    @Test
    void testRootMemberOfToManyRelationship() {
        /* RSQL Global */
        when()
                .get("/book?filter=authors.id=hasmember=1")
                .then()
                .body("data.relationships.authors", hasSize(2))
                .body("data.relationships.authors[0].data.id[0]", equalTo("1"))
                .body("data.relationships.authors[1].data.id[0]", equalTo("1"));


        /* RSQL Typed */
        when()
                .get("/book?filter[book]=authors.id=hasmember=1")
                .then()
                .body("data.relationships.authors", hasSize(2))
                .body("data.relationships.authors[0].data.id[0]", equalTo("1"))
                .body("data.relationships.authors[1].data.id[0]", equalTo("1"));

        /* Default */
        when()
                .get("/book?filter[book.authors.id][hasmember]=1")
                .then()
                .body("data.relationships.authors", hasSize(2))
                .body("data.relationships.authors[0].data.id[0]", equalTo("1"))
                .body("data.relationships.authors[1].data.id[0]", equalTo("1"));
    }

    @Test
    void testRootMemberOfAndNotMemberOfToManyRelationship() {
        /* RSQL Global */
        when()
                .get("/book?filter=authors.id=hasmember=1;authors.id=hasnomember=3")
                .then()
                .body("data.relationships.authors", hasSize(2))
                .body("data.relationships.authors[0].data.id[0]", equalTo("1"))
                .body("data.relationships.authors[1].data.id[0]", equalTo("1"));

        /* RSQL Typed */
        when()
                .get("/book?filter[book]=authors.id=hasmember=1;authors.id=hasnomember=3")
                .then()
                .body("data.relationships.authors", hasSize(2))
                .body("data.relationships.authors[0].data.id[0]", equalTo("1"))
                .body("data.relationships.authors[1].data.id[0]", equalTo("1"));

        /* Default */
        when()
                .get("/book?filter[book.authors.id][hasmember]=1&filter[book.authors.id][hasnomember]=3")
                .then()
                .body("data.relationships.authors", hasSize(2))
                .body("data.relationships.authors[0].data.id[0]", equalTo("1"))
                .body("data.relationships.authors[1].data.id[0]", equalTo("1"));
    }

    @Test
    void testSubcollectionComplexAttribute() throws Exception {
        JsonNode priceyBooks = getAsNode("/author/1/books?filter[book]=price.total>=10");

        assertEquals(2, priceyBooks.get("data").size());

        priceyBooks = getAsNode("/book?filter[book]=price.total<5");

        assertEquals(0, priceyBooks.get("data").size());
    }

    @Test
    void testSubcollectionNestedComplexAttribute() throws Exception {
        JsonNode priceyBooks = getAsNode("/author/1/books?filter[book]=price.currency.isoCode==AED");

        assertEquals(1, priceyBooks.get("data").size());

        priceyBooks = getAsNode("/author/1/books?filter[book]=price.currency.isoCode==ABC");

        assertEquals(0, priceyBooks.get("data").size());
    }

    @Test
    void testSubcollectionMemberOfToManyRelationship() {
        /* RSQL Typed */
        when()
                .get("/author/5/books?filter[book]=chapters.title=hasmember='Mamma mia I wantz some pizza!'")
                .then()
                .body("data.relationships.chapters", hasSize(1))
                .body("data.relationships.chapters[0].data.id[0]", equalTo("2"));

        /* Default */
        when()
                .get("/author/5/books?filter[book.chapters.title][hasmember]=Mamma mia I wantz some pizza!")
                .then()
                .body("data.relationships.chapters", hasSize(1))
                .body("data.relationships.chapters[0].data.id[0]", equalTo("2"));
    }

    @Test
    void testSubcollectionMemberOfToManyRelationshipConjunction() {
        /* RSQL Typed */
        when()
                .get("/author/5/books?filter[book]=chapters.title=hasmember='Mamma mia I wantz some pizza!';chapters.title=hasnomember='Foo'")
                .then()
                .body("data.relationships.chapters", hasSize(1))
                .body("data.relationships.chapters[0].data.id[0]", equalTo("2"));

        /* Default */
        when()
                .get("/author/5/books?filter[book.chapters.title][hasmember]=Mamma mia I wantz some pizza!&filter[book.chapters.title][hasnomember]=Foo")
                .then()
                .body("data.relationships.chapters", hasSize(1))
                .body("data.relationships.chapters[0].data.id[0]", equalTo("2"));
    }

    @Test
    @Tag("excludeOnHibernate3")
    void testExceptionOnMemberOfOperator() throws JsonProcessingException {
        JsonNode result;
        // Typed Expression
        result = getAsNode(String.format("/author/%s/books?filter[book.authors][hasmember]", nullNedId), HttpStatus.SC_BAD_REQUEST);
        assertEquals(
                "Invalid predicate: book.authors HASMEMBER []\n"
                        + "Invalid query parameter: filter[book.authors][hasmember]\n"
                        + "Invalid Path: Last Path Element cannot be a collection type\n"
                        + "Invalid query parameter: filter[book.authors][hasmember]",
                result.get("errors").get(0).get("detail").asText()
        );

        //RSQL
        result = getAsNode(String.format("/author/%s/books?filter[book]=authors=hasmember=true", nullNedId), HttpStatus.SC_BAD_REQUEST);
        assertEquals(
                "Invalid filter format: filter[book]\n"
                        + "Invalid query parameter: filter[book]\n"
                        + "Invalid filter format: filter[book]\n"
                        + "Invalid Path: Last Path Element cannot be a collection type",
                result.get("errors").get(0).get("detail").asText()
        );


        // Test RSQL type filter on Root Entity
        result = getAsNode(String.format("/book?filter[book]=publisher.name=hasmember=\"%s\"", "Default publisher"), HttpStatus.SC_BAD_REQUEST);
        assertEquals(
                "Invalid filter format: filter[book]\n"
                        + "Invalid query parameter: filter[book]\n"
                        + "Invalid filter format: filter[book]\n"
                        + "Invalid Path: Last Path Element has to be a collection type",
                result.get("errors").get(0).get("detail").asText()
        );
        result = getAsNode(String.format("/book?filter[book]=title=hasnomember=\"%s\"", "*The*"), HttpStatus.SC_BAD_REQUEST);
        assertEquals(
                "Invalid filter format: filter[book]\n"
                        + "Invalid query parameter: filter[book]\n"
                        + "Invalid filter format: filter[book]\n"
                        + "Invalid Path: Last Path Element has to be a collection type",
                result.get("errors").get(0).get("detail").asText()
        );
        result = getAsNode(String.format("/book?filter[book.title][hasmember]=\"%s\"", "*The*"), HttpStatus.SC_BAD_REQUEST);
        assertEquals(
                "Invalid Path: Last Path Element has to be a collection type\n"
                        + "Invalid query parameter: filter[book.title][hasmember]",
                result.get("errors").get(0).get("detail").asText()
        );
    }

    @AfterAll
    void cleanUp() {
        for (int id : authorIds) {
            given()
                    .accept(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                    .delete("/author/" + id);
        }
        for (int id : bookIds) {
            given()
                    .accept(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                    .delete("/book/" + id);
        }
    }
}
