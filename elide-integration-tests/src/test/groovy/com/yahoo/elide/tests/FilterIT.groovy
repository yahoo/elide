/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.tests
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.jayway.restassured.RestAssured
import com.yahoo.elide.core.HttpStatus
import com.yahoo.elide.initialization.AbstractIntegrationTestInitializer
import org.testng.Assert
import org.testng.annotations.AfterTest
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test
/**
 * Tests for Filters
 */
class FilterIT extends AbstractIntegrationTestInitializer {
    private final ObjectMapper mapper = new ObjectMapper()

    private JsonNode books = null
    private JsonNode authors = null
    private String asimovId = null
    private JsonNode asimovBooks = null
    private String thomasHarrisId = null
    private JsonNode thomasHarrisBooks = null
    private String nullNedId = null
    private JsonNode nullNedBooks = null
    private String orsonCardId = null
    private Set<Integer> bookIds = new HashSet<>()
    private Set<Integer> authorIds = new HashSet<>()

    @BeforeClass
    public void setup() {
        RestAssured
                .given()
                .contentType("application/vnd.api+json; ext=jsonpatch")
                .accept("application/vnd.api+json; ext=jsonpatch")
                .body('''
                    [
                      {
                        "op": "add",
                        "path": "/author",
                        "value": {
                          "id": "12345678-1234-1234-1234-1234567890ab",
                          "type": "author",
                          "attributes": {
                            "name": "Ernest Hemingway"
                          },
                          "relationships": {
                            "books": {
                              "data": [
                                {
                                  "type": "book",
                                  "id": "12345678-1234-1234-1234-1234567890ac"
                                },
                                {
                                  "type": "book",
                                  "id": "12345678-1234-1234-1234-1234567890ad"
                                }
                              ]
                            }
                          }
                        }
                      },
                      {
                        "op": "add",
                        "path": "/book",
                        "value": {
                          "type": "book",
                          "id": "12345678-1234-1234-1234-1234567890ac",
                          "attributes": {
                            "title": "The Old Man and the Sea",
                            "genre": "Literary Fiction",
                            "language": "English"
                          }
                        }
                      },
                      {
                        "op": "add",
                        "path": "/book",
                        "value": {
                          "type": "book",
                          "id": "12345678-1234-1234-1234-1234567890ad",
                          "attributes": {
                            "title": "For Whom the Bell Tolls",
                            "genre": "Literary Fiction",
                            "language": "English"
                          }
                        }
                      }
                    ]
                    ''')
                .patch("/").then().statusCode(HttpStatus.SC_OK)

        RestAssured
                .given()
                .contentType("application/vnd.api+json; ext=jsonpatch")
                .accept("application/vnd.api+json; ext=jsonpatch")
                .body('''
                    [
                      {
                        "op": "add",
                        "path": "/author",
                        "value": {
                          "id": "12345678-1234-1234-1234-1234567890ab",
                          "type": "author",
                          "attributes": {
                            "name": "Thomas Harris"
                          },
                          "relationships": {
                            "books": {
                              "data": [
                                {
                                  "type": "book",
                                  "id": "12345678-1234-1234-1234-1234567890ac"
                                }
                              ]
                            }
                          }
                        }
                      },
                      {
                        "op": "add",
                        "path": "/book",
                        "value": {
                          "type": "book",
                          "id": "12345678-1234-1234-1234-1234567890ac",
                          "attributes": {
                            "title": "I'm OK - You're OK",
                            "genre": "Psychology & Counseling",
                            "language": "English"
                          }
                        }
                      }
                    ]
                    ''')
                .patch("/").then().statusCode(HttpStatus.SC_OK)

        RestAssured
                .given()
                .contentType("application/vnd.api+json; ext=jsonpatch")
                .accept("application/vnd.api+json; ext=jsonpatch")
                .body('''
                    [
                      {
                        "op": "add",
                        "path": "/author",
                        "value": {
                          "id": "12345679-1234-1234-1234-1234567890ab",
                          "type": "author",
                          "attributes": {
                            "name": "Orson Scott Card"
                          },
                          "relationships": {
                            "books": {
                              "data": [
                                {
                                  "type": "book",
                                  "id": "12345679-1234-1234-1234-1234567890ac"
                                }
                              ]
                            }
                          }
                        }
                      },
                      {
                        "op": "add",
                        "path": "/book",
                        "value": {
                          "type": "book",
                          "id": "12345679-1234-1234-1234-1234567890ac",
                          "attributes": {
                            "title": "Enders Game",
                            "genre": "Science Fiction",
                            "language": "English",
                            "publishDate": 1454638927412
                          }
                        }
                      }
                    ]
                    ''')
                .patch("/").then().statusCode(HttpStatus.SC_OK)

        RestAssured
                .given()
                .contentType("application/vnd.api+json; ext=jsonpatch")
                .accept("application/vnd.api+json; ext=jsonpatch")
                .body('''
                    [
                      {
                        "op": "add",
                        "path": "/author",
                        "value": {
                          "id": "12345680-1234-1234-1234-1234567890ab",
                          "type": "author",
                          "attributes": {
                            "name": "Isaac Asimov"
                          },
                          "relationships": {
                            "books": {
                              "data": [
                                {
                                  "type": "book",
                                  "id": "12345680-1234-1234-1234-1234567890ac"
                                },
                                {
                                  "type": "book",
                                  "id": "12345680-1234-1234-1234-1234567890ad"
                                }
                              ]
                            }
                          }
                        }
                      },
                      {
                        "op": "add",
                        "path": "/book",
                        "value": {
                          "type": "book",
                          "id": "12345680-1234-1234-1234-1234567890ac",
                          "attributes": {
                            "title": "Foundation",
                            "genre": "Science Fiction",
                            "language": "English"
                          }
                        }
                      },
                      {
                        "op": "add",
                        "path": "/book",
                        "value": {
                          "type": "book",
                          "id": "12345680-1234-1234-1234-1234567890ad",
                          "attributes": {
                            "title": "The Roman Republic",
                            "genre": "History",
                            "language": "English"
                          }
                        }
                      },
                      {
                        "op": "add",
                        "path": "/book/12345680-1234-1234-1234-1234567890ad/chapters",
                        "value": {
                          "type": "chapter",
                          "id": "12345680-1234-1234-1234-1234567890ae",
                          "attributes": {
                            "title": "Viva la Roma!"
                          }
                        }
                      }
                    ]
                    ''')
                .patch("/").then().statusCode(HttpStatus.SC_OK)

        RestAssured
                .given()
                .contentType("application/vnd.api+json; ext=jsonpatch")
                .accept("application/vnd.api+json; ext=jsonpatch")
                .body('''
                    [
                      {
                        "op": "add",
                        "path": "/author",
                        "value": {
                          "id": "12345681-1234-1234-1234-1234567890ab",
                          "type": "author",
                          "attributes": {
                            "name": "Null Ned"
                          },
                          "relationships": {
                            "books": {
                              "data": [
                                {
                                  "type": "book",
                                  "id": "12345681-1234-1234-1234-1234567890ac"
                                },
                                {
                                  "type": "book",
                                  "id": "12345681-1234-1234-1234-1234567890ad"
                                }
                              ]
                            }
                          }
                        }
                      },
                      {
                        "op": "add",
                        "path": "/book",
                        "value": {
                          "type": "book",
                          "id": "12345681-1234-1234-1234-1234567890ac",
                          "attributes": {
                            "title": "Life with Null Ned",
                            "language": "English"
                          }
                        }
                      },
                      {
                        "op": "add",
                        "path": "/book",
                        "value": {
                          "type": "book",
                          "id": "12345681-1234-1234-1234-1234567890ad",
                          "attributes": {
                            "title": "Life with Null Ned 2",
                            "genre": "Not Null",
                            "language": "English"
                          }
                        }
                      },
                      {
                        "op": "add",
                        "path": "/book/12345681-1234-1234-1234-1234567890ad/chapters",
                        "value": {
                          "type": "chapter",
                          "id": "12345680-1234-1234-1234-1234567890ae",
                          "attributes": {
                            "title": "Mamma mia I wantz some pizza!"
                          }
                        }
                      }
                    ]
                    ''')
                .patch("/").then().statusCode(HttpStatus.SC_OK)

        books = mapper.readTree(RestAssured.get("/book").asString())
        authors = mapper.readTree(RestAssured.get("/author").asString())

        for (JsonNode author : authors.get("data")) {
            authorIds.add(author.get("id").asInt());
            if (author.get("attributes").get("name").asText() == "Isaac Asimov") {
                asimovId = author.get("id").asText()
            }

            if (author.get("attributes").get("name").asText() == "Null Ned") {
                nullNedId = author.get("id").asText()
            }

            if (author.get("attributes").get("name").asText() == "Orson Scott Card") {
                orsonCardId = author.get("id").asText();
            }

            if (author.get("attributes").get("name").asText() == "Thomas Harris") {
                thomasHarrisId = author.get("id").asText();
            }
        }

        for (JsonNode book : books.get("data")) {
            bookIds.add(book.get("id").asInt());
        }

        Assert.assertNotNull(asimovId)
        Assert.assertNotNull(nullNedId)
        Assert.assertNotNull(thomasHarrisId)

        asimovBooks = mapper.readTree(RestAssured.get("/author/${asimovId}/books").asString())
        nullNedBooks = mapper.readTree(RestAssured.get("/author/${nullNedId}/books").asString())
        thomasHarrisBooks = mapper.readTree(RestAssured.get("/author/${thomasHarrisId}/books").asString())
    }

