/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.datastore.DataStore;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.datastore.inmemory.InMemoryStoreTransaction;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.pagination.PaginationImpl;
import com.yahoo.elide.core.request.EntityProjection;
import com.yahoo.elide.core.request.Sorting;
import com.yahoo.elide.core.sort.SortingImpl;
import com.yahoo.elide.core.type.ClassType;
import com.yahoo.elide.core.utils.coerce.CoerceUtil;
import com.yahoo.elide.core.utils.coerce.converters.ISO8601DateSerde;
import com.yahoo.elide.datastores.search.models.Item;
import com.google.common.collect.Lists;
import org.h2.store.fs.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DataStoreLoadTest {

    private RSQLFilterDialect filterParser;
    private SearchDataStore searchStore;
    private DataStoreTransaction wrappedTransaction;
    private RequestScope mockScope;
    private EntityDictionary dictionary;

    public DataStoreLoadTest() {
        dictionary = EntityDictionary.builder().build();
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
    public void testEqualityPredicate() throws Exception {
        DataStoreTransaction testTransaction = searchStore.beginReadTransaction();

        //Case sensitive query against case insensitive index must lowercase
        FilterExpression filter = filterParser.parseFilterExpression("name==drum", ClassType.of(Item.class), false);

        Iterable<Object> loaded = testTransaction.loadObjects(
                EntityProjection.builder()
                .type(Item.class)
                .filterExpression(filter)
                .build(), mockScope);

        assertNull(loaded);

        /* This query should hit the underlying store */
        verify(wrappedTransaction, times(1)).loadObjects(any(), any());
    }

    @Test
    public void testEscapedPrefixPredicate() throws Exception {

        DataStoreTransaction testTransaction = searchStore.beginReadTransaction();

        /* Verify that '-' is escaped before we run the query */
        FilterExpression filter = filterParser.parseFilterExpression("name==-lucen*", ClassType.of(Item.class), false);

        Iterable<Object> loaded = testTransaction.loadObjects(EntityProjection.builder()
                .type(Item.class)
                .filterExpression(filter)
                .build(), mockScope);

        assertListContains(loaded, Lists.newArrayList(6L));
        verify(wrappedTransaction, never()).loadObjects(any(), any());
    }

    @Test
    public void testEmptyResult() throws Exception {
        DataStoreTransaction testTransaction = searchStore.beginReadTransaction();

        FilterExpression filter = filterParser.parseFilterExpression("name==+lucen*", ClassType.of(Item.class), false);

        Iterable<Object> loaded = testTransaction.loadObjects(EntityProjection.builder()
                .type(Item.class)
                .filterExpression(filter)
                .build(), mockScope);

        assertListContains(loaded, Lists.newArrayList());
        verify(wrappedTransaction, never()).loadObjects(any(), any());
    }

    @Test
    public void testPrefixPredicateWithInMemoryFiltering() throws Exception {
        DataStoreTransaction testTransaction = searchStore.beginReadTransaction();

        testTransaction = new InMemoryStoreTransaction(testTransaction);

        //Case sensitive query against case insensitive index must lowercase
        FilterExpression filter = filterParser.parseFilterExpression("name==dru*", ClassType.of(Item.class), false);

        Iterable<Object> loaded = testTransaction.loadObjects(EntityProjection.builder()
                .type(Item.class)
                .filterExpression(filter)
                .build(), mockScope);

        assertListContains(loaded, Lists.newArrayList());
        verify(wrappedTransaction, never()).loadObjects(any(), any());
    }

    @Test
    public void testPrefixPredicatePhrase() throws Exception {

        DataStoreTransaction testTransaction = searchStore.beginReadTransaction();

        //Case sensitive query against case insensitive index must lowercase
        FilterExpression filter = filterParser.parseFilterExpression("name=='snare\\ dru*'", ClassType.of(Item.class), false);

        Iterable<Object> loaded = testTransaction.loadObjects(EntityProjection.builder()
                .type(Item.class)
                .filterExpression(filter)
                .build(), mockScope);

        assertListContains(loaded, Lists.newArrayList(1L));
        verify(wrappedTransaction, never()).loadObjects(any(), any());
    }

    @Test
    public void testTabCharacter() throws Exception {

        DataStoreTransaction testTransaction = searchStore.beginReadTransaction();

        //Case sensitive query against case insensitive index must lowercase
        FilterExpression filter = filterParser.parseFilterExpression("name=ini='*est\tTa*'", ClassType.of(Item.class), false);

        Iterable<Object> loaded = testTransaction.loadObjects(EntityProjection.builder()
                .type(Item.class)
                .filterExpression(filter)
                .build(), mockScope);

        assertListContains(loaded, Lists.newArrayList(7L));
        verify(wrappedTransaction, never()).loadObjects(any(), any());
    }

    @Test
    public void testContainsPredicate() throws Exception {

        DataStoreTransaction testTransaction = searchStore.beginReadTransaction();

        //Case insensitive query against case insensitive index
        FilterExpression filter = filterParser.parseFilterExpression("name=ini=*DrU*", ClassType.of(Item.class), false);

        Iterable<Object> loaded = testTransaction.loadObjects(EntityProjection.builder()
                .type(Item.class)
                .filterExpression(filter)
                .build(), mockScope);

        assertListContains(loaded, Lists.newArrayList(1L, 3L));
        verify(wrappedTransaction, never()).loadObjects(any(), any());
    }

    @Test
    public void testPredicateConjunction() throws Exception {

        DataStoreTransaction testTransaction = searchStore.beginReadTransaction();

        FilterExpression filter = filterParser.parseFilterExpression("name==drum*;description==brass*", ClassType.of(Item.class), false);

        Iterable<Object> loaded = testTransaction.loadObjects(EntityProjection.builder()
                .type(Item.class)
                .filterExpression(filter)
                .build(), mockScope);

        assertListContains(loaded, Lists.newArrayList(1L));
        verify(wrappedTransaction, never()).loadObjects(any(), any());
    }

    @Test
    public void testNonIndexedPredicate() throws Exception {

        DataStoreTransaction testTransaction = searchStore.beginReadTransaction();

        FilterExpression filter = filterParser.parseFilterExpression("price==123;description==brass*", ClassType.of(Item.class), false);

        Iterable<Object> loaded = testTransaction.loadObjects(EntityProjection.builder()
                .type(Item.class)
                .filterExpression(filter)
                .build(), mockScope);

        assertNull(loaded);

        /* This query should hit the underlying store */
        verify(wrappedTransaction, times(1)).loadObjects(any(), any());
    }

    @Test
    public void testPredicateDisjunction() throws Exception {

        DataStoreTransaction testTransaction = searchStore.beginReadTransaction();

        FilterExpression filter = filterParser.parseFilterExpression("name==drum*,description==ride*", ClassType.of(Item.class), false);

        Iterable<Object> loaded = testTransaction.loadObjects(EntityProjection.builder()
                .type(Item.class)
                .filterExpression(filter)
                .build(), mockScope);

        assertListContains(loaded, Lists.newArrayList(1L, 2L, 3L));
        verify(wrappedTransaction, never()).loadObjects(any(), any());
    }

    @Test
    public void testSortingAscending() throws Exception {

        DataStoreTransaction testTransaction = searchStore.beginReadTransaction();

        Map<String, Sorting.SortOrder> sortRules = new HashMap<>();
        sortRules.put("name", Sorting.SortOrder.asc);
        sortRules.put("modifiedDate", Sorting.SortOrder.desc);
        Sorting sorting = new SortingImpl(sortRules, Item.class, dictionary);

        FilterExpression filter = filterParser.parseFilterExpression("name==cymbal*", ClassType.of(Item.class), false);

        Iterable<Object> loaded = testTransaction.loadObjects(EntityProjection.builder()
                .type(Item.class)
                .filterExpression(filter)
                .sorting(sorting)
                .build(), mockScope);

        assertListContains(loaded, Lists.newArrayList(4L, 5L, 2L));
        verify(wrappedTransaction, never()).loadObjects(any(), any());
    }

    @Test
    public void testSortingDescending() throws Exception {

        DataStoreTransaction testTransaction = searchStore.beginReadTransaction();

        Map<String, Sorting.SortOrder> sortRules = new HashMap<>();
        sortRules.put("name", Sorting.SortOrder.desc);
        sortRules.put("modifiedDate", Sorting.SortOrder.asc);
        Sorting sorting = new SortingImpl(sortRules, Item.class, dictionary);

        FilterExpression filter = filterParser.parseFilterExpression("name==cymbal*", ClassType.of(Item.class), false);

        Iterable<Object> loaded = testTransaction.loadObjects(EntityProjection.builder()
                .type(Item.class)
                .filterExpression(filter)
                .sorting(sorting)
                .build(), mockScope);

        assertListMatches(loaded, Lists.newArrayList(2L, 5L, 4L));
        verify(wrappedTransaction, never()).loadObjects(any(), any());
    }

    @Test
    public void testPaginationPageOne() throws Exception {

        DataStoreTransaction testTransaction = searchStore.beginReadTransaction();

        Map<String, Sorting.SortOrder> sortRules = new HashMap<>();
        sortRules.put("name", Sorting.SortOrder.desc);
        sortRules.put("modifiedDate", Sorting.SortOrder.asc);
        Sorting sorting = new SortingImpl(sortRules, Item.class, dictionary);

        PaginationImpl pagination = new PaginationImpl(Item.class, 0, 1,
                PaginationImpl.DEFAULT_PAGE_LIMIT,
                PaginationImpl.MAX_PAGE_LIMIT,
                true,
                false);

        FilterExpression filter = filterParser.parseFilterExpression("name==cymbal*", ClassType.of(Item.class), false);

        Iterable<Object> loaded = testTransaction.loadObjects(EntityProjection.builder()
                .type(Item.class)
                .filterExpression(filter)
                .sorting(sorting)
                .pagination(pagination)
                .build(), mockScope);

        assertListMatches(loaded, Lists.newArrayList(2L));
        assertEquals(pagination.getPageTotals(), 3);
        verify(wrappedTransaction, never()).loadObjects(any(), any());
    }

    @Test
    public void testPaginationPageTwo() throws Exception {

        DataStoreTransaction testTransaction = searchStore.beginReadTransaction();

        Map<String, Sorting.SortOrder> sortRules = new HashMap<>();
        sortRules.put("name", Sorting.SortOrder.desc);
        sortRules.put("modifiedDate", Sorting.SortOrder.asc);
        Sorting sorting = new SortingImpl(sortRules, Item.class, dictionary);

        PaginationImpl pagination = new PaginationImpl(Item.class, 1, 1,
                PaginationImpl.DEFAULT_PAGE_LIMIT,
                PaginationImpl.MAX_PAGE_LIMIT,
                true,
                false);

        FilterExpression filter = filterParser.parseFilterExpression("name==cymbal*", ClassType.of(Item.class), false);

        Iterable<Object> loaded = testTransaction.loadObjects(EntityProjection.builder()
                .type(Item.class)
                .filterExpression(filter)
                .sorting(sorting)
                .pagination(pagination)
                .build(), mockScope);

        assertListMatches(loaded, Lists.newArrayList(5L));
        assertEquals(pagination.getPageTotals(), 3);
        verify(wrappedTransaction, never()).loadObjects(any(), any());
    }

    @Test
    public void testEscapeWhiteSpace() {
        String toReplace = "Foo\tBar Blah\nFoobar";
        String expected = "Foo\\\tBar\\ Blah\\\nFoobar";

        String actual = FilterExpressionToLuceneQuery.escapeWhiteSpace(toReplace);

        assertEquals(expected, actual);
    }

    private void assertListMatches(Iterable<Object> actual, List<Long> expectedIds) {
        List<Long> actualIds = StreamSupport.stream(actual.spliterator(), false)
                .map((obj) -> (Item) obj)
                .map(Item::getId)
                .collect(Collectors.toList());

        assertEquals(expectedIds, actualIds);
    }

    private void assertListContains(Iterable<Object> actual, List<Long> expectedIds) {
        List<Long> actualIds = StreamSupport.stream(actual.spliterator(), false)
                .map((obj) -> (Item) obj)
                .map(Item::getId)
                .sorted()
                .collect(Collectors.toList());

        List<Long> expectedIdsSorted = expectedIds.stream().sorted().collect(Collectors.toList());

        assertEquals(expectedIdsSorted, actualIds);
    }
}
