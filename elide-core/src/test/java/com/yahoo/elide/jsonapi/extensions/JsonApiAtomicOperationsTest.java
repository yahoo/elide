/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.jsonapi.extensions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.core.datastore.DataStore;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.datastore.inmemory.HashMapDataStore;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.exceptions.InvalidEntityBodyException;
import com.yahoo.elide.core.exceptions.JsonApiAtomicOperationsException;
import com.yahoo.elide.core.request.EntityProjection;
import com.yahoo.elide.jsonapi.JsonApiMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import example.Book;
import example.Company;

import org.apache.commons.lang3.tuple.Pair;
import org.glassfish.jersey.internal.util.collection.ImmutableMultivaluedMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Tests for JsonApiAtomicOperations.
 */
public class JsonApiAtomicOperationsTest {
    private DataStore dataStore;
    private ElideSettings settings;

    @BeforeEach
    void setup() {
        this.dataStore = new HashMapDataStore(Arrays.asList(Book.class, Company.class));
        EntityDictionary entityDictionary = EntityDictionary.builder().build();
        this.dataStore.populateEntityDictionary(entityDictionary);
        this.settings = new ElideSettingsBuilder(this.dataStore).withEntityDictionary(entityDictionary)
                .withJsonApiMapper(new JsonApiMapper()).build();
    }

