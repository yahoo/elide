/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.jpa;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.datastore.test.DataStoreTestHarness;
import com.yahoo.elide.datastores.jpa.porting.QueryLogger;
import com.yahoo.elide.initialization.IntegrationTest;
import example.Author;
import example.Book;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.Arrays;

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
            book1.setTitle("Test Book1");
            tx.createObject(book1, null);

            Book book2 = new Book();
            book2.setTitle("Test Book2");
            tx.createObject(book2, null);

            Author author = new Author();
            tx.createObject(author, null);

            author.setBooks(Arrays.asList(book1, book2));
            book1.setAuthors(Arrays.asList(author));
            book2.setAuthors(Arrays.asList(author));

            tx.commit(null);
        }
    }

    @Test
    public void testLoadRootCollection() {
        given()
                .when().get("/book")
                .then()
                .statusCode(HttpStatus.SC_OK);

        verifyLoggingStatements(
                "SELECT example_Book FROM example.Book AS example_Book LEFT JOIN FETCH example_Book.publisher"
        );
    }

    @Test
    public void testMultiElementRootCollectionWithIncludedSubcollection() {
        given()
                .when().get("/book?include=authors")
                .then()
                .statusCode(HttpStatus.SC_OK);

        verifyLoggingStatements(
                "SELECT example_Book FROM example.Book AS example_Book LEFT JOIN FETCH example_Book.publisher"
        );
    }

    @Test
    public void testSingleElementRootCollectionWithIncludedSubcollection() {
        given()
                .when().get("/book?filter=title=='Test Book1'&include=authors")
                .then()
                .statusCode(HttpStatus.SC_OK);

        verifyLoggingStatements(
                "SELECT example_Book FROM example.Book AS example_Book LEFT JOIN FETCH example_Book.publisher WHERE example_Book.title IN (:XXX)"
        );
    }

    private void verifyLoggingStatements(String ... statements) {
        verify(logger, times(statements.length)).log(anyString());
        for (String statement : statements) {
            ArgumentCaptor<String> actual = ArgumentCaptor.forClass(String.class);

            verify(logger, times(1)).log(actual.capture());
            assertEquals(normalizeQuery(statement), normalizeQuery(actual.getValue()));
        }
    }

    private static String normalizeQuery(String query) {
        String normalized = query.replaceAll(":\\w+", ":XXX");
        normalized = normalized.trim();
        normalized = normalized.replaceAll("\\s+", " ");
        return normalized;
    }
}
