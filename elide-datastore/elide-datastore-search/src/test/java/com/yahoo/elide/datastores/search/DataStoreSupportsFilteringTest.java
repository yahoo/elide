/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.search;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.datastore.DataStore;
import com.yahoo.elide.core.datastore.DataStoreIterable;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.exceptions.InvalidValueException;
import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.predicates.FilterPredicate;
import com.yahoo.elide.core.request.EntityProjection;
import com.yahoo.elide.core.type.ClassType;
import com.yahoo.elide.core.utils.coerce.CoerceUtil;
import com.yahoo.elide.core.utils.coerce.converters.ISO8601DateSerde;
import com.yahoo.elide.datastores.search.models.Item;
import org.h2.store.fs.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.util.Date;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DataStoreSupportsFilteringTest {

    private RSQLFilterDialect filterParser;
    private SearchDataStore searchStore;
    private DataStoreTransaction wrappedTransaction;
    private RequestScope mockScope;

    public DataStoreSupportsFilteringTest() {
        EntityDictionary dictionary = EntityDictionary.builder().build();
        dictionary.bindEntity(Item.class);

        filterParser = RSQLFilterDialect.builder().dictionary(dictionary).build();

        DataStore mockStore = mock(DataStore.class);
        wrappedTransaction = mock(DataStoreTransaction.class);
        when(mockStore.beginReadTransaction()).thenReturn(wrappedTransaction);

        EntityManagerFactory emf = Persistence.createEntityManagerFactory("searchDataStoreTest");

        searchStore = new SearchDataStore(mockStore, emf, true, 3, 10);
        searchStore.populateEntityDictionary(dictionary);

        mockScope = mock(RequestScope.class);
        when(mockScope.getDictionary()).thenReturn(dictionary);

        CoerceUtil.register(Date.class, new ISO8601DateSerde());
    }

    @AfterAll
    public void cleanup() {
        FileUtils.deleteRecursive("/tmp/lucene", false);
    }

    @BeforeEach
    public void beforeMethods() {
        reset(wrappedTransaction);
    }

    @Test
    public void testIndexedFields() throws Exception {
        /* The field is indexed using the @Fields annotation */
        DataStoreTransaction testTransaction = searchStore.beginReadTransaction();
        FilterExpression filter = filterParser.parseFilterExpression("name==*rum*",
                ClassType.of(Item.class), false);

        EntityProjection projection = EntityProjection.builder()
                .type(Item.class)
                .filterExpression(filter)
                .build();

        DataStoreIterable<Object> loaded = testTransaction.loadObjects(projection, mockScope);

        assertFalse(loaded.needsInMemoryFilter());
        assertFalse(loaded.needsInMemoryPagination());
        assertFalse(loaded.needsInMemorySort());
        verify(wrappedTransaction, times(0)).loadObjects(any(), any());
    }

    @Test
    public void testIndexedField() throws Exception {
        /* The field is indexed using the @Field annotation */
        DataStoreTransaction testTransaction = searchStore.beginReadTransaction();
        FilterExpression filter = filterParser.parseFilterExpression("description==*rum*",
                ClassType.of(Item.class), false);

        EntityProjection projection = EntityProjection.builder()
                .type(Item.class)
                .filterExpression(filter)
                .build();

        DataStoreIterable<Object> loaded = testTransaction.loadObjects(projection, mockScope);

        assertFalse(loaded.needsInMemoryFilter());
        assertFalse(loaded.needsInMemoryPagination());
        assertFalse(loaded.needsInMemorySort());
        verify(wrappedTransaction, times(0)).loadObjects(any(), any());
    }

    @Test
    public void testUnindexedField() throws Exception {
        /* The field is not indexed */
        DataStoreTransaction testTransaction = searchStore.beginReadTransaction();
        FilterExpression filter = filterParser.parseFilterExpression("price==123",
                ClassType.of(Item.class), false);

        EntityProjection projection = EntityProjection.builder()
                .type(Item.class)
                .filterExpression(filter)
                .build();

        DataStoreIterable<Object> loaded = testTransaction.loadObjects(projection, mockScope);
        //The query was passed to the wrapped transaction which is a mock.
        assertNull(loaded);
        verify(wrappedTransaction, times(1)).loadObjects(any(), any());
    }

    @Test
    public void testNgramTooSmall() throws Exception {
        DataStoreTransaction testTransaction = searchStore.beginReadTransaction();
        FilterPredicate filter = (FilterPredicate) filterParser.parseFilterExpression("description==*ru*",
                ClassType.of(Item.class), false);

        EntityProjection projection = EntityProjection.builder()
                .type(Item.class)
                .filterExpression(filter)
                .build();

        assertThrows(InvalidValueException.class, () -> testTransaction.loadObjects(projection, mockScope));
    }

    @Test
    public void testNgramTooLarge() throws Exception {
        DataStoreTransaction testTransaction = searchStore.beginReadTransaction();
        FilterPredicate filter = (FilterPredicate) filterParser.parseFilterExpression("description==*abcdefghijk*",
                ClassType.of(Item.class), false);

        EntityProjection projection = EntityProjection.builder()
                .type(Item.class)
                .filterExpression(filter)
                .build();

        assertThrows(InvalidValueException.class, () -> testTransaction.loadObjects(projection, mockScope));
    }

    @Test
    public void testLargeNgramForEqualityOperator() throws Exception {
        DataStoreTransaction testTransaction = searchStore.beginReadTransaction();
        FilterPredicate filter = (FilterPredicate) filterParser.parseFilterExpression("description==abcdefghijk",
                ClassType.of(Item.class), false);

        EntityProjection projection = EntityProjection.builder()
                .type(Item.class)
                .filterExpression(filter)
                .build();

        DataStoreIterable<Object> loaded = testTransaction.loadObjects(projection, mockScope);
        //The query was passed to the wrapped transaction which is a mock.
        assertNull(loaded);
        verify(wrappedTransaction, times(1)).loadObjects(any(), any());
    }

    @Test
    public void testNgramJustRight() throws Exception {
        DataStoreTransaction testTransaction = searchStore.beginReadTransaction();
        FilterPredicate filter = (FilterPredicate) filterParser.parseFilterExpression("description==*ruabc*",
                ClassType.of(Item.class), false);

        EntityProjection projection = EntityProjection.builder()
                .type(Item.class)
                .filterExpression(filter)
                .build();

        DataStoreIterable<Object> loaded = testTransaction.loadObjects(projection, mockScope);
        assertFalse(loaded.needsInMemoryFilter());
        assertFalse(loaded.needsInMemoryPagination());
        assertFalse(loaded.needsInMemorySort());
        verify(wrappedTransaction, times(0)).loadObjects(any(), any());
    }

    @Test
    public void testInfixOperator() throws Exception {
        DataStoreTransaction testTransaction = searchStore.beginReadTransaction();
        FilterPredicate filter = (FilterPredicate) filterParser.parseFilterExpression("name==*rum*",
                ClassType.of(Item.class), false);

        EntityProjection projection = EntityProjection.builder()
                .type(Item.class)
                .filterExpression(filter)
                .build();

        DataStoreIterable<Object> loaded = testTransaction.loadObjects(projection, mockScope);
        assertFalse(loaded.needsInMemoryFilter());
        assertFalse(loaded.needsInMemoryPagination());
        assertFalse(loaded.needsInMemorySort());
        verify(wrappedTransaction, times(0)).loadObjects(any(), any());
    }

    @Test
    public void testPrefixOperator() throws Exception {
        DataStoreTransaction testTransaction = searchStore.beginReadTransaction();
        FilterPredicate filter = (FilterPredicate) filterParser.parseFilterExpression("name==drum*",
                ClassType.of(Item.class), false);

        EntityProjection projection = EntityProjection.builder()
                .type(Item.class)
                .filterExpression(filter)
                .build();

        DataStoreIterable<Object> loaded = testTransaction.loadObjects(projection, mockScope);
        assertTrue(loaded.needsInMemoryFilter());
        assertTrue(loaded.needsInMemoryPagination());
        assertTrue(loaded.needsInMemorySort());
        verify(wrappedTransaction, times(0)).loadObjects(any(), any());
    }

    @Test
    public void testEqualityOperator() throws Exception {
        DataStoreTransaction testTransaction = searchStore.beginReadTransaction();
        FilterPredicate filter = (FilterPredicate) filterParser.parseFilterExpression("name==drum",
                ClassType.of(Item.class), false);

        EntityProjection projection = EntityProjection.builder()
                .type(Item.class)
                .filterExpression(filter)
                .build();

        DataStoreIterable<Object> loaded = testTransaction.loadObjects(projection, mockScope);
        //The query was passed to the wrapped transaction which is a mock.
        assertNull(loaded);
        verify(wrappedTransaction, times(1)).loadObjects(any(), any());
    }
}
