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
import static org.mockito.internal.verification.VerificationModeFactory.times;

import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.datastore.inmemory.InMemoryStoreTransaction;
import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.pagination.Pagination;
import com.yahoo.elide.core.sort.Sorting;
import com.yahoo.elide.datastores.search.models.Item;
import com.yahoo.elide.utils.coerce.CoerceUtil;
import com.yahoo.elide.utils.coerce.converters.ISO8601DateSerde;

import com.google.common.collect.Lists;
import org.h2.store.fs.FileUtils;
import org.testng.Assert;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public class SearchDataStoreTest {

    private RSQLFilterDialect filterParser;
    private SearchDataStore searchStore;
    private DataStoreTransaction wrappedTransaction;
    private RequestScope mockScope;

    public SearchDataStoreTest() {
        EntityDictionary dictionary = new EntityDictionary(new HashMap<>());
        dictionary.bindEntity(Item.class);

        filterParser = new RSQLFilterDialect(dictionary);

        DataStore mockStore = mock(DataStore.class);
        wrappedTransaction = mock(DataStoreTransaction.class);
        when(mockStore.beginReadTransaction()).thenReturn(wrappedTransaction);

        EntityManagerFactory emf = Persistence.createEntityManagerFactory("searchDataStoreTest");

        searchStore = new SearchDataStore(mockStore, emf, true);
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
    public void testEqualityPredicate() throws Exception {
        DataStoreTransaction testTransaction = searchStore.beginReadTransaction();

        //Case sensitive query against case insensitive index must lowercase
        FilterExpression filter = filterParser.parseFilterExpression("name==drum", Item.class, false);

        Iterable<Object> loaded = testTransaction.loadObjects(Item.class, Optional.of(filter), Optional.empty(), Optional.empty(), mockScope);

        assertListContains(loaded, Lists.newArrayList());

        /* This query should hit the underlying store */
        verify(wrappedTransaction, times(1)).loadObjects(any(), any(), any(), any(), any());
    }

    @Test
    public void testEscapedPrefixPredicate() throws Exception {

        DataStoreTransaction testTransaction = searchStore.beginReadTransaction();

        /* Verify that '-' is escaped before we run the query */
        FilterExpression filter = filterParser.parseFilterExpression("name==-lucen*", Item.class, false);

        Iterable<Object> loaded = testTransaction.loadObjects(Item.class, Optional.of(filter), Optional.empty(), Optional.empty(), mockScope);

        assertListContains(loaded, Lists.newArrayList(6L));
        verify(wrappedTransaction, never()).loadObjects(any(), any(), any(), any(), any());
    }

    @Test
    public void testEmptyResult() throws Exception {
        DataStoreTransaction testTransaction = searchStore.beginReadTransaction();

        FilterExpression filter = filterParser.parseFilterExpression("name==+lucen*", Item.class, false);

        Iterable<Object> loaded = testTransaction.loadObjects(Item.class, Optional.of(filter), Optional.empty(), Optional.empty(), mockScope);

        assertListContains(loaded, Lists.newArrayList());
        verify(wrappedTransaction, never()).loadObjects(any(), any(), any(), any(), any());
    }

    @Test
    public void testPrefixPredicateWithInMemoryFiltering() throws Exception {
        DataStoreTransaction testTransaction = searchStore.beginReadTransaction();

        testTransaction = new InMemoryStoreTransaction(testTransaction);

        //Case sensitive query against case insensitive index must lowercase
        FilterExpression filter = filterParser.parseFilterExpression("name==dru*", Item.class, false);

        Iterable<Object> loaded = testTransaction.loadObjects(Item.class, Optional.of(filter), Optional.empty(), Optional.empty(), mockScope);

        assertListContains(loaded, Lists.newArrayList());
        verify(wrappedTransaction, never()).loadObjects(any(), any(), any(), any(), any());
    }

    @Test
    public void testPrefixPredicatePhrase() throws Exception {

        DataStoreTransaction testTransaction = searchStore.beginReadTransaction();

        //Case sensitive query against case insensitive index must lowercase
        FilterExpression filter = filterParser.parseFilterExpression("name=='snare\\ dru*'", Item.class, false);

        Iterable<Object> loaded = testTransaction.loadObjects(Item.class, Optional.of(filter), Optional.empty(), Optional.empty(), mockScope);

        assertListContains(loaded, Lists.newArrayList(1L));
        verify(wrappedTransaction, never()).loadObjects(any(), any(), any(), any(), any());
    }

    @Test
    public void testTabCharacter() throws Exception {

        DataStoreTransaction testTransaction = searchStore.beginReadTransaction();

        //Case sensitive query against case insensitive index must lowercase
        FilterExpression filter = filterParser.parseFilterExpression("name=='*est\tTa*'", Item.class, false);

        Iterable<Object> loaded = testTransaction.loadObjects(Item.class, Optional.of(filter), Optional.empty(), Optional.empty(), mockScope);

        assertListContains(loaded, Lists.newArrayList(7L));
        verify(wrappedTransaction, never()).loadObjects(any(), any(), any(), any(), any());
    }

    @Test
    public void testContainsPredicate() throws Exception {

        DataStoreTransaction testTransaction = searchStore.beginReadTransaction();

        //Case insensitive query against case insensitive index
        FilterExpression filter = filterParser.parseFilterExpression("name==*DrU*", Item.class, false);

        Iterable<Object> loaded = testTransaction.loadObjects(Item.class, Optional.of(filter), Optional.empty(), Optional.empty(), mockScope);

        assertListContains(loaded, Lists.newArrayList(1L, 3L));
        verify(wrappedTransaction, never()).loadObjects(any(), any(), any(), any(), any());
    }

    @Test
    public void testPredicateConjunction() throws Exception {

        DataStoreTransaction testTransaction = searchStore.beginReadTransaction();

        FilterExpression filter = filterParser.parseFilterExpression("name==drum*;description==brass*", Item.class, false);

        Iterable<Object> loaded = testTransaction.loadObjects(Item.class, Optional.of(filter), Optional.empty(), Optional.empty(), mockScope);

        assertListContains(loaded, Lists.newArrayList(1L));
        verify(wrappedTransaction, never()).loadObjects(any(), any(), any(), any(), any());
    }

    @Test
    public void testNonIndexedPredicate() throws Exception {

        DataStoreTransaction testTransaction = searchStore.beginReadTransaction();

        FilterExpression filter = filterParser.parseFilterExpression("price==123;description==brass*", Item.class, false);

        Iterable<Object> loaded = testTransaction.loadObjects(Item.class, Optional.of(filter), Optional.empty(), Optional.empty(), mockScope);

        assertListContains(loaded, Lists.newArrayList());

        /* This query should hit the underlying store */
        verify(wrappedTransaction, times(1)).loadObjects(any(), any(), any(), any(), any());
    }

    @Test
    public void testPredicateDisjunction() throws Exception {

        DataStoreTransaction testTransaction = searchStore.beginReadTransaction();

        FilterExpression filter = filterParser.parseFilterExpression("name==drum*,description==ride*", Item.class, false);

        Iterable<Object> loaded = testTransaction.loadObjects(Item.class, Optional.of(filter), Optional.empty(), Optional.empty(), mockScope);

        assertListContains(loaded, Lists.newArrayList(1L, 2L, 3L));
        verify(wrappedTransaction, never()).loadObjects(any(), any(), any(), any(), any());
    }

    @Test
    public void testSortingAscending() throws Exception {

        DataStoreTransaction testTransaction = searchStore.beginReadTransaction();

        Map<String, Sorting.SortOrder> sortRules = new HashMap();
        sortRules.put("name", Sorting.SortOrder.asc);
        sortRules.put("modifiedDate", Sorting.SortOrder.desc);
        Sorting sorting = new Sorting(sortRules);

        FilterExpression filter = filterParser.parseFilterExpression("name==cymbal*", Item.class, false);

        Iterable<Object> loaded = testTransaction.loadObjects(Item.class, Optional.of(filter), Optional.of(sorting), Optional.empty(), mockScope);

        assertListContains(loaded, Lists.newArrayList(4L, 5L, 2L));
        verify(wrappedTransaction, never()).loadObjects(any(), any(), any(), any(), any());
    }

    @Test
    public void testSortingDescending() throws Exception {

        DataStoreTransaction testTransaction = searchStore.beginReadTransaction();

        Map<String, Sorting.SortOrder> sortRules = new HashMap();
        sortRules.put("name", Sorting.SortOrder.desc);
        sortRules.put("modifiedDate", Sorting.SortOrder.asc);
        Sorting sorting = new Sorting(sortRules);

        FilterExpression filter = filterParser.parseFilterExpression("name==cymbal*", Item.class, false);

        Iterable<Object> loaded = testTransaction.loadObjects(Item.class, Optional.of(filter), Optional.of(sorting), Optional.empty(), mockScope);

        assertListMatches(loaded, Lists.newArrayList(2L, 5L, 4L));
        verify(wrappedTransaction, never()).loadObjects(any(), any(), any(), any(), any());
    }

    @Test
    public void testPaginationPageOne() throws Exception {

        DataStoreTransaction testTransaction = searchStore.beginReadTransaction();

        Map<String, Sorting.SortOrder> sortRules = new HashMap();
        sortRules.put("name", Sorting.SortOrder.desc);
        sortRules.put("modifiedDate", Sorting.SortOrder.asc);
        Sorting sorting = new Sorting(sortRules);

        Pagination pagination = Pagination.fromOffsetAndLimit(1, 0, true);

        FilterExpression filter = filterParser.parseFilterExpression("name==cymbal*", Item.class, false);

        Iterable<Object> loaded = testTransaction.loadObjects(Item.class, Optional.of(filter), Optional.of(sorting), Optional.of(pagination), mockScope);

        assertListMatches(loaded, Lists.newArrayList(2L));
        Assert.assertEquals(pagination.getPageTotals(), 3);
        verify(wrappedTransaction, never()).loadObjects(any(), any(), any(), any(), any());
    }

    @Test
    public void testPaginationPageTwo() throws Exception {

        DataStoreTransaction testTransaction = searchStore.beginReadTransaction();

        Map<String, Sorting.SortOrder> sortRules = new HashMap();
        sortRules.put("name", Sorting.SortOrder.desc);
        sortRules.put("modifiedDate", Sorting.SortOrder.asc);
        Sorting sorting = new Sorting(sortRules);

        Pagination pagination = Pagination.fromOffsetAndLimit(1, 1, true);

        FilterExpression filter = filterParser.parseFilterExpression("name==cymbal*", Item.class, false);

        Iterable<Object> loaded = testTransaction.loadObjects(Item.class, Optional.of(filter), Optional.of(sorting), Optional.of(pagination), mockScope);

        assertListMatches(loaded, Lists.newArrayList(5L));
        Assert.assertEquals(pagination.getPageTotals(), 3);
        verify(wrappedTransaction, never()).loadObjects(any(), any(), any(), any(), any());
    }

    @Test
    public void testEscapeWhiteSpace() {
        String toReplace = "Foo\tBar Blah\nFoobar";
        String expected = "Foo\\\tBar\\ Blah\\\nFoobar";

        String actual = FilterExpressionToLuceneQuery.escapeWhiteSpace(toReplace);

        Assert.assertEquals(actual, expected);
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
