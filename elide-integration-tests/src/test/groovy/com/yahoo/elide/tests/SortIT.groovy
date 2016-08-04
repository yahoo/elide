/*
 * Copyright 2016, Yahoo Inc.
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
 * Tests for sorting, including sorting rules that involve related entities and their attributes.
 */
class SortIT extends AbstractIntegrationTestInitializer {
    private final ObjectMapper mapper = new ObjectMapper()

    private Set<Integer> authorIds = new HashSet<>()
    private Set<Integer> bookIds = new HashSet<>()
    private Set<Integer> chapterIds = new HashSet<>()

    @BeforeClass
    public void setup() {

        // Some chapter titles from Harry Potter
        RestAssured
            .given()
            .contentType("application/vnd.api+json; ext=jsonpatch")
            .accept("application/vnd.api+json; ext=jsonpatch")
            .body('''[
                {"op": "add", "path": "/chapter",
                    "value": {"type": "chapter", "id": 1,"attributes": {"title": "The Boy Who Lived"}}
                },
                {"op": "add", "path": "/chapter",
                    "value": {"type": "chapter", "id": 2,"attributes": {"title": "The Vanishing Glass"}}
                },
                {"op": "add", "path": "/chapter",
                    "value": {"type": "chapter", "id": 3,"attributes": {"title": "The Letters from No One"}}
                },
                {"op": "add", "path": "/chapter",
                    "value": {"type": "chapter", "id": 4,"attributes": {"title": "The Worst Birthday"}}
                },
                {"op": "add", "path": "/chapter",
                    "value": {"type": "chapter", "id": 5,"attributes": {"title": "Dobby's Warning"}}
                },
                {"op": "add", "path": "/chapter",
                    "value": {"type": "chapter", "id": 6,"attributes": {"title": "The Burrow"}}
                },
                {"op": "add", "path": "/chapter",
                    "value": {"type": "chapter", "id": 7,"attributes": {"title": "Owl Post"}}
                },
                {"op": "add", "path": "/chapter",
                    "value": {"type": "chapter", "id": 8,"attributes": {"title": "Aunt Marge's Big Mistake"}}
                },
                {"op": "add", "path": "/chapter",
                    "value": {"type": "chapter", "id": 9,"attributes": {"title": "The Knight Bus"}}
                }
                ]''')
            .patch("/").then().statusCode(HttpStatus.SC_OK)

        // Create author J.K. Rowling with a few books
        RestAssured
            .given()
            .contentType("application/vnd.api+json; ext=jsonpatch")
            .accept("application/vnd.api+json; ext=jsonpatch")
            .body('''[
                {"op": "add", "path": "/author", "value": {
                    "type": "author", "id": 1,
                    "attributes": {"name": "J.K. Rowling"},
                    "relationships": {
                        "books": {
                            "data": [
                                {"type": "book", "id": 1},
                                {"type": "book", "id": 2},
                                {"type": "book", "id": 3}
                            ]
                        }
                    }
                }},
                {"op": "add", "path": "/book", "value": {
                    "type": "book", "id": 1,
                    "attributes": {
                        "title": "Harry Potter and the Philosopher's Stone",
                        "genre": "Juvenile Fiction", "language": "English"
                    },
                    "relationships": {
                        "chapters": {
                            "data": [
                                {"type": "chapter", "id": 1},
                                {"type": "chapter", "id": 2},
                                {"type": "chapter", "id": 3}
                            ]
                        }
                    }
                }},
                {"op": "add", "path": "/book", "value": {
                    "type": "book", "id": 2,
                    "attributes": {
                        "title": "Harry Potter and the Chamber of Secrets",
                        "genre": "Juvenile Fiction", "language": "English"
                    },
                    "relationships": {
                        "chapters": {
                            "data": [
                                {"type": "chapter", "id": 4},
                                {"type": "chapter", "id": 5},
                                {"type": "chapter", "id": 6}
                            ]
                        }
                    }
                }},
                {"op": "add", "path": "/book", "value": {
                    "type": "book", "id": 3,
                    "attributes": {
                        "title": "Harry Potter and the Prisoner of Azkaban",
                        "genre": "Juvenile Fiction", "language": "English"
                    },
                    "relationships": {
                        "chapters": {
                            "data": [
                                {"type": "chapter", "id": 7},
                                {"type": "chapter", "id": 8},
                                {"type": "chapter", "id": 9}
                            ]
                        }
                    }
                }}
                ]''')
            .patch("/").then().statusCode(HttpStatus.SC_OK)

        // Some chapter titles from The Hobbit and Lord of the Rings
        RestAssured
            .given()
            .contentType("application/vnd.api+json; ext=jsonpatch")
            .accept("application/vnd.api+json; ext=jsonpatch")
            .body('''[
                {"op": "add", "path": "/chapter",
                    "value": {"type": "chapter", "id": 10,"attributes": {"title": "An Unexpected Party"}}
                },
                {"op": "add", "path": "/chapter",
                    "value": {"type": "chapter", "id": 11,"attributes": {"title": "Roast Mutton"}}
                },
                {"op": "add", "path": "/chapter",
                    "value": {"type": "chapter", "id": 12,"attributes": {"title": "A Short Rest"}}
                },
                {"op": "add", "path": "/chapter",
                    "value": {"type": "chapter", "id": 13,"attributes": {"title": "A Long-expected Party"}}
                },
                {"op": "add", "path": "/chapter",
                    "value": {"type": "chapter", "id": 14,"attributes": {"title": "The Shadow of the Past"}}
                },
                {"op": "add", "path": "/chapter",
                    "value": {"type": "chapter", "id": 15,"attributes": {"title": "Three is Company"}}
                }
                ]''')
            .patch("/").then().statusCode(HttpStatus.SC_OK)

        // Create author J.R.R. Tolkien with a couple of books
        RestAssured
            .given()
            .contentType("application/vnd.api+json; ext=jsonpatch")
            .accept("application/vnd.api+json; ext=jsonpatch")
            .body('''[
                {"op": "add", "path": "/author", "value": {
                    "id": 2, "type": "author",
                    "attributes": {"name": "J.R.R. Tolkien"},
                    "relationships": {
                        "books": {
                            "data": [
                                {"type": "book", "id": 4},
                                {"type": "book", "id": 5}
                            ]
                        }
                    }
                }},
                {"op": "add", "path": "/book", "value": {
                    "type": "book", "id": 4,
                    "attributes": {
                        "title": "The Hobbit",
                        "genre": "Fantasy", "language": "English"
                    },
                    "relationships": {
                        "chapters": {
                            "data": [
                                {"type": "chapter", "id": 10},
                                {"type": "chapter", "id": 11},
                                {"type": "chapter", "id": 12}
                            ]
                        }
                    }
                }},
                {"op": "add", "path": "/book", "value": {
                    "type": "book", "id": 5,
                    "attributes": {
                        "title": "The Lord of the Rings",
                        "genre": "Fantasy", "language": "English"
                    },
                    "relationships": {
                        "chapters": {
                            "data": [
                                {"type": "chapter", "id": 13},
                                {"type": "chapter", "id": 14},
                                {"type": "chapter", "id": 15}
                            ]
                        }
                    }
                }}
                ]''')
            .patch("/").then().statusCode(HttpStatus.SC_OK)


        // Some chapter titles from The Lion, the Witch and the Wardrobe
        RestAssured
            .given()
            .contentType("application/vnd.api+json; ext=jsonpatch")
            .accept("application/vnd.api+json; ext=jsonpatch")
            .body('''[
                {"op": "add", "path": "/chapter",
                    "value": {"type": "chapter", "id": 16,"attributes": {"title": "Lucy Looks Into a Wardrobe"}}
                },
                {"op": "add", "path": "/chapter",
                    "value": {"type": "chapter", "id": 17,"attributes": {"title": "What Lucy Found There"}}
                }
                ]''')
            .patch("/").then().statusCode(HttpStatus.SC_OK)


        // Create author C.S. Lewis with one book
        RestAssured
            .given()
            .contentType("application/vnd.api+json; ext=jsonpatch")
            .accept("application/vnd.api+json; ext=jsonpatch")
            .body('''[
                {"op": "add", "path": "/author", "value": {
                    "id": 3, "type": "author",
                    "attributes": {"name": "C.S. Lewis"},
                    "relationships": {
                        "books": {
                            "data": [
                                {"type": "book", "id": 6}
                            ]
                        }
                    }
                }},
                {"op": "add", "path": "/book", "value": {
                    "type": "book", "id": 6,
                    "attributes": {
                        "title": "The Lion, the Witch and the Wardrobe",
                        "genre": "Juvenile Fiction", "language": "English"
                    },
                    "relationships": {
                        "chapters": {
                            "data": [
                                {"type": "chapter", "id": 16},
                                {"type": "chapter", "id": 17}
                            ]
                        }
                    }
                }}
                ]''')
            .patch("/").then().statusCode(HttpStatus.SC_OK)

        for (JsonNode author : mapper.readTree(RestAssured.get("/author").asString()).get("data")) {
            authorIds.add(author.get("id").asInt())
        }
        for (JsonNode book : mapper.readTree(RestAssured.get("/book").asString()).get("data")) {
            bookIds.add(book.get("id").asInt())
        }
        for (JsonNode chapter : mapper.readTree(RestAssured.get("/chapter").asString()).get("data")) {
            chapterIds.add(chapter.get("id").asInt())
        }
    }