    @Test
    public void testRootFilterInvalidField() {
        /* Test Default */
        RestAssured
                .get("/book?filter[book.name]=Ignored")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)

        /* Test RSQL typed */
        RestAssured
                .get("/book?filter[book]=name==Ignored")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)

        /* Test RSQL global */
        RestAssured
                .get("/book?filter=name==Ignored")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
    }

    @Test
    public void testRootFilterInvalidEntity() {
        /* Test Default */
        RestAssured
                .get("/book?filter[bank.title]=Ignored")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)

        /* Test RSQL typed */
        RestAssured
                .get("/book?filter[bank]=title==Ignored")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
    }

    @Test
    public void testRootInvalidOperator() {
        /* Test Default */
        RestAssured
                .get("/book?filter[book.title][invalid]=Ignored")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)

        /* Test RSQL Typed */
        RestAssured
                .get("/book?filter[book]=title=invalid=Ignored")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)

         /* Test RSQL Global */
        RestAssured
                .get("/book?filter=title=invalid=Ignored")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
    }

    @Test
    public void testFilterInvalidField() {
        /* Test Default */
        RestAssured
                .get("/author/3/book?filter[book.name]=Ignored")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)

        /* Test RSQL Typed */
        RestAssured
                .get("/author/3/book?filter[book]=name==Ignored")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
    }

    @Test
    public void testFilterInvalidEntity() {
        /* Test Default */
        RestAssured
                .get("/author/3/book?filter[bank.title]=Ignored")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)

        /* Test RSQL Typed */
        RestAssured
                .get("/author/3/book?filter[bank]=title==Ignored")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
    }

    @Test
    public void testInvalidOperator() {
        /* Test Default */
        RestAssured
                .get("/author/3/book?filter[book.title][invalid]=Ignored")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)

        /* Test RSQL Typed */
        RestAssured
                .get("/author/3/book?filter[book]=title=invalid=Ignored")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
    }

    @Test
    public void testRootFilterImplicitSingle() {
        int scienceFictionBookCount = 0
        for (JsonNode node : books.get("data")) {
            if (node.get("attributes").get("genre").asText().equals("Science Fiction")) {
                scienceFictionBookCount += 1
            }
        }

        Assert.assertTrue(scienceFictionBookCount > 0)

        /* Test Default */
        def scienceFictionBooks = mapper.readTree(
                RestAssured.get("/book?filter[book.genre]=Science Fiction").asString())

        Assert.assertEquals(scienceFictionBookCount, scienceFictionBooks.get("data").size())

        /* Test RSQL Typed */
        scienceFictionBooks = mapper.readTree(
                RestAssured.get("/book?filter[book]=genre=='Science Fiction'").asString())

        Assert.assertEquals(scienceFictionBookCount, scienceFictionBooks.get("data").size())

        /* Test RSQL Global */
        scienceFictionBooks = mapper.readTree(
                RestAssured.get("/book?filter=genre=='Science Fiction'").asString())

        Assert.assertEquals(scienceFictionBookCount, scienceFictionBooks.get("data").size())

    }

    @Test
    public void testRootFilterInSingle() {
        int literaryFictionBookCount = 0
        for (JsonNode node : books.get("data")) {
            if (node.get("attributes").get("genre").asText().equals("Literary Fiction")) {
                literaryFictionBookCount += 1
            }
        }

        Assert.assertTrue(literaryFictionBookCount > 0)

        /* Test Default */
        def literaryFictionBooks = mapper.readTree(
                RestAssured.get("/book?filter[book.genre][in]=Literary Fiction").asString())

        Assert.assertEquals(literaryFictionBookCount, literaryFictionBooks.get("data").size())

        /* Test RSQL Typed */
        literaryFictionBooks = mapper.readTree(
                RestAssured.get("/book?filter[book]=genre=in='Literary Fiction'").asString())

        Assert.assertEquals(literaryFictionBookCount, literaryFictionBooks.get("data").size())

        /* Test RSQL Global */
        literaryFictionBooks = mapper.readTree(
                RestAssured.get("/book?filter=genre=in='Literary Fiction'").asString())

        Assert.assertEquals(literaryFictionBookCount, literaryFictionBooks.get("data").size())
    }

    @Test
    public void testRootFilterNotInSingle() {
        int nonLiteraryFictionBookCount = 0
        for (JsonNode node : books.get("data")) {
            if (!node.get("attributes").get("genre").isNull()
                    && !node.get("attributes").get("genre").asText().equals("Literary Fiction")) {
                nonLiteraryFictionBookCount += 1
            }
        }

        Assert.assertTrue(nonLiteraryFictionBookCount > 0)

        /* Test Default */
        def nonLiteraryFictionBooks = mapper.readTree(
                RestAssured.get("/book?filter[book.genre][not]=Literary Fiction").asString())

        Assert.assertEquals(nonLiteraryFictionBookCount, nonLiteraryFictionBooks.get("data").size())

        /* Test RSQL typed */
        nonLiteraryFictionBooks = mapper.readTree(
                RestAssured.get("/book?filter[book]=genre!='Literary Fiction'").asString())

        Assert.assertEquals(nonLiteraryFictionBookCount, nonLiteraryFictionBooks.get("data").size())

        /* Test RSQL global */
        nonLiteraryFictionBooks = mapper.readTree(
                RestAssured.get("/book?filter=genre!='Literary Fiction'").asString())

        Assert.assertEquals(nonLiteraryFictionBookCount, nonLiteraryFictionBooks.get("data").size())
    }

    @Test
    public void testRootFilterNotInMultiple() {
        int nonFictionBookCount = 0
        for (JsonNode node : books.get("data")) {
            if (!node.get("attributes").get("genre").isNull()
                    && !node.get("attributes").get("genre").asText().equals("Literary Fiction")
                    && !node.get("attributes").get("genre").asText().equals("Science Fiction")) {
                nonFictionBookCount += 1
            }
        }

        Assert.assertTrue(nonFictionBookCount > 0)

        /* Test default */
        def nonFictionBooks = mapper.readTree(
                RestAssured.get("/book?filter[book.genre][not]=Literary Fiction,Science Fiction").asString())

        Assert.assertEquals(nonFictionBookCount, nonFictionBooks.get("data").size())

        /* Test RSQL typed */
        nonFictionBooks = mapper.readTree(
                RestAssured.get("/book?filter[book]=genre=out=('Literary Fiction','Science Fiction')").asString())

        Assert.assertEquals(nonFictionBookCount, nonFictionBooks.get("data").size())

        /* Test RSQL global */
        nonFictionBooks = mapper.readTree(
                RestAssured.get("/book?filter=genre=out=('Literary Fiction','Science Fiction')").asString())

        Assert.assertEquals(nonFictionBookCount, nonFictionBooks.get("data").size())
    }

    @Test
    public void testRootFilterInMultipleSingle() {
        int literaryAndScienceFictionBookCount = 0
        for (JsonNode node : books.get("data")) {
            if (node.get("attributes").get("genre").asText().equals("Literary Fiction")
                    || node.get("attributes").get("genre").asText().equals("Science Fiction")) {
                literaryAndScienceFictionBookCount += 1
            }
        }

        Assert.assertTrue(literaryAndScienceFictionBookCount > 0)

        /* Test Default */
        def literaryAndScienceFictionBooks = mapper.readTree(
                RestAssured.get("/book?filter[book.genre][in]=Literary Fiction,Science Fiction").asString())

        Assert.assertEquals(literaryAndScienceFictionBookCount, literaryAndScienceFictionBooks.get("data").size())

        /* Test RSQL typed */
        literaryAndScienceFictionBooks = mapper.readTree(
                RestAssured.get("/book?filter[book]=genre=in=('Literary Fiction','Science Fiction')").asString())

        Assert.assertEquals(literaryAndScienceFictionBookCount, literaryAndScienceFictionBooks.get("data").size())

        /* Test RSQL global */
        literaryAndScienceFictionBooks = mapper.readTree(
                RestAssured.get("/book?filter=genre=in=('Literary Fiction','Science Fiction')").asString())

        Assert.assertEquals(literaryAndScienceFictionBookCount, literaryAndScienceFictionBooks.get("data").size())
    }

    @Test
    public void testRootFilterPostfix() {
        int genreEndsWithFictionBookCount = 0
        for (JsonNode node : books.get("data")) {
            if (node.get("attributes").get("genre").asText().endsWith("Fiction")) {
                genreEndsWithFictionBookCount += 1
            }
        }

        Assert.assertTrue(genreEndsWithFictionBookCount > 0)

        /* Test Default */
        def genreEndsWithFictionBooks = mapper.readTree(
                RestAssured.get("/book?filter[book.genre][postfix]=Fiction").asString())

        Assert.assertEquals(genreEndsWithFictionBookCount, genreEndsWithFictionBooks.get("data").size())

        /* Test RSQL Typed */
        genreEndsWithFictionBooks = mapper.readTree(
                RestAssured.get("/book?filter[book]=genre==*Fiction").asString())

        Assert.assertEquals(genreEndsWithFictionBookCount, genreEndsWithFictionBooks.get("data").size())

        /* Test RSQL Global */
        genreEndsWithFictionBooks = mapper.readTree(
                RestAssured.get("/book?filter=genre==*Fiction").asString())

        Assert.assertEquals(genreEndsWithFictionBookCount, genreEndsWithFictionBooks.get("data").size())
    }

    @Test
    public void testRootFilterPrefix() {
        int titleStartsWithTheBookCount = 0
        for (JsonNode node : books.get("data")) {
            if (node.get("attributes").get("title").asText().startsWith("The")) {
                titleStartsWithTheBookCount += 1
            }
        }

        Assert.assertTrue(titleStartsWithTheBookCount > 0)

        /* Test Default */
        def titleStartsWithTheBooks = mapper.readTree(
                RestAssured.get("/book?filter[book.title][prefix]=The").asString())

        Assert.assertEquals(titleStartsWithTheBookCount, titleStartsWithTheBooks.get("data").size())

        /* Test RSQL Typed */
        titleStartsWithTheBooks = mapper.readTree(
                RestAssured.get("/book?filter[book]=title==The*").asString())

        Assert.assertEquals(titleStartsWithTheBookCount, titleStartsWithTheBooks.get("data").size())

        /* Test RSQL Global  */
        titleStartsWithTheBooks = mapper.readTree(
                RestAssured.get("/book?filter=title==The*").asString())

        Assert.assertEquals(titleStartsWithTheBookCount, titleStartsWithTheBooks.get("data").size())
    }

    @Test
    public void testRootFilterPrefixWithSpecialChars() {
        int titleStartsWithTheBookCount = 0
        for (JsonNode node : books.get("data")) {
            if (node.get("attributes").get("title").asText().startsWith("I'm")) {
                titleStartsWithTheBookCount += 1
            }
        }

        Assert.assertTrue(titleStartsWithTheBookCount > 0)

        /* Test Default */
        def titleStartsWithTheBooks = mapper.readTree(
                RestAssured.get("/book?filter[book.title][prefix]=I'm").asString())

        Assert.assertEquals(titleStartsWithTheBookCount, titleStartsWithTheBooks.get("data").size())

        /* Test RSQL Typed */
        titleStartsWithTheBooks = mapper.readTree(
                RestAssured.get("/book?filter[book]=title=='I\\'m*'").asString())

        Assert.assertEquals(titleStartsWithTheBookCount, titleStartsWithTheBooks.get("data").size())

        /* Test RSQL Global */
        titleStartsWithTheBooks = mapper.readTree(
                RestAssured.get("/book?filter=title=='I\\'m*'").asString())

        Assert.assertEquals(titleStartsWithTheBookCount, titleStartsWithTheBooks.get("data").size())
    }

    @Test
    public void testRootFilterInfix() {
        int titleContainsTheBookCount = 0
        for (JsonNode node : books.get("data")) {
            if (node.get("attributes").get("title").asText().toLowerCase().contains("the")) {
                titleContainsTheBookCount += 1
            }
        }

        Assert.assertTrue(titleContainsTheBookCount > 0)

        /* Test Default */
        def titleContainsTheBooks = mapper.readTree(
                RestAssured.get("/book?filter[book.title][infix]=the").asString())

        Assert.assertEquals(titleContainsTheBookCount, titleContainsTheBooks.get("data").size())

        /* Test RSQL Typed */
        titleContainsTheBooks = mapper.readTree(
                RestAssured.get("/book?filter[book]=title==*the*").asString())

        Assert.assertEquals(titleContainsTheBookCount, titleContainsTheBooks.get("data").size())

        /* Test RSQL Global */
        titleContainsTheBooks = mapper.readTree(
                RestAssured.get("/book?filter=title==*the*").asString())

        Assert.assertEquals(titleContainsTheBookCount, titleContainsTheBooks.get("data").size())
    }

    @Test
    public void testRootFilterWithInclude() {
        def authorIdsOfLiteraryFiction = [] as Set

        for (JsonNode book : books.get("data")) {
            if (book.get("attributes").get("genre").asText() == "Literary Fiction") {
                for (JsonNode author : book.get("relationships").get("authors").get("data")) {
                    authorIdsOfLiteraryFiction.add(author.get("id").asText())
                }
            }
        }

        Assert.assertTrue(authorIdsOfLiteraryFiction.size() > 0)

        /* Test Default */
        def result = mapper.readTree(
                RestAssured.get("/book?include=authors&filter[book.genre]=Literary Fiction").asString())

        for (JsonNode author : result.get("included")) {
            Assert.assertTrue(authorIdsOfLiteraryFiction.contains(author.get("id").asText()))
        }

        /* Test RSQL Typed */
        result = mapper.readTree(
                RestAssured.get("/book?include=authors&filter[book]=genre=='Literary Fiction'").asString())

        for (JsonNode author : result.get("included")) {
            Assert.assertTrue(authorIdsOfLiteraryFiction.contains(author.get("id").asText()))
        }

        /* Test RSQL Global */
        result = mapper.readTree(
                RestAssured.get("/book?include=authors&filter=genre=='Literary Fiction'").asString())

        for (JsonNode author : result.get("included")) {
            Assert.assertTrue(authorIdsOfLiteraryFiction.contains(author.get("id").asText()))
        }
    }

    @Test
    public void testRootFilterIsNull() {
        def bookIdsWithNullGenre = [] as Set

        for (JsonNode book : books.get("data")) {
            if (book.get("attributes").get("genre").isNull()) {
                bookIdsWithNullGenre.add(book.get("id"))
            }
        }

        Assert.assertTrue(bookIdsWithNullGenre.size() > 0)

        /* Test Default */
        def result = mapper.readTree(
                RestAssured.get("/book?filter[book.genre][isnull]").asString())

        Assert.assertEquals(result.get("data").size(), bookIdsWithNullGenre.size())

        for (JsonNode book : result.get("data")) {
            Assert.assertTrue(book.get("attributes").get("genre").isNull())
            Assert.assertTrue(bookIdsWithNullGenre.contains(book.get("id")))
        }
    }

    @Test
    public void testRootFilterIsNotNull() {
        def bookIdsWithNonNullGenre = [] as Set

        for (JsonNode book : books.get("data")) {
            if (!book.get("attributes").get("genre").isNull()) {
                bookIdsWithNonNullGenre.add(book.get("id"))
            }
        }

        Assert.assertTrue(bookIdsWithNonNullGenre.size() > 0)

        /* Test Default */
        def result = mapper.readTree(
                RestAssured.get("/book?filter[book.genre][notnull]").asString())

        Assert.assertEquals(result.get("data").size(), bookIdsWithNonNullGenre.size())

        for (JsonNode book : result.get("data")) {
            Assert.assertTrue(!book.get("attributes").get("genre").isNull())
            Assert.assertTrue(bookIdsWithNonNullGenre.contains(book.get("id")))
        }
    }

    @Test
    public void testNonRootFilterImplicitSingle() {
        int asimovScienceFictionBookCount = 0
        for (JsonNode node : asimovBooks.get("data")) {
            if (node.get("attributes").get("genre").asText().equals("Science Fiction")) {
                asimovScienceFictionBookCount += 1
            }
        }

        Assert.assertTrue(asimovScienceFictionBookCount > 0)

        /* Test Default */
        def asimovScienceFictionBooks = mapper.readTree(
                RestAssured.get("/author/${asimovId}/books?filter[book.genre]=Science Fiction").asString())

        Assert.assertEquals(asimovScienceFictionBookCount, asimovScienceFictionBooks.get("data").size())

        /* Test RSQL Typed */
        asimovScienceFictionBooks = mapper.readTree(
                RestAssured.get("/author/${asimovId}/books?filter[book]=genre=='Science Fiction'").asString())

        Assert.assertEquals(asimovScienceFictionBookCount, asimovScienceFictionBooks.get("data").size())
    }

    @Test
    public void testNonRootFilterInSingle() {
        int asimovHistoryBookCount = 0
        for (JsonNode node : asimovBooks.get("data")) {
            if (node.get("attributes").get("genre").asText().equals("History")) {
                asimovHistoryBookCount += 1
            }
        }

        Assert.assertTrue(asimovHistoryBookCount > 0)

        /* Test Default */
        def asimovHistoryBooks = mapper.readTree(
                RestAssured.get("/author/${asimovId}/books?filter[book.genre]=History").asString())

        Assert.assertEquals(asimovHistoryBookCount, asimovHistoryBooks.get("data").size())

        /* Test RSQL Typed */
        asimovHistoryBooks = mapper.readTree(
                RestAssured.get("/author/${asimovId}/books?filter[book]=genre==History").asString())

        Assert.assertEquals(asimovHistoryBookCount, asimovHistoryBooks.get("data").size())
    }

    @Test
    public void testNonRootFilterNotInSingle() {
        int nonHistoryBookCount = 0
        for (JsonNode node : asimovBooks.get("data")) {
            if (!node.get("attributes").get("genre").isNull()
                    && !node.get("attributes").get("genre").asText().equals("History")) {
                nonHistoryBookCount += 1
            }
        }

        Assert.assertTrue(nonHistoryBookCount > 0)

        /* Test Default */
        def nonHistoryBooks = mapper.readTree(
                RestAssured.get("/author/${asimovId}/books?filter[book.genre][not]=History").asString())

        Assert.assertEquals(nonHistoryBookCount, nonHistoryBooks.get("data").size())

        /* Test RSQL Typed */
        nonHistoryBooks = mapper.readTree(
                RestAssured.get("/author/${asimovId}/books?filter[book]=genre!=History").asString())

        Assert.assertEquals(nonHistoryBookCount, nonHistoryBooks.get("data").size())
    }

    @Test
    public void testNonRootFilterPostfix() {
        int genreEndsWithFictionBookCount = 0
        for (JsonNode node : asimovBooks.get("data")) {
            if (node.get("attributes").get("genre").asText().endsWith("Fiction")) {
                genreEndsWithFictionBookCount += 1
            }
        }

        Assert.assertTrue(genreEndsWithFictionBookCount > 0)

        /* Test Default */
        def genreEndsWithFictionBooks = mapper.readTree(
                RestAssured.get("/author/${asimovId}/books?filter[book.genre][postfix]=Fiction").asString())

        Assert.assertEquals(genreEndsWithFictionBookCount, genreEndsWithFictionBooks.get("data").size())

        /* Test RSQL Typed */
        genreEndsWithFictionBooks = mapper.readTree(
                RestAssured.get("/author/${asimovId}/books?filter[book]=genre==*Fiction").asString())

        Assert.assertEquals(genreEndsWithFictionBookCount, genreEndsWithFictionBooks.get("data").size())
    }

    @Test
    public void testNonRootFilterPrefix() {
        int titleStartsWithTheBookCount = 0
        for (JsonNode node : asimovBooks.get("data")) {
            if (node.get("attributes").get("title").asText().startsWith("The")) {
                titleStartsWithTheBookCount += 1
            }
        }

        Assert.assertTrue(titleStartsWithTheBookCount > 0)

        /* Test Default */
        def titleStartsWithTheBooks = mapper.readTree(
                RestAssured.get("/author/${asimovId}/books?filter[book.title][prefix]=The").asString())

        Assert.assertEquals(titleStartsWithTheBookCount, titleStartsWithTheBooks.get("data").size())

        /* Test RSQL Typed */
        titleStartsWithTheBooks = mapper.readTree(
                RestAssured.get("/author/${asimovId}/books?filter[book]=title==The*").asString())

        Assert.assertEquals(titleStartsWithTheBookCount, titleStartsWithTheBooks.get("data").size())
    }

    @Test
    public void testNonRootFilterPrefixWithSpecialChars() {
        int titleStartsWithTheBookCount = 0
        for (JsonNode node : thomasHarrisBooks.get("data")) {
            if (node.get("attributes").get("title").asText().startsWith("I'm")) {
                titleStartsWithTheBookCount += 1
            }
        }

        Assert.assertTrue(titleStartsWithTheBookCount > 0)

        /* Test Default */
        def titleStartsWithTheBooks = mapper.readTree(
                RestAssured.get("/author/${thomasHarrisId}/books?filter[book.title][prefix]=I'm").asString())

        Assert.assertEquals(titleStartsWithTheBookCount, titleStartsWithTheBooks.get("data").size())

        /* Test RSQL Typed */
        titleStartsWithTheBooks = mapper.readTree(
                RestAssured.get("/author/${thomasHarrisId}/books?filter[book]=title=='I\\'m*'").asString())

        Assert.assertEquals(titleStartsWithTheBookCount, titleStartsWithTheBooks.get("data").size())
    }

    @Test
    public void testNonRootFilterInfix() {
        int titleContainsTheBookCount = 0
        for (JsonNode node : asimovBooks.get("data")) {
            if (node.get("attributes").get("title").asText().toLowerCase().contains("the")) {
                titleContainsTheBookCount += 1
            }
        }

        Assert.assertTrue(titleContainsTheBookCount > 0)

        /* Test Default */
        def titleContainsTheBooks = mapper.readTree(
                RestAssured.get("/author/${asimovId}/books?filter[book.title][infix]=the").asString())

        Assert.assertEquals(titleContainsTheBookCount, titleContainsTheBooks.get("data").size())

        /* Test RSQL Typed */
        titleContainsTheBooks = mapper.readTree(
                RestAssured.get("/author/${asimovId}/books?filter[book]=title==*the*").asString())

        Assert.assertEquals(titleContainsTheBookCount, titleContainsTheBooks.get("data").size())
    }

    @Test
    public void testNonRootFilterWithInclude() {
        def authorIdsOfScienceFiction = [] as Set

        for (JsonNode book : asimovBooks.get("data")) {
            if (book.get("attributes").get("genre").asText() == "Science Fiction") {
                for (JsonNode author : book.get("relationships").get("authors").get("data")) {
                    authorIdsOfScienceFiction.add(author.get("id").asText())
                }
            }
        }

        Assert.assertTrue(authorIdsOfScienceFiction.size() > 0)

        /* Test Default */
        def result = mapper.readTree(
                RestAssured.get("/author/${asimovId}/books?include=authors&filter[book.genre]=Science Fiction").asString())

        for (JsonNode author : result.get("included")) {
            Assert.assertTrue(authorIdsOfScienceFiction.contains(author.get("id").asText()))
        }

        /* Test RSQL Typed */
        result = mapper.readTree(
                RestAssured.get("/author/${asimovId}/books?include=authors&filter[book]=genre=='Science Fiction'").asString())

        for (JsonNode author : result.get("included")) {
            Assert.assertTrue(authorIdsOfScienceFiction.contains(author.get("id").asText()))
        }
    }

    @Test
    public void testNonRootFilterIsNull() {
        def bookIdsWithNullGenre = [] as Set

        for (JsonNode book : nullNedBooks.get("data")) {
            if (book.get("attributes").get("genre").isNull()) {
                bookIdsWithNullGenre.add(book.get("id"))
            }
        }

        Assert.assertTrue(bookIdsWithNullGenre.size() > 0)

        /* Test Default */
        def result = mapper.readTree(
                RestAssured.get("/author/${nullNedId}/books?filter[book.genre][isnull]").asString())

        Assert.assertEquals(result.get("data").size(), bookIdsWithNullGenre.size())

        for (JsonNode book : result.get("data")) {
            Assert.assertTrue(book.get("attributes").get("genre").isNull())
            Assert.assertTrue(bookIdsWithNullGenre.contains(book.get("id")))
        }
    }

    @Test
    public void testNonRootFilterIsNotNull() {
        def bookIdsWithNonNullGenre = [] as Set

        for (JsonNode book : nullNedBooks.get("data")) {
            if (!book.get("attributes").get("genre").isNull()) {
                bookIdsWithNonNullGenre.add(book.get("id"))
            }
        }

        Assert.assertTrue(bookIdsWithNonNullGenre.size() > 0)

        /* Test Default */
        def result = mapper.readTree(
                RestAssured.get("/author/${nullNedId}/books?filter[book.genre][notnull]").asString())

        Assert.assertEquals(result.get("data").size(), bookIdsWithNonNullGenre.size())

        for (JsonNode book : result.get("data")) {
            Assert.assertTrue(!book.get("attributes").get("genre").isNull())
            Assert.assertTrue(bookIdsWithNonNullGenre.contains(book.get("id")))
        }
    }

    @Test
    public void testPublishDateGreaterThanFilter() {
        def bookIdsWithNonNullGenre = [] as Set
        for (JsonNode book : nullNedBooks.get("data")) {
            if (!book.get("attributes").get("genre").isNull()) {
                bookIdsWithNonNullGenre.add(book.get("id"))
            }
        }

        Assert.assertTrue(bookIdsWithNonNullGenre.size() > 0)

        /* Test Default */
        def result = mapper.readTree(
                RestAssured.get("/book?filter[book.publishDate][gt]=1").asString())
        Assert.assertEquals(result.get("data").size(), 1);
        for (JsonNode book : result.get("data")) {
            long publishDate = book.get("attributes").get("publishDate").asLong();
            Assert.assertTrue(publishDate > 1L);
        }

        /* Test RSQL Typed */
        result = mapper.readTree(
                RestAssured.get("/book?filter[book]=publishDate>1").asString())
        Assert.assertEquals(result.get("data").size(), 1);
        for (JsonNode book : result.get("data")) {
            long publishDate = book.get("attributes").get("publishDate").asLong();
            Assert.assertTrue(publishDate > 1L);
        }
    }

    @Test
    public void testPublishDateGreaterThanFilterSubRecord() {
        /* Test Default */
        def result = mapper.readTree(RestAssured.get("/author/${orsonCardId}/books?filter[book.publishDate][gt]=1454638927411").asString());
        Assert.assertTrue(result.get("data").size() > 0);
        for (JsonNode book : result.get("data")) {
            long publishDate = book.get("attributes").get("publishDate").asLong();
            Assert.assertTrue(publishDate > 1454638927411L);
        }

        /* Test RSQL Typed */
        result = mapper.readTree(RestAssured.get("/author/${orsonCardId}/books?filter[book]=publishDate>1454638927411").asString());
        Assert.assertTrue(result.get("data").size() > 0);
        for (JsonNode book : result.get("data")) {
            long publishDate = book.get("attributes").get("publishDate").asLong();
            Assert.assertTrue(publishDate > 1454638927411L);
        }
    }

    @Test
    public void testPublishDateLessThanOrEqualsFilterSubRecord() {
        /* Test Default */
        def result = mapper.readTree(RestAssured.get("/author/${orsonCardId}/books?filter[book.publishDate][le]=1454638927412").asString());
        Assert.assertEquals(result.get("data").size(), 1);
        for (JsonNode book : result.get("data")) {
            long publishDate = book.get("attributes").get("publishDate").asLong();
            Assert.assertTrue(publishDate <= 1454638927412L);
        }

        /* Test RSQL Typed */
        result = mapper.readTree(RestAssured.get("/author/${orsonCardId}/books?filter[book]=publishDate<=1454638927412").asString());
        Assert.assertEquals(result.get("data").size(), 1);
        for (JsonNode book : result.get("data")) {
            long publishDate = book.get("attributes").get("publishDate").asLong();
            Assert.assertTrue(publishDate <= 1454638927412L);
        }
    }

    @Test
    public void testPublishDateLessThanOrEqual() {
        /* Test Default */
        def result = mapper.readTree(RestAssured.get("book?filter[book.publishDate][le]=1454638927412").asString());
        Assert.assertTrue(result.get("data").size() > 0);
        for (JsonNode book : result.get("data")) {
            long publishDate = book.get("attributes").get("publishDate").asLong();
            Assert.assertTrue(publishDate <= 1454638927412L);
        }

        /* Test RSQL Typed */
        result = mapper.readTree(RestAssured.get("book?filter[book]=publishDate<=1454638927412").asString());
        Assert.assertTrue(result.get("data").size() > 0);
        for (JsonNode book : result.get("data")) {
            long publishDate = book.get("attributes").get("publishDate").asLong();
            Assert.assertTrue(publishDate <= 1454638927412L);
        }

        /* Test RSQL Global */
        result = mapper.readTree(RestAssured.get("book?filter=publishDate<=1454638927412").asString());
        Assert.assertTrue(result.get("data").size() > 0);
        for (JsonNode book : result.get("data")) {
            long publishDate = book.get("attributes").get("publishDate").asLong();
            Assert.assertTrue(publishDate <= 1454638927412L);
        }
    }

    @Test
    public void testPublishDateLessThanFilter() {
        def bookIdsWithNonNullGenre = [] as Set
        for (JsonNode book : nullNedBooks.get("data")) {
            if (!book.get("attributes").get("genre").isNull()) {
                bookIdsWithNonNullGenre.add(book.get("id"))
            }
        }

        /* Test Default */
        def result = mapper.readTree(
                RestAssured.get("/book?filter[book.publishDate][lt]=1454638927411").asString())
        Assert.assertTrue(result.get("data").size() > 0);
        for (JsonNode book : result.get("data")) {
            long publishDate = book.get("attributes").get("publishDate").asLong();
            Assert.assertTrue(publishDate < 1454638927411L);
        }

        /* RSQL Typed */
        result = mapper.readTree(
                RestAssured.get("/book?filter[book]=publishDate<1454638927411").asString())
        Assert.assertTrue(result.get("data").size() > 0);
        for (JsonNode book : result.get("data")) {
            long publishDate = book.get("attributes").get("publishDate").asLong();
            Assert.assertTrue(publishDate < 1454638927411L);
        }

        /* RSQL Global */
        result = mapper.readTree(
                RestAssured.get("/book?filter=publishDate<1454638927411").asString())
        Assert.assertTrue(result.get("data").size() > 0);
        for (JsonNode book : result.get("data")) {
            long publishDate = book.get("attributes").get("publishDate").asLong();
            Assert.assertTrue(publishDate < 1454638927411L);
        }
    }

    @Test
    public void testGetBadRelationshipNameWithNestedFieldFilter() {
        /* Test Default */
        def result = mapper.readTree(RestAssured.get("book?filter[book.author12.name]=Null Ned").asString());
        Assert.assertEquals(result.get("errors").get(0).asText(),
                "InvalidPredicateException: Unknown field in filter: author12\n" +
                "Invalid query parameter: filter[book.author12.name]");

        /* Test RSQL Global */
        result = mapper.readTree(RestAssured.get("book?filter=author12.name=='Null Ned'").asString());
        Assert.assertEquals(result.get("errors").get(0).asText(),
                "InvalidPredicateException: Invalid filter format: filter\n" +
                "No such association author12 for type book\n" +
                "Invalid filter format: filter\n" +
                "Invalid query parameter: filter");
    }

    @Test
    public void testGetBooksFilteredByAuthors() {
        /* Test Default */
        def result = mapper.readTree(RestAssured.get("book?filter[book.authors.name]=Null Ned").asString());
        Assert.assertEquals(result.get("data").size(), nullNedBooks.get("data").size());
        for (JsonNode book : result.get("data")) {
            String authorId = book.get("relationships").get("authors").get("data").get(0).get("id").asText();
            Assert.assertEquals(authorId, nullNedId);
        }

        /* Test RSQL Global */
        result = mapper.readTree(RestAssured.get("book?filter=authors.name=='Null Ned'").asString());
        Assert.assertEquals(result.get("data").size(), nullNedBooks.get("data").size());
        for (JsonNode book : result.get("data")) {
            String authorId = book.get("relationships").get("authors").get("data").get(0).get("id").asText();
            Assert.assertEquals(authorId, nullNedId);
        }
    }

    @Test
    public void testGetBooksFilteredByAuthorAndTitle() {
        /* Test Default */
        def result = mapper.readTree(RestAssured.get("book?filter[book.authors.name]=Null Ned&filter[book.title]=Life with Null Ned").asString());
        Assert.assertEquals(result.get("data").size(), 1);
        Assert.assertEquals(result.get("data").get(0).get("attributes").get("title").asText(), "Life with Null Ned");
        Assert.assertEquals(result.get("data").get(0).get("relationships").get("authors").get("data").get(0).get("id").asText(), nullNedId);

        /* Test RSQL Global */
        result = mapper.readTree(RestAssured.get("book?filter=authors.name=='Null Ned';title=='Life with Null Ned'").asString());
        Assert.assertEquals(result.get("data").size(), 1);
        Assert.assertEquals(result.get("data").get(0).get("attributes").get("title").asText(), "Life with Null Ned");
        Assert.assertEquals(result.get("data").get(0).get("relationships").get("authors").get("data").get(0).get("id").asText(), nullNedId);
    }

    @Test
    public void testFilterAuthorsByBookChapterTitle() {
        /* Test Default */
        def result = mapper.readTree(RestAssured.get("author?filter[author.books.chapters.title][in]=Viva la Roma!,Mamma mia I wantz some pizza!").asString());
        Assert.assertEquals(result.get("data").size(), 2);
        String last = null;
        for (JsonNode author : result.get("data")) {
            String name = author.get("attributes").get("name").asText();
            Assert.assertTrue(("Isaac Asimov".equals(name) || "Null Ned".equals(name)) && !name.equals(last));
        }

        /* Test RSQL Global */
        result = mapper.readTree(RestAssured.get("author?filter=books.chapters.title=in=('Viva la Roma!','Mamma mia I wantz some pizza!')").asString());
        Assert.assertEquals(result.get("data").size(), 2);
        last = null;
        for (JsonNode author : result.get("data")) {
            String name = author.get("attributes").get("name").asText();
            Assert.assertTrue(("Isaac Asimov".equals(name) || "Null Ned".equals(name)) && !name.equals(last));
        }
    }

    @Test
    public void testGetBadRelationshipRoot() {
        /* Test Default */
        def result = mapper.readTree(RestAssured.get("author?filter[idontexist.books.title][in]=Viva la Roma!,Mamma mia I wantz some pizza!").asString());
        Assert.assertEquals(result.get("errors").get(0).asText(),
                "InvalidPredicateException: Unknown entity in filter: idontexist\n" +
                        "Invalid query parameter: filter[idontexist.books.title][in]");

        /* Test RSQL Global */
        result = mapper.readTree(RestAssured.get("author?filter=idontexist.books.title=in=('Viva la Roma!','Mamma mia I wantz some pizza!')").asString());
        Assert.assertEquals(result.get("errors").get(0).asText(),
                "InvalidPredicateException: Invalid filter format: filter\n" +
                "No such association idontexist for type author\n" +
                "Invalid filter format: filter\n" +
                "Invalid query parameter: filter");
    }

    @Test
    public void testGetBadRelationshipIntermediate() {
        /* Test Default */
        def result = mapper.readTree(RestAssured.get("author?filter[author.idontexist.title][in]=Viva la Roma!,Mamma mia I wantz some pizza!").asString());
        Assert.assertEquals(result.get("errors").get(0).asText(),
                "InvalidPredicateException: Unknown field in filter: idontexist\n" +
                        "Invalid query parameter: filter[author.idontexist.title][in]");

        /* Test RSQL Global */
        result = mapper.readTree(RestAssured.get("author?filter=idontexist.title=in=('Viva la Roma!','Mamma mia I wantz some pizza!')").asString());
        Assert.assertEquals(result.get("errors").get(0).asText(),
                "InvalidPredicateException: Invalid filter format: filter\n" +
                "No such association idontexist for type author\n" +
                "Invalid filter format: filter\n" +
                "Invalid query parameter: filter");
    }

    @Test
    public void testGetBadRelationshipLeaf() {
        /* Test Default */
        def result = mapper.readTree(RestAssured.get("author?filter[author.books.idontexist][in]=Viva la Roma!,Mamma mia I wantz some pizza!").asString());
        Assert.assertEquals(result.get("errors").get(0).asText(),
                "InvalidPredicateException: Unknown field in filter: idontexist\n" +
                        "Invalid query parameter: filter[author.books.idontexist][in]");

        /* Test RSQL Global */
        result = mapper.readTree(RestAssured.get("author?filter=books.idontexist=in=('Viva la Roma!','Mamma mia I wantz some pizza!')").asString());
        Assert.assertEquals(result.get("errors").get(0).asText(),
                "InvalidPredicateException: Invalid filter format: filter\n" +
                        "No such association idontexist for type book\n" +
                        "Invalid filter format: filter\n" +
                        "Invalid query parameter: filter");
    }

    @AfterTest
    public void cleanUp() {
        for (int id : authorIds) {
            RestAssured
                    .given()
                    .accept("application/vnd.api+json; ext=jsonpatch")
                    .delete("/author/"+id)
        }
        for (int id : bookIds) {
            RestAssured
                    .given()
                    .accept("application/vnd.api+json; ext=jsonpatch")
                    .delete("/book/"+id)
        }
    }
}
