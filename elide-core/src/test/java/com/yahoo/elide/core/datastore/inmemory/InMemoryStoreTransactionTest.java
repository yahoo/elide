/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.datastore.inmemory;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.filter.InPredicate;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import example.Author;
import example.Book;
import example.Editor;
import example.Publisher;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Optional;

public class InMemoryStoreTransactionTest {

    private DataStoreTransaction wrappedTransaction = mock(DataStoreTransaction.class);
    private RequestScope scope = mock(RequestScope.class);
    private InMemoryStoreTransaction inMemoryStoreTransaction = new InMemoryStoreTransaction(wrappedTransaction);
    private EntityDictionary dictionary;

    @BeforeTest
    public void init() {
        dictionary = new EntityDictionary(new HashMap<>());
        dictionary.bindEntity(Book.class);
        dictionary.bindEntity(Author.class);
        dictionary.bindEntity(Editor.class);
        dictionary.bindEntity(Publisher.class);
        when(scope.getDictionary()).thenReturn(dictionary);
    }

    @BeforeMethod
    public void resetMocks() {
        reset(wrappedTransaction);
    }

    @Test
    public void testFullFilterPredicatePushDown() {
        FilterExpression expression =
                new InPredicate(new Path(Book.class, dictionary, "genre"), "Literary Fiction");

        Optional filterExpressionOp = Optional.of(expression);
        Optional empty = Optional.empty();

        inMemoryStoreTransaction.loadObjects(
                Book.class,
                filterExpressionOp,
                empty,
                empty,
                scope);

        verify(wrappedTransaction, times(1)).loadObjects(
                eq(Book.class),
                eq(filterExpressionOp),
                eq(empty),
                eq(empty),
                eq(scope));
    }

    @Test
    public void testTransactionRequiresInMemoryFilter() {
        FilterExpression expression =
                new InPredicate(new Path(Book.class, dictionary, "genre"), "Literary Fiction");

        when(scope.isMutatingMultipleEntities()).thenReturn(true);

        Optional filterExpressionOp = Optional.of(expression);
        Optional empty = Optional.empty();

        inMemoryStoreTransaction.loadObjects(
                Book.class,
                filterExpressionOp,
                empty,
                empty,
                scope);

        verify(wrappedTransaction, times(1)).loadObjects(
                eq(Book.class),
                eq(empty),
                eq(empty),
                eq(empty),
                eq(scope));
    }

    @Test
    public void testDataStoreRequiresTotalInMemoryFilter() {
        FilterExpression expression =
                new InPredicate(new Path(Book.class, dictionary, "genre"), "Literary Fiction");

        when(wrappedTransaction.supportsFiltering(eq(Book.class),
                any())).thenReturn(DataStoreTransaction.FeatureSupport.NONE);

        Optional filterExpressionOp = Optional.of(expression);
        Optional empty = Optional.empty();

        inMemoryStoreTransaction.loadObjects(
                Book.class,
                filterExpressionOp,
                empty,
                empty,
                scope);

        verify(wrappedTransaction, times(1)).loadObjects(
                eq(Book.class),
                eq(empty),
                eq(empty),
                eq(empty),
                eq(scope));
    }

    @Test
    public void testDataStoreRequiresPartialInMemoryFilter() {
        FilterExpression expression =
                new InPredicate(new Path(Book.class, dictionary, "genre"), "Literary Fiction");

        when(wrappedTransaction.supportsFiltering(eq(Book.class),
                any())).thenReturn(DataStoreTransaction.FeatureSupport.PARTIAL);

        Optional filterExpressionOp = Optional.of(expression);
        Optional empty = Optional.empty();

        inMemoryStoreTransaction.loadObjects(
                Book.class,
                filterExpressionOp,
                empty,
                empty,
                scope);

        verify(wrappedTransaction, times(1)).loadObjects(
                eq(Book.class),
                eq(filterExpressionOp),
                eq(empty),
                eq(empty),
                eq(scope));
    }
}
