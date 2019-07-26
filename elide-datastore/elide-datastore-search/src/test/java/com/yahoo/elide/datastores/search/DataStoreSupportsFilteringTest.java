/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.search;

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
import org.testng.Assert;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import java.util.Date;
import java.util.HashMap;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

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

    @BeforeSuite
    public void initialize() {
        FileUtils.createDirectory("/tmp/lucene");
    }

    @AfterSuite
    public void cleanup() {
        FileUtils.deleteRecursive("/tmp/lucene", false);
    }

    @BeforeMethod
    public void beforeMethods() {
        reset(wrappedTransaction);
    }

    @Test
    public void testIndexedFields() throws Exception {
        /* The field is indexed using the @Fields annotation */
        DataStoreTransaction testTransaction = searchStore.beginReadTransaction();
        FilterExpression filter = filterParser.parseFilterExpression("name==*rum*",
                Item.class, false);

        Assert.assertEquals(testTransaction.supportsFiltering(Item.class, filter), DataStoreTransaction.FeatureSupport.FULL);
    }

    @Test
    public void testIndexedField() throws Exception {
        /* The field is indexed using the @Field annotation */
        DataStoreTransaction testTransaction = searchStore.beginReadTransaction();
        FilterExpression filter = filterParser.parseFilterExpression("description==*rum*",
                Item.class, false);

        Assert.assertEquals(testTransaction.supportsFiltering(Item.class, filter), DataStoreTransaction.FeatureSupport.FULL);
    }

    @Test
    public void testUnindexedField() throws Exception {
        /* The field is not indexed */
        DataStoreTransaction testTransaction = searchStore.beginReadTransaction();
        FilterExpression filter = filterParser.parseFilterExpression("price==123",
                Item.class, false);

        Assert.assertEquals(testTransaction.supportsFiltering(Item.class, filter), null);
        verify(wrappedTransaction, times(1)).supportsFiltering(eq(Item.class), eq(filter));
    }

    @Test(expectedExceptions = InvalidValueException.class)
    public void testNgramTooSmall() throws Exception {
        DataStoreTransaction testTransaction = searchStore.beginReadTransaction();
        FilterPredicate filter = (FilterPredicate) filterParser.parseFilterExpression("description==*ru*",
                Item.class, false);

        testTransaction.supportsFiltering(Item.class, filter);
    }

    @Test(expectedExceptions = InvalidValueException.class)
    public void testNgramTooLarge() throws Exception {
        DataStoreTransaction testTransaction = searchStore.beginReadTransaction();
        FilterPredicate filter = (FilterPredicate) filterParser.parseFilterExpression("description==*abcdefghijk*",
                Item.class, false);

        testTransaction.supportsFiltering(Item.class, filter);
    }

    @Test
    public void testLargeNgramForEqualityOperator() throws Exception {
        DataStoreTransaction testTransaction = searchStore.beginReadTransaction();
        FilterPredicate filter = (FilterPredicate) filterParser.parseFilterExpression("description==abcdefghijk",
                Item.class, false);

        Assert.assertEquals(testTransaction.supportsFiltering(Item.class, filter), null);
        verify(wrappedTransaction, times(1)).supportsFiltering(eq(Item.class), eq(filter));
    }

    @Test
    public void testNgramJustRight() throws Exception {
        DataStoreTransaction testTransaction = searchStore.beginReadTransaction();
        FilterPredicate filter = (FilterPredicate) filterParser.parseFilterExpression("description==*ruabc*",
                Item.class, false);

        Assert.assertEquals(testTransaction.supportsFiltering(Item.class, filter), DataStoreTransaction.FeatureSupport.FULL);
    }

    @Test
    public void testInfixOperator() throws Exception {
        DataStoreTransaction testTransaction = searchStore.beginReadTransaction();
        FilterPredicate filter = (FilterPredicate) filterParser.parseFilterExpression("name==*rum*",
                Item.class, false);

        Assert.assertEquals(testTransaction.supportsFiltering(Item.class, filter), DataStoreTransaction.FeatureSupport.FULL);
    }

    @Test
    public void testPrefixOperator() throws Exception {
        DataStoreTransaction testTransaction = searchStore.beginReadTransaction();
        FilterPredicate filter = (FilterPredicate) filterParser.parseFilterExpression("name==drum*",
                Item.class, false);

        Assert.assertEquals(testTransaction.supportsFiltering(Item.class, filter), DataStoreTransaction.FeatureSupport.PARTIAL);
    }

    @Test
    public void testEqualityOperator() throws Exception {
        DataStoreTransaction testTransaction = searchStore.beginReadTransaction();
        FilterPredicate filter = (FilterPredicate) filterParser.parseFilterExpression("name==drum",
                Item.class, false);

        Assert.assertEquals(testTransaction.supportsFiltering(Item.class, filter), null);
        verify(wrappedTransaction, times(1)).supportsFiltering(eq(Item.class), eq(filter));
    }
}
