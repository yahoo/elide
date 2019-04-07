/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql

import com.yahoo.elide.Elide
import com.yahoo.elide.ElideSettingsBuilder
import com.yahoo.elide.audit.AuditLogger
import com.yahoo.elide.core.EntityDictionary
import com.yahoo.elide.core.datastore.inmemory.HashMapDataStore
import com.yahoo.elide.resources.DefaultOpaqueUserFunction

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.google.common.collect.Sets

import org.mockito.Mockito
import org.skyscreamer.jsonassert.JSONAssert
import org.testng.Assert
import org.testng.annotations.BeforeMethod
import org.testng.annotations.BeforeTest
import org.testng.annotations.Test

import graphqlEndpointTestModels.Author
import graphqlEndpointTestModels.Book
import graphqlEndpointTestModels.DisallowShare
import graphqlEndpointTestModels.security.CommitChecks
import graphqlEndpointTestModels.security.UserChecks

import java.security.Principal

import javax.ws.rs.core.Response
import javax.ws.rs.core.SecurityContext

/**
 * GraphQL endpoint tests tested against the in-memory store.
 */
class GraphQLEndointTest {
    GraphQLEndpoint endpoint
    SecurityContext user1 = Mockito.mock(SecurityContext)
    SecurityContext user2 = Mockito.mock(SecurityContext)
    SecurityContext user3 = Mockito.mock(SecurityContext)
    AuditLogger audit = Mockito.mock(AuditLogger)

    class User implements Principal {
        StringBuilder log = new StringBuilder()
        String name

        @Override
        public String getName() {
            return name
        }

        public User withName(String name) {
            this.name = name
            return this
        }

        public void appendLog(String stmt) {
            log.append(stmt)
        }

        public String getLog() {
            return log.toString()
            log = new StringBuilder()
        }
    }

    @BeforeTest
    void setup() {
        Mockito.when(user1.getUserPrincipal()).thenReturn(new User().withName("1"));
        Mockito.when(user2.getUserPrincipal()).thenReturn(new User().withName("2"));
        Mockito.when(user3.getUserPrincipal()).thenReturn(new User().withName("3"));
    }

    @BeforeMethod
    void setupTest() throws Exception {
        HashMapDataStore inMemoryStore = new HashMapDataStore(Book.class.getPackage())
        Map<String, Class> checkMappings = new HashMap<>()
        checkMappings[UserChecks.IS_USER_1] = UserChecks.IsUserId.One.class
        checkMappings[UserChecks.IS_USER_2] = UserChecks.IsUserId.Two.class
        checkMappings[CommitChecks.IS_NOT_USER_3] = CommitChecks.IsNotUser3.class
        Elide elide = new Elide(
                new ElideSettingsBuilder(inMemoryStore)
                        .withEntityDictionary(new EntityDictionary(checkMappings))
                        .withAuditLogger(audit)
                        .build())
        endpoint = new GraphQLEndpoint(elide, new DefaultOpaqueUserFunction() {
            @Override
            Object apply(SecurityContext securityContext) {
                return securityContext.getUserPrincipal()
            }
        })

        def tx = inMemoryStore.beginTransaction()

        // Initial data
        Book book1 = new Book()
        Author author1 = new Author()
        Author author2 = new Author()
        DisallowShare noShare = new DisallowShare();

        book1.setId(1L)
        book1.setTitle("My first book")
        book1.setAuthors(Sets.newHashSet(author1))

        author1.setId(1L)
        author1.setName("Ricky Carmichael")
        author1.setBooks(Sets.newHashSet(book1))
        author1.setBookTitlesAndAwards(["Bookz": "Pulitzer Prize", "Lost in the Data": "PEN/Faulkner Award"])

        author2.setId(2)
        author2.setName("The Silent Author")
        author2.setBookTitlesAndAwards(["Working Hard or Hardly Working": "Booker Prize"])

        noShare.setId(1L)

        tx.createObject(book1, null)
        tx.createObject(author1, null)
        tx.createObject(author2, null)
        tx.createObject(noShare, null)

        tx.save(book1, null)
        tx.save(author1, null)
        tx.save(author2, null)
        tx.save(noShare, null)

        tx.commit(null)

        print inMemoryStore.typeIds
    }

