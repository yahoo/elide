/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.search;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.datastores.search.models.Item;

import com.google.common.collect.Lists;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public class SearchDataStoreTest {

    private RSQLFilterDialect filterParser;
    private SearchDataStore searchStore;
    private DataStoreTransaction wrappedTransaction;

    public SearchDataStoreTest() {
        EntityDictionary dictionary = new EntityDictionary(new HashMap<>());
        dictionary.bindEntity(Item.class);

        filterParser = new RSQLFilterDialect(dictionary);

        DataStore mockStore = mock(DataStore.class);
        wrappedTransaction = mock(DataStoreTransaction.class);
        when(mockStore.beginReadTransaction()).thenReturn(wrappedTransaction);

        EntityManagerFactory emf = Persistence.createEntityManagerFactory("searchDataStoreTest");

        searchStore = new SearchDataStore(mockStore, emf);
        searchStore.populateEntityDictionary(dictionary);
    }

    @BeforeMethod
    public void beforeMethods() {
        reset(wrappedTransaction);
    }

    @Test
    public void testEqualityPredicate() throws Exception {

        RequestScope mockScope = mock(RequestScope.class);

        DataStoreTransaction testTransaction = searchStore.beginReadTransaction();

        FilterExpression filter = filterParser.parseFilterExpression("name==Drum", Item.class, false);

        Iterable<Object> loaded = testTransaction.loadObjects(Item.class, Optional.of(filter), Optional.empty(), Optional.empty(), mockScope);

        assertListContains(loaded, Lists.newArrayList(1L, 3L));
        verify(wrappedTransaction, never()).loadObjects(any(), any(), any(), any(), any());
    }

    @Test
    public void testContainsPredicate() throws Exception {

        RequestScope mockScope = mock(RequestScope.class);

        DataStoreTransaction testTransaction = searchStore.beginReadTransaction();

        FilterExpression filter = filterParser.parseFilterExpression("name==*Dru*", Item.class, false);

        Iterable<Object> loaded = testTransaction.loadObjects(Item.class, Optional.of(filter), Optional.empty(), Optional.empty(), mockScope);

        assertListContains(loaded, Lists.newArrayList(1L, 3L));
        verify(wrappedTransaction, never()).loadObjects(any(), any(), any(), any(), any());
    }

    @Test
    public void testPredicateConjunction() throws Exception {

        RequestScope mockScope = mock(RequestScope.class);

        DataStoreTransaction testTransaction = searchStore.beginReadTransaction();

        FilterExpression filter = filterParser.parseFilterExpression("name==Drum;description==brass", Item.class, false);

        Iterable<Object> loaded = testTransaction.loadObjects(Item.class, Optional.of(filter), Optional.empty(), Optional.empty(), mockScope);

        assertListContains(loaded, Lists.newArrayList(1L));
        verify(wrappedTransaction, never()).loadObjects(any(), any(), any(), any(), any());
    }

    @Test
    public void testPredicateDisjunction() throws Exception {

        RequestScope mockScope = mock(RequestScope.class);

        DataStoreTransaction testTransaction = searchStore.beginReadTransaction();

        FilterExpression filter = filterParser.parseFilterExpression("name==Drum,description==ride", Item.class, false);

        Iterable<Object> loaded = testTransaction.loadObjects(Item.class, Optional.of(filter), Optional.empty(), Optional.empty(), mockScope);

        assertListContains(loaded, Lists.newArrayList(1L, 2L, 3L));
        verify(wrappedTransaction, never()).loadObjects(any(), any(), any(), any(), any());
    }



    private void assertListMatches(Iterable<Object> actual, List<Long> expectedIds) {
        List<Long> actualIds = StreamSupport.stream(actual.spliterator(), false)
                .map((obj) -> (Item) obj)
                .map(Item::getId)
                .collect(Collectors.toList());

        Assert.assertEquals(actualIds, expectedIds);
    }

    private void assertListContains(Iterable<Object> actual, List<Long> expectedIds) {
        List<Long> actualIds = StreamSupport.stream(actual.spliterator(), false)
                .map((obj) -> (Item) obj)
                .map(Item::getId)
                .sorted()
                .collect(Collectors.toList());

        List<Long> expectedIdsSorted = expectedIds.stream().sorted().collect(Collectors.toList());

        Assert.assertEquals(actualIds, expectedIdsSorted);
    }
}