    @Test
    public void testSortOnSimpleAttribute() {
        def result = mapper.readTree(RestAssured.get("/chapter?sort=title").asString())
        def chapters = result["data"]
        Assert.assertEquals(chapters.size(), 17)
        final String firstChapterTitle = chapters.get(0)["attributes"]["title"].asText()
        Assert.assertEquals(firstChapterTitle, "A Long-expected Party")
    }

    @Test
    public void testSortOnSingleJoinEntityAttribute() {
        def result = mapper.readTree(RestAssured.get("/chapter?sort=books.title,title").asString())
        def chapters = result["data"]
        Assert.assertEquals(chapters.size(), 17)
        def firstChapter = chapters.get(0)
        final String firstChapterBookId = firstChapter["relationships"]["books"]["data"].get(0)["id"].asText()
        Assert.assertEquals(firstChapterBookId, "2")
        final String firstChapterTitle = firstChapter["attributes"]["title"].asText()
        Assert.assertEquals(firstChapterTitle, "Dobby's Warning")
    }

    @Test
    public void testSortOnMultipleJoinEntityAttributes() {
        def result = mapper.readTree(RestAssured.get("/chapter?sort=-books.genre,books.authors.name,-title&page[limit]=3").asString())
        def chapters = result["data"]
        Assert.assertEquals(chapters.size(), 3)
        def firstChapter = chapters.get(0)
        final String firstChapterBookId = firstChapter["relationships"]["books"]["data"].get(0)["id"].asText()
        Assert.assertEquals(firstChapterBookId, "6")
        final String firstChapterTitle = firstChapter["attributes"]["title"].asText()
        Assert.assertEquals(firstChapterTitle, "What Lucy Found There")
    }

