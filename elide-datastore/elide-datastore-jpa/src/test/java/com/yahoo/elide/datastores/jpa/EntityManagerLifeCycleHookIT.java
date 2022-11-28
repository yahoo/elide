/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.jpa;

import static com.yahoo.elide.Elide.JSONAPI_CONTENT_TYPE;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.datum;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.id;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.resource;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.type;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsInAnyOrder;

import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.exceptions.HttpStatus;
import com.yahoo.elide.initialization.IntegrationTest;
import example.Book;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class EntityManagerLifeCycleHookIT extends IntegrationTest {

    @BeforeEach
    public void setUp() throws IOException {
        try (DataStoreTransaction tx = dataStore.beginTransaction()) {
            Book book1 = new Book();
            book1.setTitle("Test Book1");
            tx.createObject(book1, null);

            Book book2 = new Book();
            book2.setTitle("Test Book2");
            tx.createObject(book2, null);

            Book book3 = new Book();
            book3.setTitle("Test Book3");
            tx.createObject(book3, null);

            tx.commit(null);
        }
    }

    @Test
    void testBookCatalogCreationHook() {
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(
                        datum(
                                resource(
                                        type("bookCatalog"),
                                        id("123")
                                )
                        )
                )
                .post("/bookCatalog")
                .then()
                .body("data.relationships.books.data.id", containsInAnyOrder("1", "2", "3"))
                .statusCode(HttpStatus.SC_CREATED);
    }
}
