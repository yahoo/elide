/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.tests

import com.yahoo.elide.initialization.AbstractIntegrationTestInitializer

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.jayway.restassured.RestAssured

import org.testng.Assert
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test

/**
 * Tests for pagination
 */
public class SortingIT extends AbstractIntegrationTestInitializer {
    private final ObjectMapper mapper = new ObjectMapper()

    @BeforeClass
    public void setup() {
        def result = RestAssured
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
                          },
                          "relationships": {
                            "publisher": {
                                "data": {
                                    "type": "publisher",
                                    "id": "12345678-1234-1234-1234-1234567890ae"
                                }
                            }
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
                          },
                          "relationships": {
                            "publisher": {
                                "data": {
                                    "type": "publisher",
                                    "id": "12345678-1234-1234-1234-1234567890af"
                                }
                            }
                          }
                        }
                      },
                      {
                        "op": "add",
                        "path": "/book/12345678-1234-1234-1234-1234567890ac/publisher",
                        "value": {
                            "type": "publisher",
                            "id": "12345678-1234-1234-1234-1234567890ae",
                            "attributes": {
                                "name": "Default publisher"
                            }
                        }
                      },
                      {
                        "op": "add",
                        "path": "/book/12345678-1234-1234-1234-1234567890ad/publisher",
                        "value": {
                            "type": "publisher",
                            "id": "12345678-1234-1234-1234-1234567890af",
                            "attributes": {
                                "name": "Super Publisher"
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
    }

    @Test
    public void testSortingRootCollectionByRelationshipProperty() {
        def result = mapper.readTree(
                RestAssured.get("/book?sort=-publisher.name").asString())
        //We expect 2 results because the Hibernate does an inner join between book & publisher
        Assert.assertEquals(result.get("data").size(), 2);

        JsonNode books = result.get("data");
        String firstBookName = books.get(0).get("attributes").get("title").asText();
        Assert.assertEquals(firstBookName, "For Whom the Bell Tolls");

        String secondBookName = books.get(1).get("attributes").get("title").asText();
        Assert.assertEquals(secondBookName, "The Old Man and the Sea");

        result = mapper.readTree(
        RestAssured.get("/book?sort=publisher.name").asString())
        //We expect 2 results because the Hibernate does an inner join between book & publisher
        Assert.assertEquals(result.get("data").size(), 2);

        books = result.get("data");
        firstBookName = books.get(0).get("attributes").get("title").asText();
        Assert.assertEquals(firstBookName, "The Old Man and the Sea");

        secondBookName = books.get(1).get("attributes").get("title").asText();
        Assert.assertEquals(secondBookName, "For Whom the Bell Tolls");
    }

    @Test
    public void testSortingSubcollectionByRelationshipProperty() {
        def result = mapper.readTree(
                RestAssured.get("/author/1/books?sort=-publisher.name").asString())
        //We expect 2 results because the Hibernate does an inner join between book & publisher
        Assert.assertEquals(result.get("data").size(), 2);

        JsonNode books = result.get("data");
        String firstBookName = books.get(0).get("attributes").get("title").asText();
        Assert.assertEquals(firstBookName, "For Whom the Bell Tolls");

        String secondBookName = books.get(1).get("attributes").get("title").asText();
        Assert.assertEquals(secondBookName, "The Old Man and the Sea");

        result = mapper.readTree(
        RestAssured.get("/author/1/books?sort=publisher.name").asString())
        //We expect 2 results because the Hibernate does an inner join between book & publisher
        Assert.assertEquals(result.get("data").size(), 2);

        books = result.get("data");
        firstBookName = books.get(0).get("attributes").get("title").asText();
        Assert.assertEquals(firstBookName, "The Old Man and the Sea");

        secondBookName = books.get(1).get("attributes").get("title").asText();
        Assert.assertEquals(secondBookName, "For Whom the Bell Tolls");
    }

    @Test
    public void testSortingRootCollectionByRelationshipPropertyWithJoinFilter() {
        def result = mapper.readTree(
                RestAssured.get("/book?filter[book.authors.name][infixi]=Hemingway&sort=-publisher.name").asString())
        //We expect 2 results because the Hibernate does an inner join between book & publisher
        Assert.assertEquals(result.get("data").size(), 2);

        JsonNode books = result.get("data");
        String firstBookName = books.get(0).get("attributes").get("title").asText();
        Assert.assertEquals(firstBookName, "For Whom the Bell Tolls");

        String secondBookName = books.get(1).get("attributes").get("title").asText();
        Assert.assertEquals(secondBookName, "The Old Man and the Sea");

        result = mapper.readTree(
        RestAssured.get("/book?filter[book.authors.name][infixi]=Hemingway&sort=publisher.name").asString())
        //We expect 2 results because the Hibernate does an inner join between book & publisher
        Assert.assertEquals(result.get("data").size(), 2);

        books = result.get("data");
        firstBookName = books.get(0).get("attributes").get("title").asText();
        Assert.assertEquals(firstBookName, "The Old Man and the Sea");

        secondBookName = books.get(1).get("attributes").get("title").asText();
        Assert.assertEquals(secondBookName, "For Whom the Bell Tolls");
    }

    @Test
    public void testSortingByRelationshipId() {
        def result = mapper.readTree(
                RestAssured.get("/book?sort=-publisher.id").asString())

        //We expect 8 results because publisher_id is a foreign key inside the book table.
        Assert.assertEquals(result.get("data").size(), 8);

        JsonNode books = result.get("data");
        String firstBookName = books.get(0).get("attributes").get("title").asText();
        Assert.assertEquals(firstBookName, "For Whom the Bell Tolls");

        String secondBookName = books.get(1).get("attributes").get("title").asText();
        Assert.assertEquals(secondBookName, "The Old Man and the Sea");

        result = mapper.readTree(
        RestAssured.get("/book?sort=publisher.id").asString())
        Assert.assertEquals(result.get("data").size(), 8);

        books = result.get("data");
        firstBookName = books.get(6).get("attributes").get("title").asText();
        Assert.assertEquals(firstBookName, "The Old Man and the Sea");

        secondBookName = books.get(7).get("attributes").get("title").asText();
        Assert.assertEquals(secondBookName, "For Whom the Bell Tolls");
    }

    @Test
    public void testSortingById() {
        List<String> bookTitles = Arrays.asList(
                "Life with Null Ned 2",
                "Life with Null Ned",
                "The Roman Republic",
                "Foundation",
                "Enders Shadow",
                "Enders Game",
                "For Whom the Bell Tolls",
                "The Old Man and the Sea"
        );

        def result = mapper.readTree(
                RestAssured.get("/book?sort=-id").asString())
        Assert.assertEquals(result.get("data").size(), 8);

        JsonNode books = result.get("data");
        for (int idx = 0; idx < bookTitles.size(); idx++) {
            String expectedTitle = bookTitles.get(idx);
            String actualTitle = books.get(idx).get("attributes").get("title").asText();
            Assert.assertEquals(expectedTitle, actualTitle);
        }
    }
}