    @Test
    public void testSortOnMultipleJoinEntityAttributesWithFilter() {
        def result = mapper.readTree(RestAssured.get(
                "/chapter?filter[chapter.books.title][prefix]=The&sort=books.genre,books.title,-title&page[limit]=5")
                .asString())
        def chapters = result["data"]
        Assert.assertEquals(chapters.size(), 5)
        def firstChapter = chapters.get(0)
        final String firstChapterBookId = firstChapter["relationships"]["books"]["data"].get(0)["id"].asText()
        Assert.assertEquals(firstChapterBookId, "4")
        final String firstChapterTitle = firstChapter["attributes"]["title"].asText()
        Assert.assertEquals(firstChapterTitle, "Roast Mutton")
    }

    @Test
    public void testSortOnMultipleJoinEntityAttributesWithPagination() {
        def result = mapper.readTree(RestAssured.get(
                "/chapter?include=books,books.authors&sort=books.authors.name,books.title,title" +
                "&page[size]=4&page[number]=2&page[totals]")
                .asString())
        def chapters = result["data"]
        Assert.assertEquals(chapters.size(), 4)

        def firstChapter = chapters.get(2)
        Assert.assertEquals(firstChapter["relationships"]["books"]["data"].get(0)["id"].asText(), "1")
        Assert.assertEquals(firstChapter["attributes"]["title"].asText(), "The Letters from No One")

        def metaPage = result["meta"]["page"]
        Assert.assertEquals(metaPage.get("number").asText(), "2")
        Assert.assertEquals(metaPage["totalPages"].asText(), "5")
        Assert.assertEquals(metaPage["totalRecords"].asText(), "17")
        Assert.assertEquals(metaPage["limit"].asText(), "4")
    }

    @AfterTest
    public void cleanUp() {
        for (int id : chapterIds) {
            RestAssured
                    .given()
                    .accept("application/vnd.api+json; ext=jsonpatch")
                    .delete("/book/"+id)
        }
        for (int id : bookIds) {
            RestAssured
                    .given()
                    .accept("application/vnd.api+json; ext=jsonpatch")
                    .delete("/book/"+id)
        }
        for (int id : authorIds) {
            RestAssured
                    .given()
                    .accept("application/vnd.api+json; ext=jsonpatch")
                    .delete("/author/"+id)
        }
    }
}
