/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.jpa;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
import java.util.List;

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
            author.setName("Bob1");
            tx.createObject(author, null);

            Author author2 = new Author();
            author2.setName("Bob2");
            tx.createObject(author2, null);

            author.setBooks(Arrays.asList(book1, book2));
            book1.setAuthors(Arrays.asList(author, author2));
            book2.setAuthors(Arrays.asList(author, author2));

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

    @Test
    public void testSingleElementRootCollectionWithIncludedAndFilteredSingleElementSubcollection() {
        given()
                .when().get("/book?filter[book]=title=='Test Book1'&include=authors&filter[author]=name=='Bob1'")
                .then()
                .statusCode(HttpStatus.SC_OK);

        verifyLoggingStatements(
                "SELECT example_Book FROM example.Book AS example_Book LEFT JOIN FETCH example_Book.publisher WHERE example_Book.title IN (:XXX)",
                "SELECT example_Author FROM example.Book example_Book__fetch JOIN example_Book__fetch.authors example_Author WHERE example_Author.name IN (:XXX) AND example_Book__fetch=:XXX",
                "SELECT example_Author FROM example.Book example_Book__fetch JOIN example_Book__fetch.authors example_Author WHERE example_Author.name IN (:XXX) AND example_Book__fetch=:XXX",
                "SELECT example_Book FROM example.Author example_Author__fetch JOIN example_Author__fetch.books example_Book WHERE example_Book.title IN (:XXX) AND example_Author__fetch=:XXX"
        );
    }

    @Test
    public void testSingleElementRootCollectionWithIncludedAndFilteredMultielementSubcollection() {
        given()
                .when().get("/book?filter[book]=title=='Test Book1'&include=authors&filter[author]=name=='Bob*'")
                .then()
                .statusCode(HttpStatus.SC_OK);

        verifyLoggingStatements(
                "SELECT example_Book FROM example.Book AS example_Book LEFT JOIN FETCH example_Book.publisher WHERE example_Book.title IN (:XXX)",
                "SELECT example_Author FROM example.Book example_Book__fetch JOIN example_Book__fetch.authors example_Author WHERE example_Author.name LIKE CONCAT(:XXX, '%') AND example_Book__fetch=:XXX",
                "SELECT example_Author FROM example.Book example_Book__fetch JOIN example_Book__fetch.authors example_Author WHERE example_Author.name LIKE CONCAT(:XXX, '%') AND example_Book__fetch=:XXX"
        );
    }

    @Test
    public void testMultiElementRootCollectionWithIncludedAndFilteredSubcollection() {
        given()
                .when().get("/book?include=authors&filter[author]=name=='Bob1'")
                .then()
                .statusCode(HttpStatus.SC_OK);

        verifyLoggingStatements(
                "SELECT example_Book FROM example.Book AS example_Book LEFT JOIN FETCH example_Book.publisher"
        );
    }

    @Test
    public void testSingleElementSubcollectionWithFilter() {
        given()
                .when().get("/book/1/authors/1/books?filter[book]=title=='Test*'")
                .then()
                .statusCode(HttpStatus.SC_OK);

        verifyLoggingStatements(
                "SELECT example_Book FROM example.Book AS example_Book WHERE example_Book.id IN (:XXX)",
                "SELECT example_Author FROM example.Book example_Book__fetch JOIN example_Book__fetch.authors example_Author WHERE example_Author.id IN (:XXX) AND example_Book__fetch=:XXX",
                "SELECT example_Book FROM example.Author example_Author__fetch JOIN example_Author__fetch.books example_Book LEFT JOIN FETCH example_Book.publisher WHERE example_Book.title LIKE CONCAT(:XXX, '%') AND example_Author__fetch=:XXX"
        );
    }

    private void verifyLoggingStatements(String ... statements) {
        ArgumentCaptor<String> actual = ArgumentCaptor.forClass(String.class);
        verify(logger, times(statements.length)).log(actual.capture());
        List<String> actualAllValues = actual.getAllValues();
        int idx = 0;
        for (String statement : statements) {
            assertEquals(normalizeQuery(statement), normalizeQuery(actualAllValues.get(idx)));
            idx++;
        }
    }

    private static String normalizeQuery(String query) {
        String normalized = query.replaceAll(":\\w+", ":XXX");
        normalized = normalized.trim();
        normalized = normalized.replaceAll("\\s+", " ");
        return normalized;
    }
}