    @Test
    void testValidFetch() {
        String graphQLRequest =
            '''
            {
              book {
                edges {
                  node {
                    id
                    title
                    authors {
                      edges {
                        node {
                          name
                        }
                      }
                    }
                  }
                }
              }
            }
            '''
        String graphQLResponse =
                '''
                {
                  "data": {
                    "book": {
                      "edges": [
                        {
                          "node": {
                            "id": "1",
                            "title": "My first book",
                            "authors": {
                              "edges": [
                                {
                                  "node": {
                                    "name": "Ricky Carmichael"
                                  }
                                }
                              ]
                            }
                          }
                        }
                      ]
                    }
                  }
                }
                '''
        def response = endpoint.post(user1, graphQLRequestToJSON(graphQLRequest))
        assert200EqualBody(response, graphQLResponse)
    }

    @Test
    void testValidFetchWithVariables() {
        String graphQLRequest =
            '''
            query myQuery($bookId: [String]) {
              book(ids: $bookId) {
                edges {
                  node {
                    id
                    title
                    authors {
                      edges {
                        node {
                          name
                        }
                      }
                    }
                  }
                }
              }
            }
            '''
        String graphQLResponse =
                '''
                {
                  "data": {
                    "book": {
                      "edges": [
                        {
                          "node": {
                            "id": "1",
                            "title": "My first book",
                            "authors": {
                              "edges": [
                                {
                                  "node": {
                                    "name": "Ricky Carmichael"
                                  }
                                }
                              ]
                            }
                          }
                        }
                      ]
                    }
                  }
                }
                '''
        def variables = ["bookId": "1"]
        def response = endpoint.post(user1, graphQLRequestToJSON(graphQLRequest, variables))
        assert200EqualBody(response, graphQLResponse)
    }

    @Test
    void testCanReadRestrictedFieldWithAppropriateAccess() {
        String graphQLRequest =
                '''
                {
                  book {
                    edges {
                      node {
                        user1SecretField
                      }
                    }
                  }
                }
                '''
        String graphQLResponse =
                '''
                {
                  "data": {
                    "book": {
                      "edges": [
                        {
                          "node": {
                            "user1SecretField": "this is a secret for user 1 only1"
                          }
                        }
                      ]
                    }
                  }
                }
                '''
        def response = endpoint.post(user1, graphQLRequestToJSON(graphQLRequest))
        assert200EqualBody(response, graphQLResponse)
    }

    @Test
    void testCannotReadRestrictedField() {
        String graphQLRequest =
                '''
                {
                  book {
                    edges {
                      node {
                        user1SecretField
                      }
                    }
                  }
                }
                '''
        def response = endpoint.post(user2, graphQLRequestToJSON(graphQLRequest))
        assertHasErrors(response)
    }

    @Test
    void testPartialResponse() {
        String graphQLRequest =
                '''
                {
                  book {
                    edges {
                      node {
                        user1SecretField
                      }
                    }
                  }
                  book {
                    edges {
                      node {
                        id
                        title
                      }
                    }
                  }
                }
                '''
        String expectedData =
                '''
                {
                  "book": {
                        "edges": [
                          {
                            "node": {
                              "user1SecretField": null,
                              "id": "1",
                              "title": "My first book"
                            }
                          }
                        ]
                      }
                }
                '''
        def response = endpoint.post(user2, graphQLRequestToJSON(graphQLRequest))
        assertHasErrors(response)
        assert200DataEqual(response, expectedData)
    }

    @Test
    void testFailedMutationAndRead() {
        String graphQLRequest =
                '''
                mutation {
                  book(op: UPSERT, data: {id: "1", title: "my new book!", authors:[{id:"2"}]}) {
                    edges {
                      node {
                        id
                        title
                      }
                    }
                  }
                }
                '''

        def response = endpoint.post(user2, graphQLRequestToJSON(graphQLRequest))
        assertHasErrors(response)

        graphQLRequest =
                '''
                {
                  book {
                    edges {
                      node {
                        id
                        title
                      }
                    }
                  }
                }
                '''
        String expected =
                '''
                {
                  "data": {
                    "book": {
                      "edges": [
                        {
                          "node": {
                            "id": "1",
                            "title": "My first book"
                          }
                        }
                      ]
                    }
                  }
                }
                '''
        response = endpoint.post(user2, graphQLRequestToJSON(graphQLRequest))
        assert200EqualBody(response, expected)
    }

