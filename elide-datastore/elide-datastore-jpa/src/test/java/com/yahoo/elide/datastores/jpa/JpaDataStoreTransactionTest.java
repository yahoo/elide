/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.jpa;

import static com.yahoo.elide.core.dictionary.EntityDictionary.NO_VERSION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.datastore.DataStore;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.request.EntityProjection;
import com.yahoo.elide.core.type.ClassType;
import com.yahoo.elide.datastores.jpa.transaction.AbstractJpaTransaction;
import example.Author;
import example.Book;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;
import javax.persistence.EntityManager;

public class JpaDataStoreTransactionTest {

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void testNoDelegationOnLoadRecords(boolean delegateToInMemory) {
        EntityManager entityManager = mock(EntityManager.class);

        AbstractJpaTransaction tx = new AbstractJpaTransaction(entityManager, (unused) -> {
        }, delegateToInMemory) {
            @Override
            public boolean isOpen() {
                return false;
            }

            @Override
            public void begin() {

            }
        };

        RequestScope scope = mock(RequestScope.class);
        EntityProjection projection = EntityProjection.builder()
                .type(Book.class)
                .build();
        assertEquals(DataStoreTransaction.FeatureSupport.FULL,
                tx.supportsFiltering(scope, Optional.empty(), projection));
        assertTrue(tx.supportsSorting(scope, Optional.empty(), projection));
        assertTrue(tx.supportsPagination(scope, Optional.empty(), projection));
    }

    @Test
    public void testDelegationOnCollectionOfCollectionsFetch() {
        EntityManager entityManager = mock(EntityManager.class);

        AbstractJpaTransaction tx = new AbstractJpaTransaction(entityManager, (unused) -> {

        }, true) {
            @Override
            public boolean isOpen() {
                return false;
            }

            @Override
            public void begin() {

            }
        };

        RequestScope scope = mock(RequestScope.class);
        EntityProjection projection = EntityProjection.builder()
                .type(Book.class)
                .build();

        Author author = mock(Author.class);
        assertEquals(DataStoreTransaction.FeatureSupport.NONE,
                tx.supportsFiltering(scope, Optional.of(author), projection));
        assertFalse(tx.supportsSorting(scope, Optional.of(author), projection));
        assertFalse(tx.supportsPagination(scope, Optional.of(author), projection));
    }

