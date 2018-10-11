/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.tests

import com.yahoo.elide.core.HttpStatus
import com.yahoo.elide.initialization.AbstractIntegrationTestInitializer

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import com.jayway.restassured.RestAssured
import com.jayway.restassured.response.ValidatableResponse

import org.testng.Assert
import org.testng.annotations.Test

import javax.ws.rs.core.MediaType

/**
 * Simple integration tests to verify session and access.
 */
class GraphQLIT extends AbstractIntegrationTestInitializer {
    private final ObjectMapper objectMapper = new ObjectMapper()

    // TODO: Bootstrapping some data before each test would be nice..
//    @BeforeTest
//    void setup() {
//        DataStoreTransaction tx = null
//        try {
//            tx = getDatabaseManager().beginTransaction()
//
//            // Create our initial set of test data
//            def author1 = tx.createNewObject(Author.class)
//            author1.setId(1L)
//            author1.setName("George Orwell")
//
//            def author2 = tx.createNewObject(Author.class)
//            author2.setId(2L)
//            author2.setName("John Steinbeck")
//
//            def book1 = tx.createNewObject(Book.class)
//            book1.setId(1L)
//            book1.setTitle("1984")
//            book1.setAuthors([author1])
//
//            def book2 = tx.createNewObject(Book.class)
//            book2.setId(2L)
//            book2.setTitle("Grapes of Wrath")
//            book2.setAuthors([author2])
//
//            author1.setBooks([book1])
//            author2.setBooks([book2])
//
//            tx.flush(null);
//            tx.save(author1, null)
//            tx.save(author2, null)
//            tx.save(book1, null)
//            tx.save(book2, null)
//            tx.flush(null)
//            tx.commit(null)
//        } finally {
//            if (tx != null) {
//                tx.close()
//            }
//        }
//    }

    @Test(priority = 1)
    void createBookAndAuthor() {
        def graphQLQuery = """
        mutation {
          book(op: UPSERT, data:{id:"1", title: "1984"}) {
            edges {
              node {
                id
                title
                authors(op: UPSERT, data:{id:"1", name: "George Orwell"}) {
                  edges {
                    node {
                      id
                      name
                    }
                  }
                }
              }
            }
          }
        }
        """
        def expectedResponse = """
        {"data":{"book":{"edges":[{"node":{"id":"1","title":"1984","authors":{"edges":[{"node":{"id":"1","name":"George Orwell"}}]}}}]}}}
        """

        runQueryWithExpectedResult(graphQLQuery, expectedResponse)
    }

    @Test(priority = 2)
    void createNewBooksAndAuthor() {
        def graphQLQuery = """
        mutation myMutation(\$bookName: String, \$authorName: String) {
          book(op: UPSERT, data:{title: \$bookName}) {
            edges {
              node {
                id
                title
                authors(op: UPSERT, data:{name: \$authorName}) {
                  edges {
                    node {
                      id
                      name
                    }
                  }
                }
              }
            }
          }
        }
        """
        String expected = """{"data":{"book":{"edges":[{"node":{"id":"2","title":"Grapes of Wrath","authors":{"edges":[{"node":{"id":"2","name":"John Setinbeck"}}]}}}]}}}"""
        def variables = [bookName: "Grapes of Wrath", authorName: "John Setinbeck"]

        runQueryWithExpectedResult(graphQLQuery, variables, expected)
    }

    @Test(priority = 3)
    void fetchCollection() {
        def graphQLQuery = "{ book { edges { node { id title authors { edges { node { id name } } } } } } }"
        def expected = """{"data":{"book":{"edges":[{"node":{"id":"1","title":"1984","authors":{"edges":[{"node":{"id":"1","name":"George Orwell"}}]}}},{"node":{"id":"2","title":"Grapes of Wrath","authors":{"edges":[{"node":{"id":"2","name":"John Setinbeck"}}]}}}]}}}"""
        runQueryWithExpectedResult(graphQLQuery, expected)
    }

    @Test(priority = 4)
    void fetchRootSingle() {
        def graphQLQuery = """
        {
          book(ids: ["1"]) {
            edges {
              node {
                id
                title
              }
            }
          }
        }
        """
        def expectedResponse = """
        {"data":{"book":{"edges":[{"node":{"id":"1","title":"1984"}}]}}}
        """

        runQueryWithExpectedResult(graphQLQuery, expectedResponse)
    }

    @Test(priority = 5)
    void runUpdateAndFetchDifferentTransactionsBatch() {
        def graphQLQuery = """
        mutation {
          book(op: UPSERT, data:{id:"abc", title: "my book created in batch!"}) {
            edges {
              node {
                id
                title
              }
            }
          }
        }
        """
        def graphQLQuery2 = """
        query {
          book(ids: "3") {
            edges {
              node {
                id
                title
              }
            }
          }
        }
        """
        def expectedResponse = """
        [{"data":{"book":{"edges":[{"node":{"id":"3","title":"my book created in batch!"}}]}}},{"data":{"book":{"edges":[{"node":{"id":"3","title":"my book created in batch!"}}]}}}]
        """

        compareJsonObject(runQuery(toJsonArray(toJsonNode(graphQLQuery), toJsonNode(graphQLQuery2))), expectedResponse)
    }

