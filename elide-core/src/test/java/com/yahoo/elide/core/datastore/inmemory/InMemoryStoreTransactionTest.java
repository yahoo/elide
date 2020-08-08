/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.datastore.inmemory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.filter.InPredicate;
import com.yahoo.elide.core.filter.expression.AndFilterExpression;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.pagination.Pagination;
import com.yahoo.elide.core.sort.Sorting;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import example.Author;
import example.Book;
import example.Editor;
import example.Publisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class InMemoryStoreTransactionTest {

    private DataStoreTransaction wrappedTransaction = mock(DataStoreTransaction.class);
    private RequestScope scope = mock(RequestScope.class);
    private InMemoryStoreTransaction inMemoryStoreTransaction = new InMemoryStoreTransaction(wrappedTransaction);
    private EntityDictionary dictionary;
    private Set<Book> books = new HashSet<>();
    private Book book1;
    private Book book2;
    private Book book3;
    private Author author;
    private ElideSettings elideSettings;

    public InMemoryStoreTransactionTest() {
        dictionary = new EntityDictionary(new HashMap<>());
        dictionary.bindEntity(Book.class);
        dictionary.bindEntity(Author.class);
        dictionary.bindEntity(Editor.class);
        dictionary.bindEntity(Publisher.class);

        elideSettings = new ElideSettingsBuilder(null).build();

        author = new Author();

        Editor editor1 = new Editor();
        editor1.setFirstName("Jon");
        editor1.setLastName("Doe");

        Editor editor2 = new Editor();
        editor2.setFirstName("Jane");
        editor2.setLastName("Doe");

        Publisher publisher1 = new Publisher();
        publisher1.setEditor(editor1);

        Publisher publisher2 = new Publisher();
        publisher2.setEditor(editor2);

        book1 = new Book(1,
                "Book 1",
                "Literary Fiction",
                "English",
                System.currentTimeMillis(),
                Sets.newHashSet(author),
                publisher1,
                Arrays.asList("Prize1"));

        book2 = new Book(2,
                "Book 2",
                "Science Fiction",
                "English",
                System.currentTimeMillis(),
                Sets.newHashSet(author),
                publisher1,
                Arrays.asList("Prize1", "Prize2"));

        book3 = new Book(3,
                "Book 3",
                "Literary Fiction",
                "English",
                System.currentTimeMillis(),
                Sets.newHashSet(author),
                publisher2,
                Arrays.asList());

        books.add(book1);
        books.add(book2);
        books.add(book3);

        author.setBooks(new ArrayList<>(books));

        when(scope.getDictionary()).thenReturn(dictionary);
    }

    @BeforeEach
    public void resetMocks() {
        reset(wrappedTransaction);
    }

    @Test
    public void testFullFilterPredicatePushDown() {
        FilterExpression expression =
                new InPredicate(new Path(Book.class, dictionary, "genre"), "Literary Fiction");

        when(wrappedTransaction.supportsFiltering(eq(Book.class),
                any())).thenReturn(DataStoreTransaction.FeatureSupport.FULL);
        when(wrappedTransaction.loadObjects(eq(Book.class), eq(Optional.of(expression)),
                eq(Optional.empty()), eq(Optional.empty()), eq(scope))).thenReturn((Set) books);

        Collection<Object> loaded = (Collection<Object>) inMemoryStoreTransaction.loadObjects(
                Book.class,
                Optional.of(expression),
                Optional.empty(),
                Optional.empty(),
                scope);

        verify(wrappedTransaction, times(1)).loadObjects(
                eq(Book.class),
                eq(Optional.of(expression)),
                eq(Optional.empty()),
                eq(Optional.empty()),
                eq(scope));

        assertEquals(3, loaded.size());
        assertTrue(loaded.contains(book1));
        assertTrue(loaded.contains(book2));
        assertTrue(loaded.contains(book3));
    }

    @Test
    public void testTransactionRequiresInMemoryFilterDuringGetRelation() {
        FilterExpression expression =
                new InPredicate(new Path(Book.class, dictionary, "genre"), "Literary Fiction");

        when(scope.getNewPersistentResources()).thenReturn(Sets.newHashSet(mock(PersistentResource.class)));
        when(wrappedTransaction.supportsFiltering(eq(Book.class),
                any())).thenReturn(DataStoreTransaction.FeatureSupport.FULL);
        when(wrappedTransaction.getRelation(eq(inMemoryStoreTransaction), eq(author), eq("books"),
                eq(Optional.empty()), eq(Optional.empty()), eq(Optional.empty()), eq(scope))).thenReturn(books);

        Collection<Object> loaded = (Collection<Object>) inMemoryStoreTransaction.getRelation(
                inMemoryStoreTransaction,
                author,
                "books",
                Optional.of(expression),
                Optional.empty(),
                Optional.empty(),
                scope);

        verify(wrappedTransaction, times(1)).getRelation(
                eq(inMemoryStoreTransaction),
                eq(author),
                eq("books"),
                eq(Optional.empty()),
                eq(Optional.empty()),
                eq(Optional.empty()),
                eq(scope));

        assertEquals(2, loaded.size());
        assertTrue(loaded.contains(book1));
        assertTrue(loaded.contains(book3));
    }

    @Test
    public void testTransactionRequiresInMemoryFilterDuringLoad() {
        FilterExpression expression =
                new InPredicate(new Path(Book.class, dictionary, "genre"), "Literary Fiction");

        when(wrappedTransaction.supportsFiltering(eq(Book.class),
                any())).thenReturn(DataStoreTransaction.FeatureSupport.FULL);
        when(wrappedTransaction.loadObjects(eq(Book.class), eq(Optional.of(expression)),
                eq(Optional.empty()), eq(Optional.empty()), eq(scope))).thenReturn((Set) books);

        Collection<Object> loaded = (Collection<Object>) inMemoryStoreTransaction.loadObjects(
                Book.class,
                Optional.of(expression),
                Optional.empty(),
                Optional.empty(),
                scope);

        verify(wrappedTransaction, times(1)).loadObjects(
                eq(Book.class),
                eq(Optional.of(expression)),
                eq(Optional.empty()),
                eq(Optional.empty()),
                eq(scope));

        assertEquals(3, loaded.size());
        assertTrue(loaded.contains(book1));
        assertTrue(loaded.contains(book2));
        assertTrue(loaded.contains(book3));
    }

    @Test
    public void testDataStoreRequiresTotalInMemoryFilter() {
        FilterExpression expression =
                new InPredicate(new Path(Book.class, dictionary, "genre"), "Literary Fiction");

        when(wrappedTransaction.supportsFiltering(eq(Book.class),
                any())).thenReturn(DataStoreTransaction.FeatureSupport.NONE);
        when(wrappedTransaction.loadObjects(eq(Book.class), eq(Optional.empty()),
                eq(Optional.empty()), eq(Optional.empty()), eq(scope))).thenReturn((Set) books);

        Collection<Object> loaded = (Collection<Object>) inMemoryStoreTransaction.loadObjects(
                Book.class,
                Optional.of(expression),
                Optional.empty(),
                Optional.empty(),
                scope);

        verify(wrappedTransaction, times(1)).loadObjects(
                eq(Book.class),
                eq(Optional.empty()),
                eq(Optional.empty()),
                eq(Optional.empty()),
                eq(scope));

        assertEquals(2, loaded.size());
        assertTrue(loaded.contains(book1));
        assertTrue(loaded.contains(book3));
    }

    @Test
    public void testDataStoreRequiresPartialInMemoryFilter() {
        FilterExpression expression1 =
                new InPredicate(new Path(Book.class, dictionary, "genre"), "Literary Fiction");
        FilterExpression expression2 =
                new InPredicate(new Path(Book.class, dictionary, "editor.firstName"), "Jane");
        FilterExpression expression = new AndFilterExpression(expression1, expression2);

        when(wrappedTransaction.supportsFiltering(eq(Book.class),
                any())).thenReturn(DataStoreTransaction.FeatureSupport.PARTIAL);
        when(wrappedTransaction.loadObjects(eq(Book.class), eq(Optional.of(expression1)),
                eq(Optional.empty()), eq(Optional.empty()), eq(scope))).thenReturn((Set) books);

        Collection<Object> loaded = (Collection<Object>) inMemoryStoreTransaction.loadObjects(
                Book.class,
                Optional.of(expression),
                Optional.empty(),
                Optional.empty(),
                scope);

        verify(wrappedTransaction, times(1)).loadObjects(
                eq(Book.class),
                eq(Optional.of(expression1)),
                eq(Optional.empty()),
                eq(Optional.empty()),
                eq(scope));

        assertEquals(1, loaded.size());
        assertTrue(loaded.contains(book3));
    }

    @Test
    public void testSortingPushDown() {
        Map<String, Sorting.SortOrder> sortOrder = new HashMap<>();
        sortOrder.put("title", Sorting.SortOrder.asc);

        Sorting sorting = new Sorting(sortOrder);

        when(wrappedTransaction.supportsFiltering(eq(Book.class),
                any())).thenReturn(DataStoreTransaction.FeatureSupport.FULL);
        when(wrappedTransaction.supportsSorting(eq(Book.class),
                any())).thenReturn(true);

        when(wrappedTransaction.loadObjects(eq(Book.class), eq(Optional.empty()),
                eq(Optional.of(sorting)), eq(Optional.empty()), eq(scope))).thenReturn((Set) books);

        Collection<Object> loaded = (Collection<Object>) inMemoryStoreTransaction.loadObjects(
                Book.class,
                Optional.empty(),
                Optional.of(sorting),
                Optional.empty(),
                scope);

        verify(wrappedTransaction, times(1)).loadObjects(
                eq(Book.class),
                eq(Optional.empty()),
                eq(Optional.of(sorting)),
                eq(Optional.empty()),
                eq(scope));

        assertEquals(3, loaded.size());
    }

    @Test
    public void testDataStoreRequiresInMemorySorting() {
        Map<String, Sorting.SortOrder> sortOrder = new HashMap<>();
        sortOrder.put("title", Sorting.SortOrder.asc);

        Sorting sorting = new Sorting(sortOrder);

        when(wrappedTransaction.supportsFiltering(eq(Book.class),
                any())).thenReturn(DataStoreTransaction.FeatureSupport.FULL);
        when(wrappedTransaction.supportsSorting(eq(Book.class),
                any())).thenReturn(false);

        when(wrappedTransaction.loadObjects(eq(Book.class), eq(Optional.empty()),
                eq(Optional.empty()), eq(Optional.empty()), eq(scope))).thenReturn((Set) books);

        Collection<Object> loaded = (Collection<Object>) inMemoryStoreTransaction.loadObjects(
                Book.class,
                Optional.empty(),
                Optional.of(sorting),
                Optional.empty(),
                scope);

        verify(wrappedTransaction, times(1)).loadObjects(
                eq(Book.class),
                eq(Optional.empty()),
                eq(Optional.empty()),
                eq(Optional.empty()),
                eq(scope));

        assertEquals(3, loaded.size());

        List<String> bookTitles = loaded.stream().map((o) -> ((Book) o).getTitle()).collect(Collectors.toList());
        assertEquals(bookTitles, Lists.newArrayList("Book 1", "Book 2", "Book 3"));
    }

    @Test
    public void testFilteringRequiresInMemorySorting() {
        FilterExpression expression =
                new InPredicate(new Path(Book.class, dictionary, "genre"), "Literary Fiction");

        Map<String, Sorting.SortOrder> sortOrder = new HashMap<>();
        sortOrder.put("title", Sorting.SortOrder.asc);

        Sorting sorting = new Sorting(sortOrder);

        when(wrappedTransaction.supportsFiltering(eq(Book.class),
                any())).thenReturn(DataStoreTransaction.FeatureSupport.NONE);
        when(wrappedTransaction.supportsSorting(eq(Book.class),
                any())).thenReturn(true);

        when(wrappedTransaction.loadObjects(eq(Book.class), eq(Optional.empty()),
                eq(Optional.empty()), eq(Optional.empty()), eq(scope))).thenReturn((Set) books);

        Collection<Object> loaded = (Collection<Object>) inMemoryStoreTransaction.loadObjects(
                Book.class,
                Optional.of(expression),
                Optional.of(sorting),
                Optional.empty(),
                scope);

        verify(wrappedTransaction, times(1)).loadObjects(
                eq(Book.class),
                eq(Optional.empty()),
                eq(Optional.empty()),
                eq(Optional.empty()),
                eq(scope));

        assertEquals(2, loaded.size());

        List<String> bookTitles = loaded.stream().map((o) -> ((Book) o).getTitle()).collect(Collectors.toList());
        assertEquals(Lists.newArrayList("Book 1", "Book 3"), bookTitles);
    }

    @Test
    public void testPaginationPushDown() {
        Pagination pagination = Pagination.getDefaultPagination(elideSettings);

        when(wrappedTransaction.supportsFiltering(eq(Book.class),
                any())).thenReturn(DataStoreTransaction.FeatureSupport.FULL);
        when(wrappedTransaction.supportsPagination(eq(Book.class))).thenReturn(true);

        when(wrappedTransaction.loadObjects(eq(Book.class), eq(Optional.empty()),
                eq(Optional.empty()), eq(Optional.of(pagination)), eq(scope))).thenReturn((Set) books);

        Collection<Object> loaded = (Collection<Object>) inMemoryStoreTransaction.loadObjects(
                Book.class,
                Optional.empty(),
                Optional.empty(),
                Optional.of(pagination),
                scope);

        verify(wrappedTransaction, times(1)).loadObjects(
                eq(Book.class),
                eq(Optional.empty()),
                eq(Optional.empty()),
                eq(Optional.of(pagination)),
                eq(scope));

        assertEquals(3, loaded.size());
    }

    @Test
    public void testDataStoreRequiresInMemoryPagination() {
        Pagination pagination = Pagination.getDefaultPagination(elideSettings);

        when(wrappedTransaction.supportsFiltering(eq(Book.class),
                any())).thenReturn(DataStoreTransaction.FeatureSupport.FULL);
        when(wrappedTransaction.supportsPagination(eq(Book.class))).thenReturn(false);

        when(wrappedTransaction.loadObjects(eq(Book.class), eq(Optional.empty()),
                eq(Optional.empty()), eq(Optional.empty()), eq(scope))).thenReturn((Set) books);

        Collection<Object> loaded = (Collection<Object>) inMemoryStoreTransaction.loadObjects(
                Book.class,
                Optional.empty(),
                Optional.empty(),
                Optional.of(pagination),
                scope);

        verify(wrappedTransaction, times(1)).loadObjects(
                eq(Book.class),
                eq(Optional.empty()),
                eq(Optional.empty()),
                eq(Optional.empty()),
                eq(scope));

        assertEquals(3, loaded.size());
        assertTrue(loaded.contains(book1));
        assertTrue(loaded.contains(book2));
        assertTrue(loaded.contains(book3));
    }

    @Test
    public void testFilteringRequiresInMemoryPagination() {
        FilterExpression expression =
                new InPredicate(new Path(Book.class, dictionary, "genre"), "Literary Fiction");

        Pagination pagination = Pagination.getDefaultPagination(elideSettings);

        when(wrappedTransaction.supportsFiltering(eq(Book.class),
                any())).thenReturn(DataStoreTransaction.FeatureSupport.NONE);
        when(wrappedTransaction.supportsPagination(eq(Book.class))).thenReturn(true);

        when(wrappedTransaction.loadObjects(eq(Book.class), eq(Optional.empty()),
                eq(Optional.empty()), eq(Optional.empty()), eq(scope))).thenReturn((Set) books);

        Collection<Object> loaded = (Collection<Object>) inMemoryStoreTransaction.loadObjects(
                Book.class,
                Optional.of(expression),
                Optional.empty(),
                Optional.of(pagination),
                scope);

        verify(wrappedTransaction, times(1)).loadObjects(
                eq(Book.class),
                eq(Optional.empty()),
                eq(Optional.empty()),
                eq(Optional.empty()),
                eq(scope));

        assertEquals(2, loaded.size());
        assertTrue(loaded.contains(book1));
        assertTrue(loaded.contains(book3));
    }

    @Test
    public void testSortingRequiresInMemoryPagination() {
        Pagination pagination = Pagination.getDefaultPagination(elideSettings);

        Map<String, Sorting.SortOrder> sortOrder = new HashMap<>();
        sortOrder.put("title", Sorting.SortOrder.asc);

        Sorting sorting = new Sorting(sortOrder);

        when(wrappedTransaction.supportsFiltering(eq(Book.class),
                any())).thenReturn(DataStoreTransaction.FeatureSupport.FULL);
        when(wrappedTransaction.supportsSorting(eq(Book.class),
                any())).thenReturn(false);
        when(wrappedTransaction.supportsPagination(eq(Book.class))).thenReturn(true);

        when(wrappedTransaction.loadObjects(eq(Book.class), eq(Optional.empty()),
                eq(Optional.empty()), eq(Optional.empty()), eq(scope))).thenReturn((Set) books);

        Collection<Object> loaded = (Collection<Object>) inMemoryStoreTransaction.loadObjects(
                Book.class,
                Optional.empty(),
                Optional.of(sorting),
                Optional.of(pagination),
                scope);

        verify(wrappedTransaction, times(1)).loadObjects(
                eq(Book.class),
                eq(Optional.empty()),
                eq(Optional.empty()),
                eq(Optional.empty()),
                eq(scope));

        assertEquals(3, loaded.size());
        assertTrue(loaded.contains(book1));
        assertTrue(loaded.contains(book2));
        assertTrue(loaded.contains(book3));
    }

    @Test
    public void testInMemoryDataStore() {
        HashMapDataStore wrapped = new HashMapDataStore(Book.class.getPackage());
        InMemoryDataStore store = new InMemoryDataStore(wrapped);
        DataStoreTransaction tx = store.beginReadTransaction();
        assertEquals(InMemoryStoreTransaction.class, tx.getClass());

        assertEquals(wrapped, wrapped.getDataStore());

        String tos = store.toString();
        assertTrue(tos.contains("Data store contents"));
        assertTrue(tos.contains("Table class example.NoReadEntity contents"));
        assertTrue(tos.contains("Table class example.Author contents"));
        assertTrue(tos.contains("Table class example.Book contents"));
        assertTrue(tos.contains("Table class example.Child contents"));
        assertTrue(tos.contains("Table class example.ComputedBean contents"));
        assertTrue(tos.contains("Table class example.Editor contents"));
        assertTrue(tos.contains("Table class example.FieldAnnotations contents"));
        assertTrue(tos.contains("Table class example.FirstClassFields contents"));
        assertTrue(tos.contains("Table class example.FunWithPermissions contents"));
        assertTrue(tos.contains("Table class example.Invoice contents"));
        assertTrue(tos.contains("Table class example.Job contents"));
        assertTrue(tos.contains("Table class example.Left contents"));
        assertTrue(tos.contains("Table class example.LineItem contents"));
        assertTrue(tos.contains("Table class example.MapColorShape contents"));
        assertTrue(tos.contains("Table class example.NoDeleteEntity contents"));
        assertTrue(tos.contains("Table class example.NoShareEntity contents"));
        assertTrue(tos.contains("Table class example.NoUpdateEntity contents"));
        assertTrue(tos.contains("Table class example.Parent contents"));
        assertTrue(tos.contains("Table class example.Post contents"));
        assertTrue(tos.contains("Table class example.PrimitiveId contents"));
        assertTrue(tos.contains("Table class example.Publisher contents"));
        assertTrue(tos.contains("Table class example.Right contents"));
        assertTrue(tos.contains("Table class example.StringId contents"));
        assertTrue(tos.contains("Table class example.UpdateAndCreate contents"));
        assertTrue(tos.contains("Table class example.User contents"));
        assertTrue(tos.contains("Table class example.packageshareable.ContainerWithPackageShare contents"));
        assertTrue(tos.contains("Table class example.packageshareable.ShareableWithPackageShare contents"));
        assertTrue(tos.contains("Table class example.packageshareable.UnshareableWithEntityUnshare contents"));
    }
}
