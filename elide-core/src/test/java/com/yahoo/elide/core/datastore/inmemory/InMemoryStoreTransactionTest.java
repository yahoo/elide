/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.datastore.inmemory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.datastore.DataStoreIterable;
import com.yahoo.elide.core.datastore.DataStoreIterableBuilder;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.predicates.InPredicate;
import com.yahoo.elide.core.pagination.PaginationImpl;
import com.yahoo.elide.core.request.EntityProjection;
import com.yahoo.elide.core.request.Relationship;
import com.yahoo.elide.core.request.Sorting;
import com.yahoo.elide.core.sort.SortingImpl;
import com.yahoo.elide.core.type.ClassType;
import com.yahoo.elide.core.utils.DefaultClassScanner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import example.Address;
import example.Author;
import example.Book;
import example.Editor;
import example.Price;
import example.Publisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class InMemoryStoreTransactionTest {

    private DataStoreTransaction wrappedTransaction = mock(DataStoreTransaction.class);
    private RequestScope scope = mock(RequestScope.class);
    private InMemoryStoreTransaction inMemoryStoreTransaction = new InMemoryStoreTransaction(wrappedTransaction);
    private EntityDictionary dictionary;
    private LinkedHashSet books = new LinkedHashSet();
    private Book book1;
    private Book book2;
    private Book book3;
    private Publisher publisher1;
    private Publisher publisher2;
    private Author author1;
    private Author author2;
    private ElideSettings elideSettings;

    public InMemoryStoreTransactionTest() {
        dictionary = EntityDictionary.builder().build();
        dictionary.bindEntity(Book.class);
        dictionary.bindEntity(Author.class);
        dictionary.bindEntity(Editor.class);
        dictionary.bindEntity(Publisher.class);

        elideSettings = new ElideSettingsBuilder(null)
                .withEntityDictionary(EntityDictionary.builder().build())
                .build();

        author1 = new Author();
        Address address1 = new Address();
        address1.setStreet1("Foo");
        author1.setHomeAddress(address1);

        author2 = new Author();
        Address address2 = new Address();
        address2.setStreet1("Bar");
        author2.setHomeAddress(address2);

        Editor editor1 = new Editor();
        editor1.setFirstName("Jon");
        editor1.setLastName("Doe");

        Editor editor2 = new Editor();
        editor2.setFirstName("Jane");
        editor2.setLastName("Doe");

        publisher1 = new Publisher();
        publisher1.setEditor(editor1);

        publisher2 = new Publisher();
        publisher2.setEditor(editor2);

        book1 = new Book(1,
                "Book 1",
                "Literary Fiction",
                "English",
                System.currentTimeMillis(),
                Sets.newHashSet(author1),
                publisher1,
                Arrays.asList("Prize1"),
                new Price());

        book2 = new Book(2,
                "Book 2",
                "Science Fiction",
                "English",
                System.currentTimeMillis(),
                Sets.newHashSet(author1),
                publisher1,
                Arrays.asList("Prize1", "Prize2"),
                new Price());

        book3 = new Book(3,
                "Book 3",
                "Literary Fiction",
                "English",
                System.currentTimeMillis(),
                Sets.newHashSet(author1),
                publisher2,
                Arrays.asList(),
                new Price());

        books.add(book1);
        books.add(book2);
        books.add(book3);

        author1.setBooks(new ArrayList<>(books));

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

        EntityProjection projection = EntityProjection.builder()
                .type(Book.class)
                .filterExpression(expression)
                .build();

        DataStoreIterable expected = new DataStoreIterableBuilder<>(books).build();

        when(wrappedTransaction.loadObjects(eq(projection), eq(scope)))
                .thenReturn(expected);

        DataStoreIterable actual = inMemoryStoreTransaction.loadObjects(projection, scope);
        assertEquals(expected, actual);

        verify(wrappedTransaction, times(1)).loadObjects(eq(projection), eq(scope));
    }

    @Test
    public void testFilterPredicateInMemoryOnComplexAttribute() {
        FilterExpression expression =
                new InPredicate(new Path(Author.class, dictionary, "homeAddress.street1"), "Foo");

        EntityProjection projection = EntityProjection.builder()
                .type(Author.class)
                .filterExpression(expression)
                .build();

        DataStoreIterable filterInMemory =
                new DataStoreIterableBuilder(Arrays.asList(author1, author2)).filterInMemory(true).build();

        when(wrappedTransaction.loadObjects(any(), eq(scope))).thenReturn(filterInMemory);

        Collection<Object> loaded = ImmutableList.copyOf(inMemoryStoreTransaction.loadObjects(projection, scope));

        assertEquals(1, loaded.size());
        assertTrue(loaded.contains(author1));
    }

    @Test
    public void testSortOnComputedAttribute() {
        Map<String, Sorting.SortOrder> sortOrder = new HashMap<>();
        sortOrder.put("fullName", Sorting.SortOrder.desc);

        Editor editor1 = new Editor();
        editor1.setFirstName("A");
        editor1.setLastName("X");
        Editor editor2 = new Editor();
        editor2.setFirstName("B");
        editor2.setLastName("Y");

        Sorting sorting = new SortingImpl(sortOrder, Editor.class, dictionary);

        EntityProjection projection = EntityProjection.builder()
                .type(Editor.class)
                .sorting(sorting)
                .build();

        DataStoreIterable iterable =
                new DataStoreIterableBuilder(Arrays.asList(editor1, editor2)).sortInMemory(false).build();

        ArgumentCaptor<EntityProjection> projectionArgument = ArgumentCaptor.forClass(EntityProjection.class);
        when(wrappedTransaction.loadObjects(projectionArgument.capture(), eq(scope))).thenReturn(iterable);

        Collection<Object> loaded = Lists.newArrayList(inMemoryStoreTransaction.loadObjects(projection, scope));

        assertNull(projectionArgument.getValue().getSorting());
        assertEquals(2, loaded.size());

        Object[] sorted = loaded.toArray();
        assertEquals(editor2, sorted[0]);
        assertEquals(editor1, sorted[1]);
    }

    @Test
    public void testSortOnComplexAttribute() {
        Map<String, Sorting.SortOrder> sortOrder = new HashMap<>();
        sortOrder.put("homeAddress.street1", Sorting.SortOrder.asc);

        Sorting sorting = new SortingImpl(sortOrder, Author.class, dictionary);

        EntityProjection projection = EntityProjection.builder()
                .type(Author.class)
                .sorting(sorting)
                .build();

        DataStoreIterable sortInMemory =
                new DataStoreIterableBuilder(Arrays.asList(author1, author2)).sortInMemory(true).build();

        when(wrappedTransaction.loadObjects(any(), eq(scope))).thenReturn(sortInMemory);

        Collection<Object> loaded = Lists.newArrayList(inMemoryStoreTransaction.loadObjects(projection, scope));

        assertEquals(2, loaded.size());

        Object[] sorted = loaded.toArray();
        assertEquals(author2, sorted[0]);
        assertEquals(author1, sorted[1]);
    }

    @Test
    public void testTransactionRequiresInMemoryFilterDuringGetRelation() {
        FilterExpression expression =
                new InPredicate(new Path(Book.class, dictionary, "genre"), "Literary Fiction");

        Relationship relationship = Relationship.builder()
                .projection(EntityProjection.builder()
                    .type(Book.class)
                    .filterExpression(expression)
                    .build())
                .name("books")
                .alias("books")
                .build();

        ArgumentCaptor<Relationship> relationshipArgument = ArgumentCaptor.forClass(Relationship.class);

        when(scope.getNewPersistentResources()).thenReturn(Sets.newHashSet(mock(PersistentResource.class)));

        when(wrappedTransaction.getToManyRelation(eq(inMemoryStoreTransaction), eq(author1), any(), eq(scope)))
                .thenReturn(new DataStoreIterableBuilder<>(books).build());

        Collection<Object> loaded = ImmutableList.copyOf((Iterable) inMemoryStoreTransaction.getToManyRelation(
                inMemoryStoreTransaction, author1, relationship, scope));

        verify(wrappedTransaction, times(1)).getToManyRelation(
                eq(inMemoryStoreTransaction),
                eq(author1),
                relationshipArgument.capture(),
                eq(scope));

        assertNull(relationshipArgument.getValue().getProjection().getFilterExpression());
        assertNull(relationshipArgument.getValue().getProjection().getSorting());
        assertNull(relationshipArgument.getValue().getProjection().getPagination());

        assertEquals(2, loaded.size());
        assertTrue(loaded.contains(book1));
        assertTrue(loaded.contains(book3));
    }

    @Test
    public void testGetToOneRelationship() {

        Relationship relationship = Relationship.builder()
                .projection(EntityProjection.builder()
                        .type(Book.class)
                        .build())
                .name("publisher")
                .alias("publisher")
                .build();

        when(wrappedTransaction.getToOneRelation(eq(inMemoryStoreTransaction), eq(book1), any(), eq(scope)))
                .thenReturn(publisher1);

        Publisher loaded = inMemoryStoreTransaction.getToOneRelation(
                inMemoryStoreTransaction, book1, relationship, scope);

        verify(wrappedTransaction, times(1)).getToOneRelation(
                eq(inMemoryStoreTransaction),
                eq(book1),
                eq(relationship),
                eq(scope));

        assertEquals(publisher1, loaded);
    }

    @Test
    public void testDataStoreRequiresTotalInMemoryFilter() {
        FilterExpression expression =
                new InPredicate(new Path(Book.class, dictionary, "genre"), "Literary Fiction");

        EntityProjection projection = EntityProjection.builder()
                .type(Book.class)
                .filterExpression(expression)
                .build();

        DataStoreIterable filterInMemory = new DataStoreIterableBuilder(books).filterInMemory(true).build();

        when(wrappedTransaction.loadObjects(any(), eq(scope))).thenReturn(filterInMemory);

        Collection<Object> loaded = ImmutableList.copyOf(inMemoryStoreTransaction.loadObjects(projection, scope));

        verify(wrappedTransaction, times(1)).loadObjects(
                any(EntityProjection.class),
                eq(scope));

        assertEquals(2, loaded.size());
        assertTrue(loaded.contains(book1));
        assertTrue(loaded.contains(book3));
    }


    @Test
    public void testSortingPushDown() {
        Map<String, Sorting.SortOrder> sortOrder = new HashMap<>();
        sortOrder.put("title", Sorting.SortOrder.asc);

        Sorting sorting = new SortingImpl(sortOrder, Book.class, dictionary);

        EntityProjection projection = EntityProjection.builder()
                .type(Book.class)
                .sorting(sorting)
                .build();

        DataStoreIterable expected = new DataStoreIterableBuilder<>(books).build();
        when(wrappedTransaction.loadObjects(any(), eq(scope)))
                .thenReturn(expected);

        DataStoreIterable actual = inMemoryStoreTransaction.loadObjects(projection, scope);

        verify(wrappedTransaction, times(1)).loadObjects(
                eq(projection),
                eq(scope));

        assertEquals(expected, actual);
    }

    @Test
    public void testDataStoreRequiresInMemorySorting() {
        Map<String, Sorting.SortOrder> sortOrder = new HashMap<>();
        sortOrder.put("title", Sorting.SortOrder.desc);

        Sorting sorting = new SortingImpl(sortOrder, Book.class, dictionary);

        EntityProjection projection = EntityProjection.builder()
                .type(Book.class)
                .sorting(sorting)
                .build();

        DataStoreIterable sortInMemory = new DataStoreIterableBuilder(books).sortInMemory(true).build();

        when(wrappedTransaction.loadObjects(any(), eq(scope))).thenReturn(sortInMemory);

        Collection<Object> loaded = Lists.newArrayList(inMemoryStoreTransaction.loadObjects(
                projection,
                scope));

        verify(wrappedTransaction, times(1)).loadObjects(
                any(EntityProjection.class),
                eq(scope));

        assertEquals(3, loaded.size());

        List<String> bookTitles = loaded.stream().map((o) -> ((Book) o).getTitle()).collect(Collectors.toList());
        assertEquals(bookTitles, Lists.newArrayList("Book 3", "Book 2", "Book 1"));
    }

    @Test
    public void testFilteringRequiresInMemorySorting() {
        FilterExpression expression =
                new InPredicate(new Path(Book.class, dictionary, "genre"), "Literary Fiction");

        Map<String, Sorting.SortOrder> sortOrder = new HashMap<>();
        sortOrder.put("title", Sorting.SortOrder.desc);

        Sorting sorting = new SortingImpl(sortOrder, Book.class, dictionary);

        EntityProjection projection = EntityProjection.builder()
                .type(Book.class)
                .filterExpression(expression)
                .sorting(sorting)
                .build();

        DataStoreIterable filterInMemory = new DataStoreIterableBuilder(books).filterInMemory(true).build();

        when(wrappedTransaction.loadObjects(any(), eq(scope))).thenReturn(filterInMemory);

        Collection<Object> loaded = Lists.newArrayList(inMemoryStoreTransaction.loadObjects(
                projection,
                scope));

        verify(wrappedTransaction, times(1)).loadObjects(
                any(EntityProjection.class),
                eq(scope));

        assertEquals(2, loaded.size());

        List<String> bookTitles = loaded.stream().map((o) -> ((Book) o).getTitle()).collect(Collectors.toList());
        assertEquals(Lists.newArrayList("Book 3", "Book 1"), bookTitles);
    }

    @Test
    public void testPaginationPushDown() {
        PaginationImpl pagination = new PaginationImpl(ClassType.of(Book.class), 0, 1, 10, 10, false, false);

        EntityProjection projection = EntityProjection.builder()
                .type(Book.class)
                .pagination(pagination)
                .build();

        ArgumentCaptor<EntityProjection> projectionArgument = ArgumentCaptor.forClass(EntityProjection.class);

        when(wrappedTransaction.loadObjects(any(), eq(scope)))
                .thenReturn(new DataStoreIterableBuilder<>(books).build());

        Collection<Object> loaded = Lists.newArrayList(inMemoryStoreTransaction.loadObjects(
                projection,
                scope));

        verify(wrappedTransaction, times(1)).loadObjects(
                projectionArgument.capture(),
                eq(scope));

        assertEquals(pagination, projectionArgument.getValue().getPagination());
        assertEquals(3, loaded.size());
    }

    @Test
    public void testDataStoreRequiresInMemoryPagination() {
        PaginationImpl pagination = new PaginationImpl(ClassType.of(Book.class), 0, 2, 10, 10, false, false);

        EntityProjection projection = EntityProjection.builder()
                .type(Book.class)
                .pagination(pagination)
                .build();

        DataStoreIterable paginateInMemory = new DataStoreIterableBuilder(books).paginateInMemory(true).build();

        when(wrappedTransaction.loadObjects(any(), eq(scope))).thenReturn(paginateInMemory);

        Collection<Object> loaded = Lists.newArrayList(inMemoryStoreTransaction.loadObjects(
                projection,
                scope));

        verify(wrappedTransaction, times(1)).loadObjects(
                any(EntityProjection.class),
                eq(scope));

        assertEquals(2, loaded.size());
        assertTrue(loaded.contains(book1));
        assertTrue(loaded.contains(book2));
    }

    @Test
    public void testFilteringRequiresInMemoryPagination() {
        FilterExpression expression =
                new InPredicate(new Path(Book.class, dictionary, "genre"), "Literary Fiction");

        PaginationImpl pagination = new PaginationImpl(ClassType.of(Book.class), 0, 2, 10, 10, true, false);

        EntityProjection projection = EntityProjection.builder()
                .type(Book.class)
                .filterExpression(expression)
                .pagination(pagination)
                .build();

        DataStoreIterable filterInMemory = new DataStoreIterableBuilder(books).filterInMemory(true).build();

        when(wrappedTransaction.loadObjects(any(), eq(scope))).thenReturn(filterInMemory);

        Collection<Object> loaded = Lists.newArrayList(inMemoryStoreTransaction.loadObjects(
                projection,
                scope));

        verify(wrappedTransaction, times(1)).loadObjects(
                any(EntityProjection.class),
                eq(scope));

        assertEquals(2, loaded.size());
        assertTrue(loaded.contains(book1));
        assertTrue(loaded.contains(book3));
        assertEquals(2, pagination.getPageTotals());
    }

    @Test
    public void testSortingRequiresInMemoryPagination() {
        PaginationImpl pagination = new PaginationImpl(ClassType.of(Book.class), 0, 3, 10, 10, true, false);

        Map<String, Sorting.SortOrder> sortOrder = new HashMap<>();
        sortOrder.put("title", Sorting.SortOrder.desc);

        Sorting sorting = new SortingImpl(sortOrder, Book.class, dictionary);

        EntityProjection projection = EntityProjection.builder()
                .type(Book.class)
                .sorting(sorting)
                .pagination(pagination)
                .build();

        DataStoreIterable sortInMemory = new DataStoreIterableBuilder(books).sortInMemory(true).build();

        when(wrappedTransaction.loadObjects(any(), eq(scope))).thenReturn(sortInMemory);

        Collection<Object> loaded = Lists.newArrayList(inMemoryStoreTransaction.loadObjects(
                projection,
                scope));

        verify(wrappedTransaction, times(1)).loadObjects(
                any(EntityProjection.class),
                eq(scope));

        assertEquals(3, loaded.size());
        List<String> bookTitles = loaded.stream().map((o) -> ((Book) o).getTitle()).collect(Collectors.toList());
        assertEquals(Lists.newArrayList("Book 3", "Book 2", "Book 1"), bookTitles);
        assertEquals(3, pagination.getPageTotals());
    }

    @Test
    public void testGetProperty() {
        when(wrappedTransaction.getProperty(any())).thenReturn(1);

        Integer result = inMemoryStoreTransaction.getProperty("foo");

        verify(wrappedTransaction, times(1)).getProperty(eq("foo"));

        assertEquals(1, result);
    }

    @Test
    public void testInMemoryDataStore() {
        HashMapDataStore wrapped = new HashMapDataStore(DefaultClassScanner.getInstance(), Book.class.getPackage());
        InMemoryDataStore store = new InMemoryDataStore(wrapped);
        DataStoreTransaction tx = store.beginReadTransaction();
        assertEquals(InMemoryStoreTransaction.class, tx.getClass());

        assertEquals(wrapped, wrapped.getDataStore());

        // extract class names from DataStore string
        String tos = store.toString()
                .replace("Data store contents", "")
                .replace("Table ClassType{cls=class", "").replace("} contents", "")
                .replace("Wrapped:[", "").replace("]", "")
                .replace("\n\n", ",")
                .replace(" ", "").replace("\n", "");

        // make sure count is correct
        assertEquals(ImmutableSet.copyOf(new String[] {
            "example.Author",
            "example.Book",
            "example.Child",
            "example.CoerceBean",
            "example.ComputedBean",
            "example.Editor",
            "example.FieldAnnotations",
            "example.FirstClassFields",
            "example.FunWithPermissions",
            "example.Invoice",
            "example.Job",
            "example.Left",
            "example.LineItem",
            "example.MapColorShape",
            "example.NoDeleteEntity",
            "example.NoReadEntity",
            "example.NoShareEntity",
            "example.NoUpdateEntity",
            "example.Parent",
            "example.Post",
            "example.PrimitiveId",
            "example.Publisher",
            "example.Right",
            "example.StringId",
            "example.UpdateAndCreate",
            "example.User",
            "example.Company",
            "example.models.generics.Employee",
            "example.models.generics.Manager",
            "example.models.generics.Overlord",
            "example.models.generics.Peon",
            "example.models.packageinfo.IncludedPackageLevel",
            "example.models.packageinfo.included.IncludedSubPackage",
            "example.models.triggers.Invoice",
            "example.models.versioned.BookV2",
            "example.nontransferable.ContainerWithPackageShare",
            "example.nontransferable.NoTransferBiDirectional",
            "example.nontransferable.ShareableWithPackageShare",
            "example.nontransferable.StrictNoTransfer",
            "example.nontransferable.Untransferable"
        }), ImmutableSet.copyOf(tos.split(",")));
    }
}
