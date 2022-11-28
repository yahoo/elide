/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.jpa;

import static io.restassured.RestAssured.given;
import static org.mockito.Mockito.reset;

import com.yahoo.elide.core.datastore.DataStoreTransaction;
import example.Author;
import example.Book;
import example.Publisher;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;

/**
 * Verifies JPA Store reduces JPQL queries to solve N+1 problem.
 */
public class NPlusOneIT extends JPQLIntegrationTest {

    @Override
    protected boolean delegateToInMemoryStore() {
        return true;
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

            Book book3 = new Book();
            book3.setTitle("Test Book3");
            tx.createObject(book3, null);

            Author author1 = new Author();
            author1.setName("Bob1");
            tx.createObject(author1, null);

            Author author2 = new Author();
            author2.setName("Bob2");
            tx.createObject(author2, null);

            Author author3 = new Author();
            author3.setName("Bob2");
            tx.createObject(author3, null);

            Publisher publisher = new Publisher();
            tx.createObject(publisher, null);

            author1.setBooks(Arrays.asList(book1, book2));
            author2.setBooks(Arrays.asList(book1, book2));
            author3.setBooks(Arrays.asList(book3));
            book1.setAuthors(Arrays.asList(author1, author2));
            book2.setAuthors(Arrays.asList(author1, author2));
            book3.setAuthors(Arrays.asList(author3));

            book1.setPublisher(publisher);
            book2.setPublisher(publisher);
            publisher.setBooks(new HashSet<>(Arrays.asList(book1, book2)));

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

    @Test
    public void testToOneRelationshipSubcollectionWithFilter() {
        given()
                .when().get("/book/1/publisher/1/books?filter[book]=title=='The'")
                .then()
                .statusCode(HttpStatus.SC_OK);

        verifyLoggingStatements(
                "SELECT example_Book FROM example.Book AS example_Book LEFT JOIN FETCH example_Book.publisher WHERE example_Book.id IN (:XXX)",
                "SELECT example_Book FROM example.Publisher example_Publisher__fetch JOIN example_Publisher__fetch.books example_Book WHERE example_Book.title IN (:XXX) AND example_Publisher__fetch=:XXX"
        );
    }

    @Test
    public void testToOneRelationshipSubcollectionWithoutFilter() {
        given()
                .when().get("/book/1/publisher/1/books")
                .then()
                .statusCode(HttpStatus.SC_OK);

        verifyLoggingStatements(
                "SELECT example_Book FROM example.Book AS example_Book LEFT JOIN FETCH example_Book.publisher WHERE example_Book.id IN (:XXX)"
        );
    }

    @Test
    public void testSingleElementPersistentCollection() {
        given()
                .when().get("/book?filter[book]=title=='Test Book3'&include=authors,authors.books")
                .then()
                .statusCode(HttpStatus.SC_OK);

        verifyLoggingStatements(
                "SELECT example_Book FROM example.Book AS example_Book LEFT JOIN FETCH example_Book.publisher WHERE example_Book.title IN (:XXX)",
                "SELECT example_Book FROM example.Author example_Author__fetch JOIN example_Author__fetch.books example_Book WHERE example_Book.title IN (:XXX) AND example_Author__fetch=:XXX",
                "SELECT example_Book FROM example.Author example_Author__fetch JOIN example_Author__fetch.books example_Book WHERE example_Book.title IN (:XXX) AND example_Author__fetch=:XXX",
                "SELECT example_Book FROM example.Author example_Author__fetch JOIN example_Author__fetch.books example_Book LEFT JOIN FETCH example_Book.publisher WHERE example_Book.title IN (:XXX) AND example_Author__fetch=:XXX"
        );
    }
}