    @Test
    public void testNoDelegationOnCollectionOfCollectionsFetch() {
        EntityManager entityManager = mock(EntityManager.class);

        AbstractJpaTransaction tx = new AbstractJpaTransaction(entityManager, (unused) -> {

        }, false) {
            @Override
            public boolean isOpen() {
                return false;
            }

            @Override
            public void begin() {

            }
        };

        RequestScope scope = mock(RequestScope.class);
        EntityProjection projection = EntityProjection.builder()
                .type(Book.class)
                .build();

        Author author = mock(Author.class);
        assertEquals(DataStoreTransaction.FeatureSupport.FULL,
                tx.supportsFiltering(scope, Optional.of(author), projection));
        assertTrue(tx.supportsSorting(scope, Optional.of(author), projection));
        assertTrue(tx.supportsPagination(scope, Optional.of(author), projection));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void testNoDelegationOnCollectionOfOneFetch(boolean delegateToInMemory) throws Exception {
        EntityDictionary dictionary = new EntityDictionary(new HashMap<>());

        JpaDataStoreHarness harness = new JpaDataStoreHarness(delegateToInMemory);
        DataStore store = harness.getDataStore();
        store.populateEntityDictionary(dictionary);

        ElideSettings settings = new ElideSettingsBuilder(store)
                .withEntityDictionary(dictionary)
                .build();

        DataStoreTransaction writeTx = store.beginTransaction();
        RequestScope scope = new RequestScope("", "", "", null, writeTx,
                null, null, null, null, settings);

        Author saveAuthor = new Author();
        writeTx.createObject(saveAuthor, scope);
        writeTx.commit(scope);
        writeTx.close();

        DataStoreTransaction readTx = store.beginReadTransaction();
        scope = new RequestScope("", "", "", null, readTx,
                null, null, null, null, settings);

        Author loadedAuthor = readTx.loadObject(EntityProjection.builder().type(Author.class).build(), 1L, scope);

        EntityProjection projection = EntityProjection.builder()
                .type(Book.class)
                .build();
        assertEquals(DataStoreTransaction.FeatureSupport.FULL,
                readTx.supportsFiltering(scope, Optional.of(loadedAuthor), projection));
        assertTrue(readTx.supportsSorting(scope, Optional.of(loadedAuthor), projection));
        assertTrue(readTx.supportsPagination(scope, Optional.of(loadedAuthor), projection));
    }

    @ParameterizedTest
    @ValueSource(strings = {"id==1"})
    public void testDelegationForFilteredRootCollections(String rsqlFilter) throws Exception {
        EntityDictionary dictionary = new EntityDictionary(new HashMap<>());
        dictionary.bindEntity(Author.class);
        dictionary.bindEntity(Book.class);
        RSQLFilterDialect dialect = new RSQLFilterDialect(dictionary);

        FilterExpression expression = dialect.parse(new ClassType(Author.class),
                new HashSet<>(), rsqlFilter, NO_VERSION);

        JpaDataStoreHarness harness = new JpaDataStoreHarness(true);
        DataStore store = harness.getDataStore();
        store.populateEntityDictionary(dictionary);

        ElideSettings settings = new ElideSettingsBuilder(store)
                .withEntityDictionary(dictionary)
                .build();

        DataStoreTransaction writeTx = store.beginTransaction();
        RequestScope scope = new RequestScope("", "", "", null, writeTx,
                null, null, null, null, settings);

        Author saveAuthor = new Author();
        writeTx.createObject(saveAuthor, scope);
        writeTx.commit(scope);
        writeTx.close();

        DataStoreTransaction readTx = store.beginReadTransaction();
        scope = new RequestScope("", "", "", null, readTx,
                null, null, null, null, settings);

        EntityProjection rootLoadProjection = EntityProjection.builder()
                .type(Author.class)
                .filterExpression(expression)
                .build();

        Author loadedAuthor = (Author) readTx.loadObjects(rootLoadProjection,  scope).iterator().next();

        EntityProjection projection = EntityProjection.builder()
                .type(Book.class)
                .build();

        assertEquals(DataStoreTransaction.FeatureSupport.FULL,
                readTx.supportsFiltering(scope, Optional.of(loadedAuthor), projection));
        assertTrue(readTx.supportsSorting(scope, Optional.of(loadedAuthor), projection));
        assertTrue(readTx.supportsPagination(scope, Optional.of(loadedAuthor), projection));
    }

    @ParameterizedTest
    @ValueSource(strings = {"id>0", "name==Foo", "id==1;id==1", "books.authors.id==1", "id=in=(1,2)"})
    public void testNoDelegationForFilteredRootCollections(String rsqlFilter) throws Exception {
        EntityDictionary dictionary = new EntityDictionary(new HashMap<>());
        dictionary.bindEntity(Author.class);
        dictionary.bindEntity(Book.class);
        RSQLFilterDialect dialect = new RSQLFilterDialect(dictionary);

        FilterExpression expression = dialect.parse(new ClassType(Author.class),
                new HashSet<>(), rsqlFilter, NO_VERSION);

        JpaDataStoreHarness harness = new JpaDataStoreHarness(true);
        DataStore store = harness.getDataStore();
        store.populateEntityDictionary(dictionary);

        ElideSettings settings = new ElideSettingsBuilder(store)
                .withEntityDictionary(dictionary)
                .build();

        DataStoreTransaction writeTx = store.beginTransaction();
        RequestScope scope = new RequestScope("", "", "", null, writeTx,
                null, null, null, null, settings);

        Author saveAuthor = new Author();
        Book saveBook = new Book();
        saveAuthor.setName("Foo");
        saveAuthor.setBooks(Arrays.asList(saveBook));
        saveBook.setAuthors(Arrays.asList(saveAuthor));
        writeTx.createObject(saveAuthor, scope);
        writeTx.createObject(saveBook, scope);
        writeTx.commit(scope);
        writeTx.close();

        DataStoreTransaction readTx = store.beginReadTransaction();
        scope = new RequestScope("", "", "", null, readTx,
                null, null, null, null, settings);

        EntityProjection rootLoadProjection = EntityProjection.builder()
                .type(Author.class)
                .filterExpression(expression)
                .build();

        Author loadedAuthor = (Author) readTx.loadObjects(rootLoadProjection,  scope).iterator().next();

        EntityProjection projection = EntityProjection.builder()
                .type(Book.class)
                .build();

        assertEquals(DataStoreTransaction.FeatureSupport.NONE,
                readTx.supportsFiltering(scope, Optional.of(loadedAuthor), projection));
        assertFalse(readTx.supportsSorting(scope, Optional.of(loadedAuthor), projection));
        assertFalse(readTx.supportsPagination(scope, Optional.of(loadedAuthor), projection));
    }
}
