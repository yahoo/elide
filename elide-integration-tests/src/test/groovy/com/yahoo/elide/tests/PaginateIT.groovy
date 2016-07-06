/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.tests;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper
import com.jayway.restassured.RestAssured;
import com.yahoo.elide.initialization.AbstractIntegrationTestInitializer;
import org.testng.Assert
import org.testng.annotations.AfterTest
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test

/**
 * Tests for pagination
 */
public class PaginateIT extends AbstractIntegrationTestInitializer {
    private final ObjectMapper mapper = new ObjectMapper()

    private JsonNode books = null
    private JsonNode authors = null
    private String asimovId = null
    private JsonNode asimovBooks = null
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
                .patch("/")

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
                                },
                                {
                                  "type": "book",
                                  "id": "23451234-1234-1234-1234-1234567890ac"
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
                      },
                      {
                        "op": "add",
                        "path": "/book",
                        "value": {
                          "type": "book",
                          "id": "23451234-1234-1234-1234-1234567890ac",
                          "attributes": {
                            "title": "Enders Shadow",
                            "genre": "Science Fiction",
                            "language": "English",
                            "publishDate": 1464638927412
                          }
                        }
                      }
                    ]
                    ''')
                .patch("/")

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
                      }
                    ]
                    ''')
                .patch("/")

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
                      }
                    ]
                    ''')
                .patch("/")

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
        }

        for (JsonNode book : books.get("data")) {
            bookIds.add(book.get("id").asInt());
        }

        Assert.assertNotNull(asimovId)
        Assert.assertNotNull(nullNedId)

        asimovBooks = mapper.readTree(RestAssured.get("/author/${asimovId}/books").asString())
        nullNedBooks = mapper.readTree(RestAssured.get("/author/${nullNedId}/books").asString())
    }

    @Test
    public void testNoFilterSortDescPaginationFirstPage() {
        def result = mapper.readTree(
                RestAssured.get("/book?sort=-title&page[size]=3").asString())
        Assert.assertTrue(result.get("data").size() == 3);

        final JsonNode books = result.get("data");
        final String firstBookName = books.get(0).get("attributes").get("title").asText();
        Assert.assertEquals(firstBookName, "The Roman Republic");
    }

    @Test
    public void testPaginationOnSubRecords() {
        final String httpResult = RestAssured.get("/author/${orsonCardId}/books?sort=-title&page[size]=1").asString();
        def result = mapper.readTree(httpResult);
        Assert.assertEquals(result.get("data").size(), 1);
        JsonNode book = result.get("data").get(0);
        Assert.assertEquals(book.get("attributes").get("title").asText(), "Enders Shadow");
    }

    @Test
    public void testNoFilterSortDescPagination() {
        def result = mapper.readTree(
                RestAssured.get("/book?sort=-title&page[number]=2&page[size]=3").asString())
        Assert.assertTrue(result.get("data").size() == 3);

        final JsonNode books = result.get("data");
        final String firstBookName = books.get(0).get("attributes").get("title").asText();
        Assert.assertEquals(firstBookName, "Life with Null Ned");
    }

    @Test
    public void testNoFilterMultiSortPagination() {
        //select * from book order by title desc, genre asc;
        def result = mapper.readTree(
                RestAssured.get("/book?sort=-title,genre&page[size]=3").asString())
        Assert.assertTrue(result.get("data").size() == 3);

        final JsonNode books = result.get("data");
        final String firstBookName = books.get(0).get("attributes").get("title").asText();
        Assert.assertEquals(firstBookName, "The Roman Republic"); // has a null genre, should be first.
    }

    @Test
    public void testPublishDateLessThanFilter() {
        def result = mapper.readTree(
                RestAssured.get("/book?filter[book.publishDate][lt]=1454638927411&page[size]=2").asString())
        Assert.assertEquals(result.get("data").size(), 2);
        for (JsonNode book : result.get("data")) {
            // we should be starting off at index 4 here - this is true. However due to limitations of the test environment
            // there is no way we can check based on index since results could have been added many times...
            // should clean up in an after test...
            long publishDate = book.get("attributes").get("publishDate").asLong();
            Assert.assertTrue(publishDate < 1454638927411L);
        }
    }

    @Test
    public void testPageAndSortOnSubRecords() {
        def result = mapper.readTree(RestAssured.get("/author/${orsonCardId}/books?sort=-title,publishDate&page[size]=1").asString());
        //select * from book join book_author on book.`id` = book_author.`books_id` where book_author.`authors_id` = 2 order by book.`title` desc, book.`publishDate` asc limit 1;
        Assert.assertEquals(result.get("data").size(), 1);
        JsonNode book = result.get("data").get(0);
        long publishDate = book.get("attributes").get("publishDate").asLong();
        Assert.assertEquals(publishDate, 1464638927412L);
        String authorIdFromRelation = book.get("relationships").get("authors").get("data").get(0).get("id").asText();
        Assert.assertEquals(authorIdFromRelation, orsonCardId); // ensure we always have a bound relationship
    }

    @Test
    public void testPageAndSortOnSubRecordsPageTwo() {
        def result = mapper.readTree(RestAssured.get("/author/${orsonCardId}/books?sort=-title&page[number]=2&page[size]=1").asString());
        Assert.assertEquals(result.get("data").size(), 1);
        JsonNode book = result.get("data").get(0);
        String title = book.get("attributes").get("title").asText();
        Assert.assertEquals(title, "Enders Game");
        String authorIdFromRelation = book.get("relationships").get("authors").get("data").get(0).get("id").asText();
        Assert.assertEquals(authorIdFromRelation, orsonCardId); // ensure we always have a bound relationship
    }

    @Test
    public void testPageAndSortShouldFailOnBadSortFields() {
        def result = mapper.readTree(RestAssured.get("/author/${orsonCardId}/books?sort=-title,publishDate,onion&page[size]=1").asString());
        Assert.assertEquals(result.get("errors").size(), 1);
        JsonNode errors = result.get("errors").get(0);

        String errorMsg = errors.asText();

        Assert.assertEquals(errorMsg, "InvalidValueException: Invalid value: Book doesn't contain the field onion");
    }

    @Test
    public void testBasicPageBasedPagination() {
        def result = mapper.readTree(
                RestAssured.get("/book?page[number]=2&page[size]=2").asString())
        Assert.assertTrue(result.get("data").size() == 2);
    }

    @Test
    public void testBasicOffsetBasedPagination() {
        def result = mapper.readTree(
                RestAssured.get("/book?page[offset]=3&page[limit]=2").asString())
        Assert.assertTrue(result.get("data").size() == 2);
    }

    @Test
    public void testPaginationOffsetOnly() {
        def result = mapper.readTree(
                RestAssured.get("/book?page[offset]=3").asString())
        Assert.assertTrue(result.get("data").size() == 5);
    }

    @Test
    public void testPaginationSizeOnly() {
        def result = mapper.readTree(
                RestAssured.get("/book?page[size]=2").asString())
        Assert.assertTrue(result.get("data").size() == 2);
    }

    @Test(priority = -1)
    public void testPaginationOffsetWithSorting() {
        def result = mapper.readTree(
                RestAssured.get("/book?sort=title&page[offset]=3").asString())
        Assert.assertTrue(result.get("data").size() == 5);

        final JsonNode books = result.get("data");
        final String firstBookName = books.get(0).get("attributes").get("title").asText();
        Assert.assertEquals(firstBookName, "Foundation");
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
