/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.exceptions.InvalidValueException;
import com.yahoo.elide.core.filter.FilterPredicate;
import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.datastores.search.models.Item;
import com.yahoo.elide.utils.coerce.CoerceUtil;
import com.yahoo.elide.utils.coerce.converters.ISO8601DateSerde;
import org.h2.store.fs.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.Date;
import java.util.HashMap;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DataStoreSupportsFilteringTest {

    private RSQLFilterDialect filterParser;
    private SearchDataStore searchStore;
    private DataStoreTransaction wrappedTransaction;
    private RequestScope mockScope;

    public DataStoreSupportsFilteringTest() {
        EntityDictionary dictionary = new EntityDictionary(new HashMap<>());
        dictionary.bindEntity(Item.class);

        filterParser = new RSQLFilterDialect(dictionary);

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

    @BeforeAll
    public void initialize() {
        FileUtils.createDirectory("/tmp/lucene");
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
                Item.class, false);

        assertEquals(DataStoreTransaction.FeatureSupport.FULL, testTransaction.supportsFiltering(Item.class, filter));
    }

    @Test
    public void testIndexedField() throws Exception {
        /* The field is indexed using the @Field annotation */
        DataStoreTransaction testTransaction = searchStore.beginReadTransaction();
        FilterExpression filter = filterParser.parseFilterExpression("description==*rum*",
                Item.class, false);

        assertEquals(DataStoreTransaction.FeatureSupport.FULL, testTransaction.supportsFiltering(Item.class, filter));
    }

    @Test
    public void testUnindexedField() throws Exception {
        /* The field is not indexed */
        DataStoreTransaction testTransaction = searchStore.beginReadTransaction();
        FilterExpression filter = filterParser.parseFilterExpression("price==123",
                Item.class, false);

        assertEquals(null, testTransaction.supportsFiltering(Item.class, filter));
        verify(wrappedTransaction, times(1)).supportsFiltering(eq(Item.class), eq(filter));
    }

    @Test
    public void testNgramTooSmall() throws Exception {
        DataStoreTransaction testTransaction = searchStore.beginReadTransaction();
        FilterPredicate filter = (FilterPredicate) filterParser.parseFilterExpression("description==*ru*",
                Item.class, false);

        assertThrows(InvalidValueException.class, () -> testTransaction.supportsFiltering(Item.class, filter));
    }

    @Test
    public void testNgramTooLarge() throws Exception {
        DataStoreTransaction testTransaction = searchStore.beginReadTransaction();
        FilterPredicate filter = (FilterPredicate) filterParser.parseFilterExpression("description==*abcdefghijk*",
                Item.class, false);

        assertThrows(InvalidValueException.class, () -> testTransaction.supportsFiltering(Item.class, filter));
    }

    @Test
    public void testLargeNgramForEqualityOperator() throws Exception {
        DataStoreTransaction testTransaction = searchStore.beginReadTransaction();
        FilterPredicate filter = (FilterPredicate) filterParser.parseFilterExpression("description==abcdefghijk",
                Item.class, false);

        assertEquals(null, testTransaction.supportsFiltering(Item.class, filter));
        verify(wrappedTransaction, times(1)).supportsFiltering(eq(Item.class), eq(filter));
    }

    @Test
    public void testNgramJustRight() throws Exception {
        DataStoreTransaction testTransaction = searchStore.beginReadTransaction();
        FilterPredicate filter = (FilterPredicate) filterParser.parseFilterExpression("description==*ruabc*",
                Item.class, false);

        assertEquals(DataStoreTransaction.FeatureSupport.FULL, testTransaction.supportsFiltering(Item.class, filter));
    }

    @Test
    public void testInfixOperator() throws Exception {
        DataStoreTransaction testTransaction = searchStore.beginReadTransaction();
        FilterPredicate filter = (FilterPredicate) filterParser.parseFilterExpression("name==*rum*",
                Item.class, false);

        assertEquals(DataStoreTransaction.FeatureSupport.FULL, testTransaction.supportsFiltering(Item.class, filter));
    }

    @Test
    public void testPrefixOperator() throws Exception {
        DataStoreTransaction testTransaction = searchStore.beginReadTransaction();
        FilterPredicate filter = (FilterPredicate) filterParser.parseFilterExpression("name==drum*",
                Item.class, false);

        assertEquals(DataStoreTransaction.FeatureSupport.PARTIAL, testTransaction.supportsFiltering(Item.class, filter));
    }

    @Test
    public void testEqualityOperator() throws Exception {
        DataStoreTransaction testTransaction = searchStore.beginReadTransaction();
        FilterPredicate filter = (FilterPredicate) filterParser.parseFilterExpression("name==drum",
                Item.class, false);

        assertEquals(null, testTransaction.supportsFiltering(Item.class, filter));
        verify(wrappedTransaction, times(1)).supportsFiltering(eq(Item.class), eq(filter));
    }
}
