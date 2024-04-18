/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.jsonapi.extensions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.paiondata.elide.ElideSettings;
import com.paiondata.elide.core.datastore.DataStore;
import com.paiondata.elide.core.datastore.DataStoreTransaction;
import com.paiondata.elide.core.datastore.inmemory.HashMapDataStore;
import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.exceptions.InvalidEntityBodyException;
import com.paiondata.elide.core.exceptions.JsonApiAtomicOperationsException;
import com.paiondata.elide.core.request.EntityProjection;
import com.paiondata.elide.core.request.route.Route;
import com.paiondata.elide.jsonapi.JsonApiMapper;
import com.paiondata.elide.jsonapi.JsonApiSettings;
import com.paiondata.elide.jsonapi.models.Resource;
import com.paiondata.elide.jsonapi.models.Results;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import example.Book;
import example.Company;
import example.Person;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Tests for JsonApiAtomicOperations.
 */
public class JsonApiAtomicOperationsTest {
    private DataStore dataStore;
    private ElideSettings settings;

    @BeforeEach
    void setup() {
        this.dataStore = new HashMapDataStore(Arrays.asList(Book.class, Company.class, Person.class));
        EntityDictionary entityDictionary = EntityDictionary.builder().build();
        this.dataStore.populateEntityDictionary(entityDictionary);
        JsonApiMapper jsonApiMapper = new JsonApiMapper();
        JsonApiSettings.JsonApiSettingsBuilder jsonApiSettings = JsonApiSettings.builder().jsonApiMapper(jsonApiMapper);
        this.settings = ElideSettings.builder().dataStore(this.dataStore).entityDictionary(entityDictionary)
                .objectMapper(jsonApiMapper.getObjectMapper()).settings(jsonApiSettings).build();
    }

