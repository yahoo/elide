/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.jpa;

import static io.restassured.RestAssured.given;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.datastore.test.DataStoreTestHarness;
import com.yahoo.elide.datastores.jpa.porting.QueryLogger;
import com.yahoo.elide.initialization.IntegrationTest;
import example.Book;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

/**
 * Verifies JPQL generation for the JPA Store.
 */
public class NPlusOneIT extends IntegrationTest {
    private QueryLogger logger;

    @Override
    protected DataStoreTestHarness createHarness() {
        logger = mock(QueryLogger.class);
        return new JpaDataStoreHarness(logger, true);
    }

    @BeforeEach
    public void setUp() throws IOException {
        reset(logger);

        try (DataStoreTransaction tx = dataStore.beginTransaction()) {
            Book book1 = new Book();
            book1.setTitle("Test Book");

            tx.save(book1, null);
            tx.commit(null);
        }
    }

    @Test
    public void testLoadRootCollection() {
        given()
                .when().get("/book")
                .then()
                .statusCode(HttpStatus.SC_OK);

        verify(logger).log(eq("SELECT example_Book FROM example.Book AS example_Book  LEFT JOIN FETCH example_Book.publisher  "));
    }
}