    void doInTransaction(Consumer<JsonApiAtomicOperationsRequestScope> callback) {
        try (DataStoreTransaction transaction = this.dataStore.beginTransaction()) {
            JsonApiAtomicOperationsRequestScope scope = new JsonApiAtomicOperationsRequestScope("https://elide.io", "", "", transaction, null,
                    UUID.randomUUID(), ImmutableMultivaluedMap.empty(), new HashMap<>(), settings);
            callback.accept(scope);

            scope.saveOrCreateObjects();
            transaction.commit(scope);
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
                JsonApiAtomicOperations.processAtomicOperations(this.dataStore, null, operationsDoc, scope);
            } catch (InvalidEntityBodyException e) {
                assertEquals("Bad Request Body'Invalid Atomic Operations extension operation code:invalid'",
                        e.getMessage());
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
                JsonApiAtomicOperations.processAtomicOperations(this.dataStore, null, operationsDoc, scope);
            } catch (InvalidEntityBodyException e) {
                assertEquals("Bad Request Body'{invalidjson'", e.getMessage());
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
                JsonApiAtomicOperations.processAtomicOperations(this.dataStore, null, operationsDoc, scope);
            } catch (JsonApiAtomicOperationsException e) {
                ObjectNode error = (ObjectNode) e.getErrorResponse().getValue().get(0).get("errors").get(0);
                assertEquals("400", error.get("status").asText());
                assertEquals("Bad Request Body&#39;Atomic Operations extension requires ref type to be specified.&#39;",
                        error.get("detail").asText());
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
                JsonApiAtomicOperations.processAtomicOperations(this.dataStore, null, operationsDoc, scope);
            } catch (JsonApiAtomicOperationsException e) {
                ObjectNode error = (ObjectNode) e.getErrorResponse().getValue().get(0).get("errors").get(0);
                assertEquals("400", error.get("status").asText());
                assertEquals("Bad Request Body&#39;Atomic Operations extension create resource may only specify href.&#39;",
                        error.get("detail").asText());
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
                JsonApiAtomicOperations.processAtomicOperations(this.dataStore, null, operationsDoc, scope);
            } catch (JsonApiAtomicOperationsException e) {
                ObjectNode error = (ObjectNode) e.getErrorResponse().getValue().get(0).get("errors").get(0);
                assertEquals("400", error.get("status").asText());
                assertEquals("Bad Request Body&#39;Atomic Operations extension ref and href cannot both be specified together.&#39;",
                        error.get("detail").asText());
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
                JsonApiAtomicOperations.processAtomicOperations(this.dataStore, null, operationsDoc, scope);
            } catch (JsonApiAtomicOperationsException e) {
                ObjectNode error = (ObjectNode) e.getErrorResponse().getValue().get(0).get("errors").get(0);
                assertEquals("400", error.get("status").asText());
                assertEquals(
                        "Bad Request Body&#39;Atomic Operations extension requires either ref or href to be specified.&#39;",
                        error.get("detail").asText());
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
                JsonApiAtomicOperations.processAtomicOperations(this.dataStore, null, operationsDoc, scope);
            } catch (JsonApiAtomicOperationsException e) {
                ObjectNode error = (ObjectNode) e.getErrorResponse().getValue().get(0).get("errors").get(0);
                assertEquals("400", error.get("status").asText());
                assertEquals(
                        "Bad Request Body&#39;Atomic Operations extension requires either ref or href to be specified.&#39;",
                        error.get("detail").asText());
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
                JsonApiAtomicOperations.processAtomicOperations(this.dataStore, null, operationsDoc, scope);
            } catch (JsonApiAtomicOperationsException e) {
                JsonNode error = e.getErrorResponse().getValue().get(0).get("errors").get(0);
                assertEquals("404", error.get("status").asText());
                assertEquals("Unknown collection author", error.get("detail").asText());
            }
        });
    }

    @Test
    void addUpdateRemove() throws IOException {
        // Add
        doInTransaction(scope -> {
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
            Pair<Integer, JsonNode> result = JsonApiAtomicOperations
                    .processAtomicOperations(this.dataStore, null, operationsDoc, scope).get();
            assertEquals(200, result.getKey());
            JsonNode data = result.getValue().get("atomic:results").get(0).get("data");
            assertEquals("company", data.get("type").asText());
            assertEquals("1", data.get("id").asText());
        });

        doInTransaction(scope -> {
            Company company = scope.getTransaction().loadObject(EntityProjection.builder().type(Company.class).build(),
                    "1", scope);
            assertEquals("Company Description", company.getDescription());
        });

        // Update
        doInTransaction(scope -> {
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
            Pair<Integer, JsonNode> result = JsonApiAtomicOperations
                    .processAtomicOperations(this.dataStore, null, operationsDoc, scope).get();
            assertEquals(200, result.getKey());
            JsonNode data = result.getValue().get("atomic:results").get(0).get("data");
            assertTrue(data.isNull());
        });

        doInTransaction(scope -> {
            Company company = scope.getTransaction().loadObject(EntityProjection.builder().type(Company.class).build(),
                    "1", scope);
            assertEquals("Updated Company Description", company.getDescription());
        });

        // Remove
        doInTransaction(scope -> {
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
            Pair<Integer, JsonNode> result = JsonApiAtomicOperations
                    .processAtomicOperations(this.dataStore, null, operationsDoc, scope).get();
            assertEquals(200, result.getKey());
            JsonNode data = result.getValue().get("atomic:results").get(0).get("data");
            assertTrue(data.isNull());
        });

        doInTransaction(scope -> {
            Company company = scope.getTransaction().loadObject(EntityProjection.builder().type(Company.class).build(),
                    "1", scope);
            assertNull(company);
        });
    }

    @Test
    void addUpdateRemoveHref() throws IOException {
        // Add
        doInTransaction(scope -> {
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
            Pair<Integer, JsonNode> result = JsonApiAtomicOperations
                    .processAtomicOperations(this.dataStore, null, operationsDoc, scope).get();
            assertEquals(200, result.getKey());
            JsonNode data = result.getValue().get("atomic:results").get(0).get("data");
            assertEquals("company", data.get("type").asText());
            assertEquals("1", data.get("id").asText());
        });

        doInTransaction(scope -> {
            Company company = scope.getTransaction().loadObject(EntityProjection.builder().type(Company.class).build(),
                    "1", scope);
            assertEquals("Company Description", company.getDescription());
        });

        // Update
        doInTransaction(scope -> {
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
            Pair<Integer, JsonNode> result = JsonApiAtomicOperations
                    .processAtomicOperations(this.dataStore, null, operationsDoc, scope).get();
            assertEquals(200, result.getKey());
            JsonNode data = result.getValue().get("atomic:results").get(0).get("data");
            assertTrue(data.isNull());
        });

        doInTransaction(scope -> {
            Company company = scope.getTransaction().loadObject(EntityProjection.builder().type(Company.class).build(),
                    "1", scope);
            assertEquals("Updated Company Description", company.getDescription());
        });

        // Remove
        doInTransaction(scope -> {
            String operationsDoc = """
                    {
                      "atomic:operations": [{
                        "op": "remove",
                        "href" : "company/1"
                      }]
                    }""";
            Pair<Integer, JsonNode> result = JsonApiAtomicOperations
                    .processAtomicOperations(this.dataStore, null, operationsDoc, scope).get();
            assertEquals(200, result.getKey());
            JsonNode data = result.getValue().get("atomic:results").get(0).get("data");
            assertTrue(data.isNull());
        });

        doInTransaction(scope -> {
            Company company = scope.getTransaction().loadObject(EntityProjection.builder().type(Company.class).build(),
                    "1", scope);
            assertNull(company);
        });

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