    Supplier<Pair<Integer, JsonNode>> doInTransaction(
            Function<JsonApiAtomicOperationsRequestScope, Supplier<Pair<Integer, JsonNode>>> callback) {
        try (DataStoreTransaction transaction = this.dataStore.beginTransaction()) {
            Route route = Route.builder().baseUrl("https://elide.io").build();
            JsonApiAtomicOperationsRequestScope scope = new JsonApiAtomicOperationsRequestScope(route, transaction, null,
                    UUID.randomUUID(), settings);
            Supplier<Pair<Integer, JsonNode>> result = callback.apply(scope);

            scope.saveOrCreateObjects();
            transaction.commit(scope);
            return result;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    void invalidOperationShouldThrow() {
        String operationsDoc = """
                {
                  "atomic:operations": [{
                    "op": "invalid",
                    "ref": {
                      "type": "articles",
                      "id": "13"
                    }
                  }]
                }""";

        doInTransaction(scope -> {
            assertThrows(InvalidEntityBodyException.class,
                    () -> JsonApiAtomicOperations.processAtomicOperations(this.dataStore, null, operationsDoc, scope));
            try {
                return JsonApiAtomicOperations.processAtomicOperations(this.dataStore, null, operationsDoc, scope);
            } catch (InvalidEntityBodyException e) {
                assertEquals("Bad Request Body'Invalid Atomic Operations extension operation code:invalid'",
                        e.getMessage());
                return null;
            }
        });
    }

    @Test
    void invalidJsonShouldThrow() {
        String operationsDoc = """
                {invalidjson""";
        doInTransaction(scope -> {
            assertThrows(InvalidEntityBodyException.class,
                    () -> JsonApiAtomicOperations.processAtomicOperations(null, null, operationsDoc, scope));
            try {
                return JsonApiAtomicOperations.processAtomicOperations(this.dataStore, null, operationsDoc, scope);
            } catch (InvalidEntityBodyException e) {
                assertEquals("Bad Request Body'{invalidjson'", e.getMessage());
                return null;
            }
        });
    }

    @Test
    void invalidRefShouldThrow() {
        String operationsDoc = """
                {
                  "atomic:operations": [{
                    "op": "remove",
                    "ref": {
                      "id": "13"
                    }
                  }]
                }""";

        doInTransaction(scope -> {
            assertThrows(JsonApiAtomicOperationsException.class,
                    () -> JsonApiAtomicOperations.processAtomicOperations(null, null, operationsDoc, scope));
            try {
                return JsonApiAtomicOperations.processAtomicOperations(this.dataStore, null, operationsDoc, scope);
            } catch (JsonApiAtomicOperationsException e) {
                ObjectNode error = (ObjectNode) e.getErrorResponse().getBody().get(0).get("errors").get(0);
                assertEquals("400", error.get("status").asText());
                assertEquals("Bad Request Body&#39;Atomic Operations extension ref must specify the type member.&#39;",
                        error.get("detail").asText());
                return null;
            }
        });
    }

    @Test
    void createResourceShouldNotSpecifyRefShouldThrow() {
        String operationsDoc = """
                {
                  "atomic:operations": [{
                    "op": "add",
                    "ref": {
                      "id": "13",
                      "type": "group"
                    }
                  }]
                }""";

        doInTransaction(scope -> {
            assertThrows(JsonApiAtomicOperationsException.class,
                    () -> JsonApiAtomicOperations.processAtomicOperations(null, null, operationsDoc, scope));
            try {
                return JsonApiAtomicOperations.processAtomicOperations(this.dataStore, null, operationsDoc, scope);
            } catch (JsonApiAtomicOperationsException e) {
                ObjectNode error = (ObjectNode) e.getErrorResponse().getBody().get(0).get("errors").get(0);
                assertEquals("400", error.get("status").asText());
                assertEquals("Bad Request Body&#39;Atomic Operations extension add resource operation may only specify the href member.&#39;",
                        error.get("detail").asText());
                return null;
            }
        });
    }

    @Test
    void bothRefAndHrefShouldThrow() {
        String operationsDoc = """
                {
                  "atomic:operations": [{
                    "op": "remove",
                    "href": "/group/13",
                    "ref": {
                      "id": "13",
                      "type": "group"
                    }
                  }]
                }""";

        doInTransaction(scope -> {
            assertThrows(JsonApiAtomicOperationsException.class,
                    () -> JsonApiAtomicOperations.processAtomicOperations(null, null, operationsDoc, scope));
            try {
                return JsonApiAtomicOperations.processAtomicOperations(this.dataStore, null, operationsDoc, scope);
            } catch (JsonApiAtomicOperationsException e) {
                ObjectNode error = (ObjectNode) e.getErrorResponse().getBody().get(0).get("errors").get(0);
                assertEquals("400", error.get("status").asText());
                assertEquals("Bad Request Body&#39;Atomic Operations extension operation cannot contain both ref and href members.&#39;",
                        error.get("detail").asText());
                return null;
            }
        });
    }

    @Test
    void refWithBothIdAndLidShouldThrow() {
        String operationsDoc = """
                {
                  "atomic:operations": [{
                    "op": "remove",
                    "ref": {
                      "id": "13",
                      "lid": "6868e773-e05c-4ef5-8db7-0a493336fbb5",
                      "type": "group"
                    }
                  }]
                }""";

        doInTransaction(scope -> {
            assertThrows(JsonApiAtomicOperationsException.class,
                    () -> JsonApiAtomicOperations.processAtomicOperations(null, null, operationsDoc, scope));
            try {
                return JsonApiAtomicOperations.processAtomicOperations(this.dataStore, null, operationsDoc, scope);
            } catch (JsonApiAtomicOperationsException e) {
                ObjectNode error = (ObjectNode) e.getErrorResponse().getBody().get(0).get("errors").get(0);
                assertEquals("400", error.get("status").asText());
                assertEquals("Bad Request Body&#39;Atomic Operations extension ref cannot contain both id and lid members.&#39;",
                        error.get("detail").asText());
                return null;
            }
        });
    }

    @Test
    void noRefAndHrefShouldThrow() {
        String operationsDoc = """
                {
                  "atomic:operations": [{
                    "op": "add"
                  }]
                }""";
        doInTransaction(scope -> {
            assertThrows(JsonApiAtomicOperationsException.class,
                    () -> JsonApiAtomicOperations.processAtomicOperations(this.dataStore, null, operationsDoc, scope));
            try {
                return JsonApiAtomicOperations.processAtomicOperations(this.dataStore, null, operationsDoc, scope);
            } catch (JsonApiAtomicOperationsException e) {
                ObjectNode error = (ObjectNode) e.getErrorResponse().getBody().get(0).get("errors").get(0);
                assertEquals("400", error.get("status").asText());
                assertEquals(
                        "Bad Request Body&#39;Atomic Operations extension operation requires either ref or href members to be specified.&#39;",
                        error.get("detail").asText());
                return null;
            }
        });
    }

    @Test
    void removeNoRefAndHrefShouldThrow() {
        String operationsDoc = """
                {
                  "atomic:operations": [{
                    "op": "remove",
                    "data": {
                      "type": "book",
                      "id": "13"
                    }
                  }]
                }""";
        doInTransaction(scope -> {
            assertThrows(JsonApiAtomicOperationsException.class,
                    () -> JsonApiAtomicOperations.processAtomicOperations(this.dataStore, null, operationsDoc, scope));
            try {
                return JsonApiAtomicOperations.processAtomicOperations(this.dataStore, null, operationsDoc, scope);
            } catch (JsonApiAtomicOperationsException e) {
                ObjectNode error = (ObjectNode) e.getErrorResponse().getBody().get(0).get("errors").get(0);
                assertEquals("400", error.get("status").asText());
                assertEquals(
                        "Bad Request Body&#39;Atomic Operations extension operation requires either ref or href members to be specified.&#39;",
                        error.get("detail").asText());
                return null;
            }
        });
    }

    @Test
    void oneToOneDeleteUnknownCollectionShouldThrow() {
        String operationsDoc = """
                {
                  "atomic:operations": [{
                    "op": "update",
                    "ref": {
                      "type": "book",
                      "id": "13",
                      "relationship": "author"
                    },
                    "data": null
                  }]
                }""";
        doInTransaction(scope -> {
            try {
                return JsonApiAtomicOperations.processAtomicOperations(this.dataStore, null, operationsDoc, scope);
            } catch (JsonApiAtomicOperationsException e) {
                JsonNode error = e.getErrorResponse().getBody().get(0).get("errors").get(0);
                assertEquals("404", error.get("status").asText());
                assertEquals("Unknown collection author", error.get("detail").asText());
                return null;
            }
        });
    }

    @Test
    void addUpdateRemove() throws IOException {
        // Add
        Pair<Integer, JsonNode> result = doInTransaction(scope -> {
            String operationsDoc = """
                    {
                      "atomic:operations": [{
                        "op": "add",
                        "data": {
                          "type": "company",
                          "id": "1",
                          "attributes": {
                            "description": "Company Description"
                          }
                        }
                      }]
                    }""";
            return JsonApiAtomicOperations
                    .processAtomicOperations(this.dataStore, null, operationsDoc, scope);
        }).get();

        assertEquals(200, result.getKey());
        JsonNode data = result.getValue().get("atomic:results").get(0).get("data");
        assertEquals("company", data.get("type").asText());
        assertEquals("1", data.get("id").asText());


        doInTransaction(scope -> {
            Company company = scope.getTransaction().loadObject(EntityProjection.builder().type(Company.class).build(),
                    "1", scope);
            assertEquals("Company Description", company.getDescription());
            return null;
        });

        // Update
        result = doInTransaction(scope -> {
            String operationsDoc = """
                    {
                      "atomic:operations": [{
                        "op": "update",
                        "data": {
                          "type": "company",
                          "id": "1",
                          "attributes": {
                            "description": "Updated Company Description"
                          }
                        }
                      }]
                    }""";
            return JsonApiAtomicOperations
                    .processAtomicOperations(this.dataStore, null, operationsDoc, scope);
        }).get();

        assertEquals(200, result.getKey());
        data = result.getValue().get("atomic:results").get(0).get("data");
        assertTrue(data.isNull());


        doInTransaction(scope -> {
            Company company = scope.getTransaction().loadObject(EntityProjection.builder().type(Company.class).build(),
                    "1", scope);
            assertEquals("Updated Company Description", company.getDescription());
            return null;
        });

        // Remove
        result = doInTransaction(scope -> {
            String operationsDoc = """
                    {
                      "atomic:operations": [{
                        "op": "remove",
                        "ref": {
                          "type": "company",
                          "id": "1"
                        }
                      }]
                    }""";
            return JsonApiAtomicOperations
                    .processAtomicOperations(this.dataStore, null, operationsDoc, scope);
        }).get();

        assertEquals(200, result.getKey());
        data = result.getValue().get("atomic:results").get(0).get("data");
        assertTrue(data.isNull());


        doInTransaction(scope -> {
            Company company = scope.getTransaction().loadObject(EntityProjection.builder().type(Company.class).build(),
                    "1", scope);
            assertNull(company);
            return null;
        });
    }

    @Test
    void addUpdateRemoveHref() throws IOException {
        // Add
        Pair<Integer, JsonNode> result = doInTransaction(scope -> {
            String operationsDoc = """
                    {
                      "atomic:operations": [{
                        "op": "add",
                        "href": "company",
                        "data": {
                          "type": "company",
                          "id": "1",
                          "attributes": {
                            "description": "Company Description"
                          }
                        }
                      }]
                    }""";
            return JsonApiAtomicOperations
                    .processAtomicOperations(this.dataStore, null, operationsDoc, scope);
        }).get();

        assertEquals(200, result.getKey());
        JsonNode data = result.getValue().get("atomic:results").get(0).get("data");
        assertEquals("company", data.get("type").asText());
        assertEquals("1", data.get("id").asText());


        doInTransaction(scope -> {
            Company company = scope.getTransaction().loadObject(EntityProjection.builder().type(Company.class).build(),
                    "1", scope);
            assertEquals("Company Description", company.getDescription());
            return null;
        });

        // Update
        result = doInTransaction(scope -> {
            String operationsDoc = """
                    {
                      "atomic:operations": [{
                        "op": "update",
                        "href": "company/1",
                        "data": {
                          "type": "company",
                          "id": "1",
                          "attributes": {
                            "description": "Updated Company Description"
                          }
                        }
                      }]
                    }""";
            return JsonApiAtomicOperations
                    .processAtomicOperations(this.dataStore, null, operationsDoc, scope);
        }).get();

        assertEquals(200, result.getKey());
        data = result.getValue().get("atomic:results").get(0).get("data");
        assertTrue(data.isNull());

        doInTransaction(scope -> {
            Company company = scope.getTransaction().loadObject(EntityProjection.builder().type(Company.class).build(),
                    "1", scope);
            assertEquals("Updated Company Description", company.getDescription());
            return null;
        });

        // Remove
        result = doInTransaction(scope -> {
            String operationsDoc = """
                    {
                      "atomic:operations": [{
                        "op": "remove",
                        "href" : "company/1"
                      }]
                    }""";
            return JsonApiAtomicOperations
                    .processAtomicOperations(this.dataStore, null, operationsDoc, scope);
        }).get();

        assertEquals(200, result.getKey());
        data = result.getValue().get("atomic:results").get(0).get("data");
        assertTrue(data.isNull());


        doInTransaction(scope -> {
            Company company = scope.getTransaction().loadObject(EntityProjection.builder().type(Company.class).build(),
                    "1", scope);
            assertNull(company);
            return null;
        });

    }

    @Test
    void addRemoveLid() throws IOException {
        // Add
        Pair<Integer, JsonNode> result = doInTransaction(scope -> {
            String operationsDoc = """
                    {
                      "atomic:operations": [{
                        "op": "add",
                        "href": "person",
                        "data": {
                          "lid": "24fd9ef5-41dc-49b6-984e-ab958bb328c0",
                          "type": "person",
                          "attributes": {
                            "firstName": "John",
                            "lastName": "Doe"
                          },
                          "relationships": {
                            "bestFriend": {
                              "data": {
                                "lid": "24fd9ef5-41dc-49b6-984e-ab958bb328c0",
                                "type": "person"
                              }
                            }
                          }
                        }
                      },{
                        "op": "add",
                        "data": {
                          "lid": "386f2e88-26a7-4202-a238-06692df77c28",
                          "type": "person",
                          "attributes": {
                            "firstName": "Jane",
                            "lastName": "Doe"
                          }
                        }
                      },{
                        "op": "update",
                        "ref": {
                          "type": "person",
                          "lid": "386f2e88-26a7-4202-a238-06692df77c28",
                          "relationship": "bestFriend"
                        },
                        "data": {
                          "lid": "24fd9ef5-41dc-49b6-984e-ab958bb328c0",
                          "type": "person"
                        }
                      },{
                        "op": "update",
                        "data": {
                          "lid": "386f2e88-26a7-4202-a238-06692df77c28",
                          "type": "person",
                          "attributes": {
                            "firstName": "Mary"
                          }
                        }
                      }]
                    }""";
            return JsonApiAtomicOperations
                    .processAtomicOperations(this.dataStore, null, operationsDoc, scope);
        }).get();

        assertEquals(200, result.getKey());

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode data = result.getValue();
        Results results = objectMapper.treeToValue(data, Results.class);
        assertEquals(4, results.getResults().size());
        Resource person1 = results.getResults().get(0).getData();
        Resource person2 = results.getResults().get(1).getData();
        assertEquals("1", person1.getId());
        assertEquals("2", person2.getId());

        String expected = """
                {"atomic:results":[{"data":{"type":"person","id":"1","attributes":{"firstName":"John","lastName":"Doe"},"relationships":{"bestFriend":{"data":{"type":"person","id":"1"}}}}},{"data":{"type":"person","id":"2","attributes":{"firstName":"Mary","lastName":"Doe"},"relationships":{"bestFriend":{"data":{"type":"person","id":"1"}}}}},{"data":null},{"data":null}]}""";
        String actual = data.toString();
        assertEquals(expected, actual);

        // Remove
        result = doInTransaction(scope -> {
            String operationsDoc = """
                    {
                      "atomic:operations": [{
                        "op": "remove",
                        "href": "/person",
                        "data": {
                          "type": "person",
                          "id": "1"
                        }
                      },{
                        "op": "remove",
                        "ref": {
                          "type": "person",
                          "id": "2"
                        }
                      }]
                    }""";
            return JsonApiAtomicOperations
                    .processAtomicOperations(this.dataStore, null, operationsDoc, scope);
        }).get();

        assertEquals(200, result.getKey());
        expected = """
                {"atomic:results":[{"data":null},{"data":null}]}""";
        actual = result.getValue().toString();
        assertEquals(expected, actual);
    }

    @Test
    void nullHeader() {
        assertFalse(JsonApiAtomicOperations.isAtomicOperationsExtension(null));
    }

    @Test
    void jsonPatchHeader() {
        assertFalse(JsonApiAtomicOperations.isAtomicOperationsExtension("application/vnd.api+json; ext=jsonpatch"));
    }

    @Test
    void atomicOperationsHeader() {
        assertTrue(JsonApiAtomicOperations
                .isAtomicOperationsExtension("application/vnd.api+json;ext=\"https://jsonapi.org/ext/atomic\""));
    }

    @Test
    void atomicOperationsHeaderNoQuotes() {
        assertTrue(JsonApiAtomicOperations
                .isAtomicOperationsExtension("application/vnd.api+json;ext=https://jsonapi.org/ext/atomic"));
    }

    @Test
    void atomicOperationsHeaderNoQuotesSpaces() {
        assertTrue(JsonApiAtomicOperations
                .isAtomicOperationsExtension("application/vnd.api+json; ext = https://jsonapi.org/ext/atomic"));
    }

    @Test
    void atomicOperationsHeaderNoValue() {
        assertFalse(JsonApiAtomicOperations
                .isAtomicOperationsExtension("application/vnd.api+json;ext="));
    }

    @Test
    void atomicOperationsHeaderSingleQuote() {
        assertFalse(JsonApiAtomicOperations
                .isAtomicOperationsExtension("application/vnd.api+json;ext=\""));
    }

    @Test
    void atomicOperationsHeaderMultiple() {
        assertTrue(JsonApiAtomicOperations
                .isAtomicOperationsExtension(
                        "application/vnd.api+json;ext=\"jsonpatch https://jsonapi.org/ext/atomic\""));
    }
}