    @Test
    void testNonShareable() {
        String graphQLRequest =
                '''
                mutation {
                  book{
                    edges {
                      node {
                        id
                        authors(op: UPSERT, data: {id: "123", name: "my new author", noShare:{id:"1"}}) {
                          edges {
                            node {
                              id
                              name
                              noShare {
                                edges {
                                  node {
                                    id
                                  }
                                }
                              }
                            }
                          }
                        }
                      }
                    }
                  }
                }
                '''
        def response = endpoint.post(user1, graphQLRequestToJSON(graphQLRequest))

        assertHasErrors(response)

        graphQLRequest =
                '''
                {
                  book{
                    edges {
                      node {
                        id
                        authors {
                          edges {
                            node {
                              id
                              name
                              noShare {
                                edges {
                                  node {
                                    id
                                  }
                                }
                              }
                            }
                          }
                        }
                      }
                    }
                  }
                }
                '''
        def expected =
                '''
                {
                  "data": {
                    "book": {
                      "edges": [
                        {
                          "node": {
                            "id": "1",
                            "authors": {
                              "edges": [
                                {
                                  "node": {
                                    "id": "1",
                                    "name": "Ricky Carmichael",
                                    "noShare": {
                                      "edges": []
                                    }
                                  }
                                }
                              ]
                            }
                          }
                        }
                      ]
                    }
                  }
                }
                '''

        response = endpoint.post(user1, graphQLRequestToJSON(graphQLRequest))
        assert200EqualBody(response, expected)
    }

    @Test
    void testLifeCycleHooks () {
        /* Separate user 1 so it doesn't interfere */
        SecurityContext user = Mockito.mock(SecurityContext)
        Mockito.when(user.getUserPrincipal()).thenReturn(new User().withName("1"));

        String graphQLRequest =
                '''
                mutation {
                  book(op: UPSERT, data: {id: "1", title: "my new book!"}) {
                    edges {
                      node {
                        id
                        title
                      }
                    }
                  }
                }
                '''
        String expected =
                '''
                {
                  "data": {
                    "book": {
                      "edges": [
                        {
                          "node": {
                            "id": "1",
                            "title": "my new book!"
                          }
                        }
                      ]
                    }
                  }
                }
                '''

        def response = endpoint.post(user, graphQLRequestToJSON(graphQLRequest))
        assert200EqualBody(response, expected)

        def expectedLog = "On Title Update Pre Security\nOn Title Update Pre Commit\nOn Title Update Post Commit\n";

        Assert.assertEquals(user.getUserPrincipal().getLog(), expectedLog)
    }

    @Test
    void testAuditLogging () {
        Mockito.reset(audit)

        String graphQLRequest =
                '''
                mutation {
                  book(op: UPSERT, data: {title: "my new book!"}) {
                    edges {
                      node {
                        id
                        title
                      }
                    }
                  }
                }
                '''
        endpoint.post(user1, graphQLRequestToJSON(graphQLRequest))

        Mockito.verify(audit, Mockito.times(1)).log(Mockito.any())
        Mockito.verify(audit, Mockito.times(1)).commit(Mockito.any())
        Mockito.verify(audit, Mockito.times(1)).clear()
    }