    @Test(priority = 6)
    void runMultipleRequestsSameTransaction() {
        // This test demonstrates that multiple roots can be manipulated within a _single_ transaction
        def graphQLQuery = """
        {
          book(ids: ["1"]) {
            edges {
              node {
                id
                title
                authors {
                  edges {
                    node {
                      id
                      name
                    }
                  }
                }
              }
            }
          }
          author {
            edges {
              node {
                id
                name
              }
            }
          }
        }
        """
        def expectedResponse = """
        {"data":{"book":{"edges":[{"node":{"id":"1","title":"1984","authors":{"edges":[{"node":{"id":"1","name":"George Orwell"}}]}}}]},"author":{"edges":[{"node":{"id":"1","name":"George Orwell"}},{"node":{"id":"2","name":"John Setinbeck"}}]}}}
        """

        runQueryWithExpectedResult(graphQLQuery, expectedResponse)
    }

    @Test(priority = 7)
    void runMultipleRequestsSameTransactionMutation() {
        // This test demonstrates that multiple roots can be manipulated within a _single_ transaction
        // and results are consistent across a mutation.
        def graphQLQuery = """
        mutation {
          book(ids: ["1"]) {
            edges {
              node {
                id
                title
                authors(op: UPSERT, data:{id:"3", name:"Stephen King"}) {
                  edges {
                    node {
                      id
                      name
                    }
                  }
                }
              }
            }
          }
          author {
            edges {
              node {
                id
                name
              }
            }
          }
        }
        """
        def expectedResponse = """
        {"data":{"book":{"edges":[{"node":{"id":"1","title":"1984","authors":{"edges":[{"node":{"id":"3","name":"Stephen King"}}]}}}]},"author":{"edges":[{"node":{"id":"1","name":"George Orwell"}},{"node":{"id":"2","name":"John Setinbeck"}},{"node":{"id":"3","name":"Stephen King"}}]}}}
        """

        runQueryWithExpectedResult(graphQLQuery, expectedResponse)
    }

    @Test(priority = 6)
    void runMultipleRequestsSameTransactionWithAliases() {
        // This test demonstrates that multiple roots can be manipulated within a _single_ transaction
        def graphQLQuery = """
        {
          firstAuthorCollection: author {
            edges {
              node {
                id
                name
              }
            }
          }
          secondAuthorCollection: author {
            edges {
              node {
                id
                name
              }
            }
          }
        }
        """
        def expectedResponse = """
        {"data":{"firstAuthorCollection":{"edges":[{"node":{"id":"1","name":"George Orwell"}},{"node":{"id":"2","name":"John Setinbeck"}}]},"secondAuthorCollection":{"edges":[{"node":{"id":"1","name":"George Orwell"}},{"node":{"id":"2","name":"John Setinbeck"}}]}}}
        """

        runQueryWithExpectedResult(graphQLQuery, expectedResponse)
    }

    private void runQueryWithExpectedResult(String graphQLQuery, Map<String, Object> variables, String expected) {
        compareJsonObject(runQuery(graphQLQuery, variables), expected)
    }

    private void runQueryWithExpectedResult(String graphQLQuery, String expected) {
        runQueryWithExpectedResult(graphQLQuery, null, expected)
    }

    private void compareJsonObject(ValidatableResponse response, String expected) {
        JsonNode responseNode = objectMapper.readTree(response.extract().body().asString())
        JsonNode expectedNode = objectMapper.readTree(expected)
        Assert.assertEquals(responseNode, expectedNode)
    }

    private ValidatableResponse runQuery(String query, Map<String, Object> variables) {
        return runQuery(toJsonQuery(query, variables))
    }

    private ValidatableResponse runQuery(String query) {
        return RestAssured.given()
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(query)
                .post("/graphQL")
                .then()
                .statusCode(HttpStatus.SC_OK)
    }

    private String toJsonArray(JsonNode... nodes) {
        ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode()
        for(JsonNode node : nodes) {
            arrayNode.add(node)
        }
        return objectMapper.writeValueAsString(arrayNode)
    }

    private String toJsonQuery(String query, Map<String, Object> variables) {
        return objectMapper.writeValueAsString(toJsonNode(query, variables))
    }

    private JsonNode toJsonNode(String query) {
        return toJsonNode(query, null)
    }

    private JsonNode toJsonNode(String query, Map<String, Object> variables) {
        ObjectNode graphqlNode = JsonNodeFactory.instance.objectNode()
        graphqlNode.put("query", query)
        if (variables != null) {
            graphqlNode.set("variables", objectMapper.valueToTree(variables))
        }
        return graphqlNode
    }
}
