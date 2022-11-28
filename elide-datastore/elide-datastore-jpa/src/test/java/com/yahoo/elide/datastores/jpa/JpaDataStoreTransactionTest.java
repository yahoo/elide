/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.jpa;

import static com.yahoo.elide.core.dictionary.EntityDictionary.NO_VERSION;
import static com.yahoo.elide.datastores.jpa.JpaDataStore.DEFAULT_LOGGER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.datastore.DataStoreIterable;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.request.EntityProjection;
import com.yahoo.elide.core.request.Relationship;
import com.yahoo.elide.core.type.ClassType;
import com.yahoo.elide.datastores.jpa.transaction.AbstractJpaTransaction;
import example.Author;
import example.Book;
import org.hibernate.collection.spi.PersistentSet;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class JpaDataStoreTransactionTest {

    protected EntityDictionary dictionary;
    protected RequestScope scope;
    protected EntityManager entityManager;
    protected Query query;

    @BeforeAll
    public void initCommonMocks() {
        dictionary = EntityDictionary.builder().build();
        dictionary.bindEntity(Book.class);
        dictionary.bindEntity(Author.class);

        entityManager = mock(EntityManager.class);
        query = mock(Query.class);
        when(entityManager.createQuery(any(String.class))).thenReturn(query);
        when(query.setParameter(any(String.class), any())).thenReturn(query);
        when(query.setParameter(any(Integer.class), any())).thenReturn(query);

        scope = mock(RequestScope.class);
        when(scope.getDictionary()).thenReturn(dictionary);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void testNoDelegationOnLoadRecords(boolean delegateToInMemory) {

        AbstractJpaTransaction tx = new AbstractJpaTransaction(entityManager, (unused) -> {
        }, DEFAULT_LOGGER, delegateToInMemory) {
            @Override
            public boolean isOpen() {
                return false;
            }

            @Override
            public void begin() {

            }

            @Override
            protected Predicate<Collection<?>> isPersistentCollection() {
                return (unused) -> true;
            };
        };

        EntityProjection projection = EntityProjection.builder()
                .type(Book.class)
                .build();

        DataStoreIterable<Book> result = tx.loadObjects(projection, scope);
        assertFalse(result.needsInMemoryFilter());
        assertFalse(result.needsInMemorySort());
        assertFalse(result.needsInMemoryPagination());
    }

    @ParameterizedTest
    @MethodSource("getTestArguments")
    public void testGetRelationDelegation(
            boolean delegateToInMemory,
            int numberOfAuthors,
            FilterExpression filter,
            boolean usesInMemory) throws Exception {
        AbstractJpaTransaction tx = new AbstractJpaTransaction(entityManager, (unused) -> {

        }, DEFAULT_LOGGER, delegateToInMemory, false) {
            @Override
            public boolean isOpen() {
                return false;
            }

            @Override
            public void begin() {

            }

            @Override
            protected Predicate<Collection<?>> isPersistentCollection() {
                return (unused) -> true;
            };
        };

        EntityProjection projection = EntityProjection.builder()
                .type(Author.class)
                .build();


        List<Author> authors = new ArrayList<>();
        Author author1 = mock(Author.class);
        authors.add(author1);

        for (int idx = 1; idx < numberOfAuthors; idx++) {
            authors.add(mock(Author.class));
        }

        when (query.getResultList()).thenReturn(authors);

        DataStoreIterable<Author> loadedAuthors = tx.loadObjects(projection, scope);
        assertFalse(loadedAuthors.needsInMemoryPagination());
        assertFalse(loadedAuthors.needsInMemorySort());
        assertFalse(loadedAuthors.needsInMemoryFilter());

        Relationship relationship = Relationship.builder()
                .name("books")
                .projection(EntityProjection.builder()
                        .type(Book.class)
                        .filterExpression(filter)
                        .build())
                .build();

        PersistentSet returnCollection = mock(PersistentSet.class);

        when(author1.getBooks()).thenReturn(returnCollection);

        DataStoreIterable<Book> loadedBooks = tx.getToManyRelation(tx, author1, relationship, scope);

        assertEquals(usesInMemory, loadedBooks.needsInMemoryFilter());
        assertEquals(usesInMemory, loadedBooks.needsInMemorySort());
        assertEquals(usesInMemory, loadedBooks.needsInMemoryPagination());
    }

    private Stream<Arguments> getTestArguments() throws Exception {
        RSQLFilterDialect parser = RSQLFilterDialect.builder().dictionary(dictionary).build();

        FilterExpression expression = parser.parse(ClassType.of(Book.class), Collections.emptySet(), "title=='foo'", NO_VERSION);
        return Stream.of(
                arguments(true, 1, null, true),
                arguments(true, 2, null, true),
                arguments(false, 1, null, true),
                arguments(false, 2, null, true),
                arguments(true, 1, expression, false),
                arguments(true, 2, expression, true),
                arguments(false, 1, expression, false),
                arguments(false, 2, expression, false)
        );
    }
}