    @Test
    void testSuccessfulMutation() {
        String graphQLRequest =
                '''
                mutation {
                  book(op: UPSERT, data: {id: "123", title: "my new book!", authors:[{id:"2"}]}) {
                    edges {
                      node {
                        id
                        title
                        user1SecretField
                      }
                    }
                  }
                }
                '''
        String expected =
                '''
                {
                  "data": {
                    "book": {
                      "edges": [
                        {
                          "node": {
                            "id": "2",
                            "title": "my new book!",
                            "user1SecretField": "this is a secret for user 1 only1"
                          }
                        }
                      ]
                    }
                  }
                }
                '''

        def response = endpoint.post(user1, graphQLRequestToJSON(graphQLRequest))
        assert200EqualBody(response, expected)

        graphQLRequest =
                '''
                {
                  book {
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
                }
                '''
        expected =
                '''
                {
                  "data": {
                  "book": {
                    "edges": [
                      {
                        "node": {
                          "id": "1",
                          "title": "My first book",
                          "authors": {
                            "edges": [
                              {
                                "node": {
                                  "id": "1",
                                  "name": "Ricky Carmichael"
                                }
                              }
                            ]
                          }
                        }
                      },
                      {
                        "node": {
                          "id": "2",
                          "title": "my new book!",
                          "authors": {
                            "edges": [
                              {
                                "node": {
                                  "id": "2",
                                  "name": "The Silent Author"
                                }
                              }
                            ]
                          }
                        }
                      }
                    ]
                  }
                }
              }
              '''
        response = endpoint.post(user1, graphQLRequestToJSON(graphQLRequest))
        assert200EqualBody(response, expected)
    }

    @Test
    void testFailedCommitCheck() {
        // NOTE: User 3 cannot update books.
        def graphQLRequest =
                '''
                mutation {
                  book(op:UPSERT, data:{id:"1", title:"update title"}) {
                    edges {
                      node {
                        id
                        title
                      }
                    }
                  }
                }
                '''
        def response = endpoint.post(user3, graphQLRequestToJSON(graphQLRequest))
        assertHasErrors(response)
    }

    @Test
    void testQueryAMap() {
        def graphQLRequest =
                '''
                query {
                  book {
                    edges {
                      node {
                        id
                        authors {
                          edges {
                            node {
                              bookTitlesAndAwards {
                                key
                                value
                              }
                            }
                          }
                        }
                      }
                    }
                  }
                }
                '''
        def expected = '{"data":{"book":{"edges":[{"node":{"id":"1","authors":{"edges":[{"node":{"bookTitlesAndAwards":[{"key":"Bookz","value":"Pulitzer Prize"},{"key":"Lost in the Data","value":"PEN/Faulkner Award"}]}}]}}}]}}}'
        def response = endpoint.post(user1, graphQLRequestToJSON(graphQLRequest))
        assert200EqualBody(response, expected)
    }

    @Test
    void testQueryAMapWithBadFields() {
        def graphQLRequest =
                '''
                query {
                  book {
                    edges {
                      node {
                        id
                        authors {
                          edges {
                            node {
                              bookTitlesAndAwards {
                                key
                                value
                                Bookz
                              }
                            }
                          }
                        }
                      }
                    }
                  }
                }
                '''
        def response = endpoint.post(user1, graphQLRequestToJSON(graphQLRequest))
        assertHasErrors(response)
    }

    static String graphQLRequestToJSON(String request) {
        return graphQLRequestToJSON(request, new HashMap<String, String>())
    }

    static String graphQLRequestToJSON(String request, Map<String, String> variables) {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = JsonNodeFactory.instance.objectNode()
        node.put("query", request)
        node.set("variables", variables == null ? null : mapper.valueToTree(variables))
        return node.toString()
    }

    static JsonNode extract200Response(Response response) {
        return new ObjectMapper().readTree(extract200ResponseString(response))
    }

    static String extract200ResponseString(Response response) {
        Assert.assertEquals(response.getStatus(), 200)
        return (String) response.getEntity()
    }

    static assert200EqualBody(Response response, String expected) {
        String actual = extract200ResponseString(response)
        JSONAssert.assertEquals(expected, actual, true)
    }

    static assert200DataEqual(Response response, String expected) {
        JsonNode actualNode = extract200Response(response)
        String actual = new ObjectMapper().writeValueAsString(actualNode.get("data"))
        JSONAssert.assertEquals(expected, actual, true)
    }

    static assertHasErrors(Response response) {
        JsonNode node = extract200Response(response)
        Assert.assertFalse(node.get("errors").asList().isEmpty())
    }
}
