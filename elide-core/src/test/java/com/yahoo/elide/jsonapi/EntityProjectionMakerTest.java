/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.jsonapi;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.TestRequestScope;
import com.yahoo.elide.core.filter.InInsensitivePredicate;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.pagination.PaginationImpl;
import com.yahoo.elide.core.sort.SortingImpl;
import com.yahoo.elide.request.Attribute;
import com.yahoo.elide.request.EntityProjection;
import com.yahoo.elide.request.Sorting;
import example.Address;
import example.Author;
import example.Book;
import example.Editor;
import example.Publisher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.HashMap;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class EntityProjectionMakerTest {
    private EntityDictionary dictionary;

    @BeforeAll
    public void init() {
        dictionary = new EntityDictionary(new HashMap<>());
        dictionary.bindEntity(Book.class);
        dictionary.bindEntity(Author.class);
        dictionary.bindEntity(Publisher.class);
        dictionary.bindEntity(Editor.class);
    }

    @Test
    public void testRootCollectionNoQueryParams() {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        String path = "/book";

        RequestScope scope = new TestRequestScope(dictionary, path, queryParams);

        EntityProjectionMaker maker = new EntityProjectionMaker(dictionary, scope);

        EntityProjection expected = EntityProjection.builder()
                .type(Book.class)
                .attribute(Attribute.builder().name("title").type(String.class).build())
                .attribute(Attribute.builder().name("genre").type(String.class).build())
                .attribute(Attribute.builder().name("language").type(String.class).build())
                .attribute(Attribute.builder().name("publishDate").type(long.class).build())
                .relationship("authors", EntityProjection.builder()
                        .type(Author.class)
                        .build())
                .relationship("publisher", EntityProjection.builder()
                        .type(Publisher.class)
                        .build())
                .relationship("editor", EntityProjection.builder()
                        .type(Editor.class)
                        .build())
                .pagination(PaginationImpl.getDefaultPagination(Book.class))
                .build();

        EntityProjection actual = maker.parsePath(path);

        assertEquals(expected, actual);
    }

    @Test
    public void testRootCollectionSparseFields() {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        queryParams.add("fields[book]", "title,publishDate,authors");
        String path = "/book";

        RequestScope scope = new TestRequestScope(dictionary, path, queryParams);

        EntityProjectionMaker maker = new EntityProjectionMaker(dictionary, scope);

        EntityProjection expected = EntityProjection.builder()
                .type(Book.class)
                .attribute(Attribute.builder().name("title").type(String.class).build())
                .attribute(Attribute.builder().name("publishDate").type(long.class).build())
                .relationship("authors", EntityProjection.builder()
                        .type(Author.class)
                        .build())
                .pagination(PaginationImpl.getDefaultPagination(Book.class))
                .build();

        EntityProjection actual = maker.parsePath(path);

        assertEquals(expected, actual);
    }

    @Test
    public void testRootEntityNoQueryParams() {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        String path = "/book/1";

        RequestScope scope = new TestRequestScope(dictionary, path, queryParams);

        EntityProjectionMaker maker = new EntityProjectionMaker(dictionary, scope);

        EntityProjection expected = EntityProjection.builder()
                .type(Book.class)
                .attribute(Attribute.builder().name("title").type(String.class).build())
                .attribute(Attribute.builder().name("genre").type(String.class).build())
                .attribute(Attribute.builder().name("language").type(String.class).build())
                .attribute(Attribute.builder().name("publishDate").type(long.class).build())
                .relationship("authors", EntityProjection.builder()
                        .type(Author.class)
                        .build())
                .relationship("publisher", EntityProjection.builder()
                        .type(Publisher.class)
                        .build())
                .relationship("editor", EntityProjection.builder()
                        .type(Editor.class)
                        .build())
                .build();

        EntityProjection actual = maker.parsePath(path);

        assertEquals(expected, actual);
    }

    @Test
    public void testNestedCollectionNoQueryParams() {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        String path = "/author/1/books/3/publisher";

        RequestScope scope = new TestRequestScope(dictionary, path, queryParams);

        EntityProjectionMaker maker = new EntityProjectionMaker(dictionary, scope);

        EntityProjection expected = EntityProjection.builder()
                .type(Author.class)
                .relationship("books", EntityProjection.builder()
                        .type(Book.class)
                        .relationship("publisher", EntityProjection.builder()
                                .type(Publisher.class)
                                .attribute(Attribute.builder().name("name").type(String.class).build())
                                .attribute(Attribute.builder().name("updateHookInvoked").type(boolean.class).build())
                                .relationship("books", EntityProjection.builder()
                                        .type(Book.class)
                                        .build())
                                .relationship("editor", EntityProjection.builder()
                                        .type(Editor.class)
                                        .build())
                                .pagination(PaginationImpl.getDefaultPagination(Publisher.class))
                                .build())
                        .build())
                .build();

        EntityProjection actual = maker.parsePath(path);

        assertEquals(expected, actual);
    }

    @Test
    public void testNestedEntityNoQueryParams() {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        String path = "/author/1/books/3/publisher/1";

        RequestScope scope = new TestRequestScope(dictionary, path, queryParams);

        EntityProjectionMaker maker = new EntityProjectionMaker(dictionary, scope);

        EntityProjection expected = EntityProjection.builder()
                .type(Author.class)
                .relationship("books", EntityProjection.builder()
                        .type(Book.class)
                        .relationship("publisher", EntityProjection.builder()
                                .type(Publisher.class)
                                .attribute(Attribute.builder().name("name").type(String.class).build())
                                .attribute(Attribute.builder().name("updateHookInvoked").type(boolean.class).build())
                                .relationship("books", EntityProjection.builder()
                                        .type(Book.class)
                                        .build())
                                .relationship("editor", EntityProjection.builder()
                                        .type(Editor.class)
                                        .build())
                                .build())
                        .build())
                .build();

        EntityProjection actual = maker.parsePath(path);

        assertEquals(expected, actual);
    }

    @Test
    public void testRelationshipNoQueryParams() {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        String path = "/author/1/relationships/books";

        RequestScope scope = new TestRequestScope(dictionary, path, queryParams);

        EntityProjectionMaker maker = new EntityProjectionMaker(dictionary, scope);

        EntityProjection expected = EntityProjection.builder()
                .type(Author.class)
                .relationship("books", EntityProjection.builder()
                        .type(Book.class)
                        .pagination(PaginationImpl.getDefaultPagination(Book.class))
                        .build())
                .build();

        EntityProjection actual = maker.parsePath(path);

        assertEquals(expected, actual);
    }

    @Test
    public void testRelationshipWithSingleInclude() {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        queryParams.add("include", "authors");
        String path = "/book/1/relationships/publisher";

        RequestScope scope = new TestRequestScope(dictionary, path, queryParams);

        EntityProjectionMaker maker = new EntityProjectionMaker(dictionary, scope);

        EntityProjection expected = EntityProjection.builder()
                .type(Book.class)
                .relationship("publisher", EntityProjection.builder()
                        .type(Publisher.class)
                        .pagination(PaginationImpl.getDefaultPagination(Publisher.class))
                        .build())
                .relationship("authors", EntityProjection.builder()
                        .attribute(Attribute.builder().name("name").type(String.class).build())
                        .attribute(Attribute.builder().name("type").type(Author.AuthorType.class).build())
                        .attribute(Attribute.builder().name("homeAddress").type(Address.class).build())
                        .relationship("books", EntityProjection.builder()
                                .type(Book.class)
                                .build())
                        .type(Author.class)
                        .build())
                .relationship("editor", EntityProjection.builder()
                        .type(Editor.class)
                        .build())
                .build();

        EntityProjection actual = maker.parsePath(path);

        assertEquals(expected, actual);
    }

    @Test
    public void testRootCollectionWithSingleInclude() {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        queryParams.add("include", "authors");
        String path = "/book";

        RequestScope scope = new TestRequestScope(dictionary, path, queryParams);

        EntityProjectionMaker maker = new EntityProjectionMaker(dictionary, scope);

        EntityProjection expected = EntityProjection.builder()
                .type(Book.class)
                .attribute(Attribute.builder().name("title").type(String.class).build())
                .attribute(Attribute.builder().name("genre").type(String.class).build())
                .attribute(Attribute.builder().name("language").type(String.class).build())
                .attribute(Attribute.builder().name("publishDate").type(long.class).build())
                .relationship("authors", EntityProjection.builder()
                        .type(Author.class)
                        .attribute(Attribute.builder().name("name").type(String.class).build())
                        .attribute(Attribute.builder().name("type").type(Author.AuthorType.class).build())
                        .attribute(Attribute.builder().name("homeAddress").type(Address.class).build())
                        .relationship("books", EntityProjection.builder()
                                .type(Book.class)
                                .build())
                        .build())
                .relationship("publisher", EntityProjection.builder()
                        .type(Publisher.class)
                        .build())
                .relationship("editor", EntityProjection.builder()
                        .type(Editor.class)
                        .build())
                .pagination(PaginationImpl.getDefaultPagination(Book.class))
                .build();

        EntityProjection actual = maker.parsePath(path);

        assertEquals(expected, actual);
    }

    @Test
    public void testRootEntityWithSingleInclude() {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        queryParams.add("include", "authors");
        String path = "/book/1";

        RequestScope scope = new TestRequestScope(dictionary, path, queryParams);

        EntityProjectionMaker maker = new EntityProjectionMaker(dictionary, scope);

        EntityProjection expected = EntityProjection.builder()
                .type(Book.class)
                .attribute(Attribute.builder().name("title").type(String.class).build())
                .attribute(Attribute.builder().name("genre").type(String.class).build())
                .attribute(Attribute.builder().name("language").type(String.class).build())
                .attribute(Attribute.builder().name("publishDate").type(long.class).build())
                .relationship("authors", EntityProjection.builder()
                        .type(Author.class)
                        .attribute(Attribute.builder().name("name").type(String.class).build())
                        .attribute(Attribute.builder().name("type").type(Author.AuthorType.class).build())
                        .attribute(Attribute.builder().name("homeAddress").type(Address.class).build())
                        .relationship("books", EntityProjection.builder()
                                .type(Book.class)
                                .build())
                        .build())
                .relationship("publisher", EntityProjection.builder()
                        .type(Publisher.class)
                        .build())
                .relationship("editor", EntityProjection.builder()
                        .type(Editor.class)
                        .build())
                .build();

        EntityProjection actual = maker.parsePath(path);

        assertEquals(expected, actual);
    }

    @Test
    public void testRootCollectionWithNestedInclude() throws Exception {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        queryParams.add("include", "books");
        queryParams.add("include", "books.publisher,books.editor");
        String path = "/author";

        RequestScope scope = new TestRequestScope(dictionary, path, queryParams);

        EntityProjectionMaker maker = new EntityProjectionMaker(dictionary, scope);

        EntityProjection expected = EntityProjection.builder()
                .type(Author.class)
                .attribute(Attribute.builder().name("homeAddress").type(Address.class).build())
                .attribute(Attribute.builder().name("name").type(String.class).build())
                .attribute(Attribute.builder().name("type").type(Author.AuthorType.class).build())
                .relationship("books", EntityProjection.builder()
                        .type(Book.class)
                        .attribute(Attribute.builder().name("title").type(String.class).build())
                        .attribute(Attribute.builder().name("genre").type(String.class).build())
                        .attribute(Attribute.builder().name("language").type(String.class).build())
                        .attribute(Attribute.builder().name("publishDate").type(long.class).build())
                        .relationship("editor", EntityProjection.builder()
                                .type(Editor.class)
                                .attribute(Attribute.builder().name("firstName").type(String.class).build())
                                .attribute(Attribute.builder().name("lastName").type(String.class).build())
                                .attribute(Attribute.builder().name("fullName").type(String.class).build())
                                .relationship("editor", EntityProjection.builder()
                                        .type(Editor.class)
                                        .build())
                                .build())
                        .relationship("publisher", EntityProjection.builder()
                                .type(Publisher.class)
                                .attribute(Attribute.builder().name("name").type(String.class).build())
                                .attribute(Attribute.builder().name("updateHookInvoked").type(boolean.class).build())
                                .relationship("books", EntityProjection.builder()
                                        .type(Book.class)
                                        .build())
                                .relationship("editor", EntityProjection.builder()
                                        .type(Editor.class)
                                        .build())
                                .build())
                        .relationship("authors", EntityProjection.builder()
                                .type(Author.class)
                                .build())
                        .build())
                .pagination(PaginationImpl.getDefaultPagination(Author.class))
                .build();

        EntityProjection actual = maker.parsePath(path);

        assertEquals(expected, actual);
    }

    @Test
    public void testRootEntityWithNestedInclude() {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        queryParams.add("include", "books");
        queryParams.add("include", "books.publisher,books.editor");
        String path = "/author/1";

        RequestScope scope = new TestRequestScope(dictionary, path, queryParams);

        EntityProjectionMaker maker = new EntityProjectionMaker(dictionary, scope);

        EntityProjection expected = EntityProjection.builder()
                .type(Author.class)
                .attribute(Attribute.builder().name("name").type(String.class).build())
                .attribute(Attribute.builder().name("type").type(Author.AuthorType.class).build())
                .attribute(Attribute.builder().name("homeAddress").type(Address.class).build())
                .relationship("books", EntityProjection.builder()
                        .type(Book.class)
                        .attribute(Attribute.builder().name("title").type(String.class).build())
                        .attribute(Attribute.builder().name("genre").type(String.class).build())
                        .attribute(Attribute.builder().name("language").type(String.class).build())
                        .attribute(Attribute.builder().name("publishDate").type(long.class).build())
                        .relationship("publisher", EntityProjection.builder()
                                .type(Publisher.class)
                                .attribute(Attribute.builder().name("name").type(String.class).build())
                                .attribute(Attribute.builder().name("updateHookInvoked").type(boolean.class).build())
                                .relationship("books", EntityProjection.builder()
                                        .type(Book.class)
                                        .build())
                                .relationship("editor", EntityProjection.builder()
                                        .type(Editor.class)
                                        .build())
                                .build())
                        .relationship("editor", EntityProjection.builder()
                                .type(Editor.class)
                                .attribute(Attribute.builder().name("firstName").type(String.class).build())
                                .attribute(Attribute.builder().name("lastName").type(String.class).build())
                                .attribute(Attribute.builder().name("fullName").type(String.class).build())
                                .relationship("editor", EntityProjection.builder()
                                        .type(Editor.class)
                                        .build())
                                .build())
                        .relationship("authors", EntityProjection.builder()
                                .type(Author.class)
                                .build())
                        .build())
                .build();

        EntityProjection actual = maker.parsePath(path);

        assertEquals(expected, actual);
    }

    @Test
    public void testNestedEntityWithSingleInclude() {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        queryParams.add("include", "books");
        String path = "/author/1/books/3/publisher/1";

        RequestScope scope = new TestRequestScope(dictionary, path, queryParams);

        EntityProjectionMaker maker = new EntityProjectionMaker(dictionary, scope);

        EntityProjection expected = EntityProjection.builder()
                .type(Author.class)
                .relationship("books", EntityProjection.builder()
                        .type(Book.class)
                        .relationship("publisher", EntityProjection.builder()
                                .type(Publisher.class)
                                .attribute(Attribute.builder().name("name").type(String.class).build())
                                .attribute(Attribute.builder().name("updateHookInvoked").type(boolean.class).build())
                                .relationship("books", EntityProjection.builder()
                                        .type(Book.class)
                                        .attribute(Attribute.builder().name("title").type(String.class).build())
                                        .attribute(Attribute.builder().name("genre").type(String.class).build())
                                        .attribute(Attribute.builder().name("language").type(String.class).build())
                                        .attribute(Attribute.builder().name("publishDate").type(long.class).build())
                                        .relationship("authors", EntityProjection.builder()
                                                .type(Author.class)
                                                .build())
                                        .relationship("publisher", EntityProjection.builder()
                                                .type(Publisher.class)
                                                .build())
                                        .relationship("editor", EntityProjection.builder()
                                                .type(Editor.class)
                                                .build())
                                        .build())
                                .relationship("editor", EntityProjection.builder()
                                        .type(Editor.class)
                                        .build())
                                .build())
                        .build())
                .build();

        EntityProjection actual = maker.parsePath(path);

        assertEquals(expected, actual);
    }

    @Test
    public void testNestedCollectionWithSingleInclude() {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        queryParams.add("include", "books");
        String path = "/author/1/books/3/publisher";

        RequestScope scope = new TestRequestScope(dictionary, path, queryParams);

        EntityProjectionMaker maker = new EntityProjectionMaker(dictionary, scope);

        EntityProjection expected = EntityProjection.builder()
                .type(Author.class)
                .relationship("books", EntityProjection.builder()
                        .type(Book.class)
                        .relationship("publisher", EntityProjection.builder()
                                .type(Publisher.class)
                                .attribute(Attribute.builder().name("name").type(String.class).build())
                                .attribute(Attribute.builder().name("updateHookInvoked").type(boolean.class).build())
                                .relationship("books", EntityProjection.builder()
                                        .attribute(Attribute.builder().name("title").type(String.class).build())
                                        .attribute(Attribute.builder().name("genre").type(String.class).build())
                                        .attribute(Attribute.builder().name("language").type(String.class).build())
                                        .attribute(Attribute.builder().name("publishDate").type(long.class).build())
                                        .relationship("authors", EntityProjection.builder()
                                                .type(Author.class)
                                                .build())
                                        .relationship("publisher", EntityProjection.builder()
                                                .type(Publisher.class)
                                                .build())
                                        .relationship("editor", EntityProjection.builder()
                                                .type(Editor.class)
                                                .build())
                                        .type(Book.class)
                                        .build())
                                .relationship("editor", EntityProjection.builder()
                                        .type(Editor.class)
                                        .build())
                                .pagination(PaginationImpl.getDefaultPagination(Publisher.class))
                                .build())
                        .build())
                .build();

        EntityProjection actual = maker.parsePath(path);

        assertEquals(expected, actual);
    }

    @Test
    public void testRootEntityWithNestedIncludeAndSparseFields() {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        queryParams.add("include", "books");
        queryParams.add("include", "books.publisher,books.editor");
        queryParams.add("fields[publisher]", "name");
        queryParams.add("fields[editor]", "fullName");
        queryParams.add("fields[book]", "publisher,editor,title");
        String path = "/author/1";

        RequestScope scope = new TestRequestScope(dictionary, path, queryParams);

        EntityProjectionMaker maker = new EntityProjectionMaker(dictionary, scope);

        EntityProjection expected = EntityProjection.builder()
                .type(Author.class)
                .attribute(Attribute.builder().name("name").type(String.class).build())
                .attribute(Attribute.builder().name("type").type(Author.AuthorType.class).build())
                .attribute(Attribute.builder().name("homeAddress").type(Address.class).build())
                .relationship("books", EntityProjection.builder()
                        .type(Book.class)
                        .attribute(Attribute.builder().name("title").type(String.class).build())
                        .relationship("publisher", EntityProjection.builder()
                                .type(Publisher.class)
                                .attribute(Attribute.builder().name("name").type(String.class).build())
                                .build())
                        .relationship("editor", EntityProjection.builder()
                                .type(Editor.class)
                                .attribute(Attribute.builder().name("fullName").type(String.class).build())
                                .build())
                        .build())
                .build();

        EntityProjection actual = maker.parsePath(path);

        assertEquals(expected, actual);
    }

    @Test
    public void testRootCollectionWithGlobalFilter() {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        queryParams.add("filter", "genre=='Science Fiction'");
        String path = "/book";

        RequestScope scope = new TestRequestScope(dictionary, path, queryParams);

        FilterExpression expression =
                new InInsensitivePredicate(new Path(Book.class, dictionary, "genre"), "Science Fiction");

        EntityProjectionMaker maker = new EntityProjectionMaker(dictionary, scope);

        EntityProjection expected = EntityProjection.builder()
                .type(Book.class)
                .attribute(Attribute.builder().name("language").type(String.class).build())
                .attribute(Attribute.builder().name("genre").type(String.class).build())
                .attribute(Attribute.builder().name("title").type(String.class).build())
                .attribute(Attribute.builder().name("publishDate").type(long.class).build())
                .filterExpression(expression)
                .relationship("publisher", EntityProjection.builder()
                        .type(Publisher.class)
                        .build())
                .relationship("editor", EntityProjection.builder()
                        .type(Editor.class)
                        .build())
                .relationship("authors", EntityProjection.builder()
                        .type(Author.class)
                        .build())
                .pagination(PaginationImpl.getDefaultPagination(Book.class))
                .build();

        EntityProjection actual = maker.parsePath(path);

        assertEquals(expected, actual);
    }

    @Test
    public void testNestedCollectionWithTypedFilter() {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        queryParams.add("filter[publisher]", "name=='Foo'");
        String path = "/author/1/books/3/publisher";

        FilterExpression expression =
                new InInsensitivePredicate(new Path(Publisher.class, dictionary, "name"), "Foo");

        RequestScope scope = new TestRequestScope(dictionary, path, queryParams);

        EntityProjectionMaker maker = new EntityProjectionMaker(dictionary, scope);

        EntityProjection expected = EntityProjection.builder()
                .type(Author.class)
                .relationship("books", EntityProjection.builder()
                        .type(Book.class)
                        .relationship("publisher", EntityProjection.builder()
                                .type(Publisher.class)
                                .attribute(Attribute.builder().name("name").type(String.class).build())
                                .attribute(Attribute.builder().name("updateHookInvoked").type(boolean.class).build())
                                .filterExpression(expression)
                                .relationship("books", EntityProjection.builder()
                                        .type(Book.class)
                                        .build())
                                .relationship("editor", EntityProjection.builder()
                                        .type(Editor.class)
                                        .build())
                                .pagination(PaginationImpl.getDefaultPagination(Publisher.class))
                                .build())
                        .build())
                .build();

        EntityProjection actual = maker.parsePath(path);

        assertEquals(expected, actual);
    }

    @Test
    public void testRelationshipsAndIncludeWithFilterAndSort() {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        queryParams.add("include", "authors");
        queryParams.add("filter[author]", "name=='Foo'");
        queryParams.add("filter[publisher]", "name=='Foo'");
        queryParams.add("sort", "name");
        String path = "/book/1/relationships/publisher";
        Sorting sorting = SortingImpl.parseSortRule("name", Publisher.class, dictionary);

        RequestScope scope = new TestRequestScope(dictionary, path, queryParams);

        EntityProjectionMaker maker = new EntityProjectionMaker(dictionary, scope);

        EntityProjection expected = EntityProjection.builder()
                .type(Book.class)
                .relationship("publisher", EntityProjection.builder()
                        .type(Publisher.class)
                        .filterExpression(new InInsensitivePredicate(new Path(Publisher.class, dictionary, "name"), "Foo"))
                        .sorting(sorting)
                        .pagination(PaginationImpl.getDefaultPagination(Publisher.class))
                        .build())
                .relationship("authors", EntityProjection.builder()
                        .attribute(Attribute.builder().name("name").type(String.class).build())
                        .attribute(Attribute.builder().name("type").type(Author.AuthorType.class).build())
                        .attribute(Attribute.builder().name("homeAddress").type(Address.class).build())
                        .filterExpression(new InInsensitivePredicate(new Path(Author.class, dictionary, "name"), "Foo"))
                        .relationship("books", EntityProjection.builder()
                                .type(Book.class)
                                .build())
                        .type(Author.class)
                        .build())
                .relationship("editor", EntityProjection.builder()
                        .type(Editor.class)
                        .build())
                .build();

        EntityProjection actual = maker.parsePath(path);

        assertEquals(expected, actual);
    }

    @Test
    public void testRootCollectionWithTypedFilter() {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        queryParams.add("filter[book]", "genre=='Science Fiction'");
        String path = "/book";

        RequestScope scope = new TestRequestScope(dictionary, path, queryParams);

        FilterExpression expression =
                new InInsensitivePredicate(new Path(Book.class, dictionary, "genre"), "Science Fiction");

        EntityProjectionMaker maker = new EntityProjectionMaker(dictionary, scope);

        EntityProjection expected = EntityProjection.builder()
                .type(Book.class)
                .attribute(Attribute.builder().name("title").type(String.class).build())
                .attribute(Attribute.builder().name("genre").type(String.class).build())
                .attribute(Attribute.builder().name("language").type(String.class).build())
                .attribute(Attribute.builder().name("publishDate").type(long.class).build())
                .filterExpression(expression)
                .relationship("authors", EntityProjection.builder()
                        .type(Author.class)
                        .build())
                .relationship("publisher", EntityProjection.builder()
                        .type(Publisher.class)
                        .build())
                .relationship("editor", EntityProjection.builder()
                        .type(Editor.class)
                        .build())
                .pagination(PaginationImpl.getDefaultPagination(Book.class))
                .build();

        EntityProjection actual = maker.parsePath(path);

        assertEquals(expected, actual);
    }
}
