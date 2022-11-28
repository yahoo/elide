/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.jsonapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.TestRequestScope;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.exceptions.InvalidValueException;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.predicates.InPredicate;
import com.yahoo.elide.core.pagination.PaginationImpl;
import com.yahoo.elide.core.request.Attribute;
import com.yahoo.elide.core.request.EntityProjection;
import com.yahoo.elide.core.request.Relationship;
import com.yahoo.elide.core.request.Sorting;
import com.yahoo.elide.core.sort.SortingImpl;
import com.yahoo.elide.core.type.ClassType;
import com.google.common.collect.Sets;
import example.Address;
import example.Author;
import example.Book;
import example.Editor;
import example.Price;
import example.Publisher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class EntityProjectionMakerTest {
    private EntityDictionary dictionary;

    @BeforeAll
    public void init() {
        dictionary = EntityDictionary.builder().build();
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
                .attribute(Attribute.builder().name("awards").type(Collection.class).build())
                .attribute(Attribute.builder().name("genre").type(String.class).build())
                .attribute(Attribute.builder().name("language").type(String.class).build())
                .attribute(Attribute.builder().name("publishDate").type(long.class).build())
                .attribute(Attribute.builder().name("authorTypes").type(Collection.class).build())
                .attribute(Attribute.builder().name("price").type(Price.class).build())
                .relationship("authors", EntityProjection.builder()
                        .type(Author.class)
                        .build())
                .relationship("publisher", EntityProjection.builder()
                        .type(Publisher.class)
                        .build())
                .relationship("editor", EntityProjection.builder()
                        .type(Editor.class)
                        .build())
                .pagination(PaginationImpl.getDefaultPagination(ClassType.of(Book.class)))
                .build();

        EntityProjection actual = maker.parsePath(path);

        projectionEquals(expected, actual);
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
                .pagination(PaginationImpl.getDefaultPagination(ClassType.of(Book.class)))
                .build();

        EntityProjection actual = maker.parsePath(path);

        projectionEquals(expected, actual);
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
                .attribute(Attribute.builder().name("awards").type(Collection.class).build())
                .attribute(Attribute.builder().name("genre").type(String.class).build())
                .attribute(Attribute.builder().name("language").type(String.class).build())
                .attribute(Attribute.builder().name("publishDate").type(long.class).build())
                .attribute(Attribute.builder().name("authorTypes").type(Collection.class).build())
                .attribute(Attribute.builder().name("price").type(Price.class).build())
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

        projectionEquals(expected, actual);
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
                                .pagination(PaginationImpl.getDefaultPagination(ClassType.of(Publisher.class)))
                                .build())
                        .build())
                .build();

        EntityProjection actual = maker.parsePath(path);

        projectionEquals(expected, actual);
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

        projectionEquals(expected, actual);
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
                        .pagination(PaginationImpl.getDefaultPagination(ClassType.of(Book.class)))
                        .build())
                .relationship("products", EntityProjection.builder()
                        .type(Book.class)
                        .build())
                .build();

        EntityProjection actual = maker.parsePath(path);

        projectionEquals(expected, actual);
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
                        .pagination(PaginationImpl.getDefaultPagination(ClassType.of(Publisher.class)))
                        .build())
                .relationship("authors", EntityProjection.builder()
                        .attribute(Attribute.builder().name("name").type(String.class).build())
                        .attribute(Attribute.builder().name("type").type(Author.AuthorType.class).build())
                        .attribute(Attribute.builder().name("homeAddress").type(Address.class).build())
                        .attribute(Attribute.builder().name("vacationHomes").type(Set.class).build())
                        .attribute(Attribute.builder().name("stuff").type(Map.class).build())
                        .attribute(Attribute.builder().name("awards").type(Collection.class).build())
                        .relationship("products", EntityProjection.builder()
                                .type(Book.class)
                                .build())
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

        projectionEquals(expected, actual);
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
                .attribute(Attribute.builder().name("awards").type(Collection.class).build())
                .attribute(Attribute.builder().name("genre").type(String.class).build())
                .attribute(Attribute.builder().name("language").type(String.class).build())
                .attribute(Attribute.builder().name("publishDate").type(long.class).build())
                .attribute(Attribute.builder().name("authorTypes").type(Collection.class).build())
                .attribute(Attribute.builder().name("price").type(Price.class).build())
                .relationship("authors", EntityProjection.builder()
                        .type(Author.class)
                        .attribute(Attribute.builder().name("name").type(String.class).build())
                        .attribute(Attribute.builder().name("type").type(Author.AuthorType.class).build())
                        .attribute(Attribute.builder().name("homeAddress").type(Address.class).build())
                        .attribute(Attribute.builder().name("vacationHomes").type(Set.class).build())
                        .attribute(Attribute.builder().name("stuff").type(Map.class).build())
                        .attribute(Attribute.builder().name("awards").type(Collection.class).build())
                        .relationship("books", EntityProjection.builder()
                                .type(Book.class)
                                .build())
                        .relationship("products", EntityProjection.builder()
                                .type(Book.class)
                                .build())
                        .build())
                .relationship("publisher", EntityProjection.builder()
                        .type(Publisher.class)
                        .build())
                .relationship("editor", EntityProjection.builder()
                        .type(Editor.class)
                        .build())
                .pagination(PaginationImpl.getDefaultPagination(ClassType.of(Book.class)))
                .build();

        EntityProjection actual = maker.parsePath(path);

        projectionEquals(expected, actual);
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
                .attribute(Attribute.builder().name("awards").type(Collection.class).build())
                .attribute(Attribute.builder().name("genre").type(String.class).build())
                .attribute(Attribute.builder().name("language").type(String.class).build())
                .attribute(Attribute.builder().name("publishDate").type(long.class).build())
                .attribute(Attribute.builder().name("authorTypes").type(Collection.class).build())
                .attribute(Attribute.builder().name("price").type(Price.class).build())
                .relationship("authors", EntityProjection.builder()
                        .type(Author.class)
                        .attribute(Attribute.builder().name("name").type(String.class).build())
                        .attribute(Attribute.builder().name("type").type(Author.AuthorType.class).build())
                        .attribute(Attribute.builder().name("homeAddress").type(Address.class).build())
                        .attribute(Attribute.builder().name("vacationHomes").type(Set.class).build())
                        .attribute(Attribute.builder().name("stuff").type(Map.class).build())
                        .attribute(Attribute.builder().name("awards").type(Collection.class).build())
                        .relationship("books", EntityProjection.builder()
                                .type(Book.class)
                                .build())
                        .relationship("products", EntityProjection.builder()
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

        projectionEquals(expected, actual);
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
                .attribute(Attribute.builder().name("vacationHomes").type(Set.class).build())
                .attribute(Attribute.builder().name("stuff").type(Map.class).build())
                .attribute(Attribute.builder().name("name").type(String.class).build())
                .attribute(Attribute.builder().name("type").type(Author.AuthorType.class).build())
                .attribute(Attribute.builder().name("awards").type(Collection.class).build())
                .relationship("products", EntityProjection.builder()
                        .type(Book.class)
                        .build())
                .relationship("books", EntityProjection.builder()
                        .type(Book.class)
                        .attribute(Attribute.builder().name("title").type(String.class).build())
                        .attribute(Attribute.builder().name("awards").type(Collection.class).build())
                        .attribute(Attribute.builder().name("genre").type(String.class).build())
                        .attribute(Attribute.builder().name("language").type(String.class).build())
                        .attribute(Attribute.builder().name("publishDate").type(long.class).build())
                        .attribute(Attribute.builder().name("authorTypes").type(Collection.class).build())
                        .attribute(Attribute.builder().name("price").type(Price.class).build())
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
                .pagination(PaginationImpl.getDefaultPagination(ClassType.of(Author.class)))
                .build();

        EntityProjection actual = maker.parsePath(path);

        projectionEquals(expected, actual);
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
                .attribute(Attribute.builder().name("vacationHomes").type(Set.class).build())
                .attribute(Attribute.builder().name("stuff").type(Map.class).build())
                .attribute(Attribute.builder().name("awards").type(Collection.class).build())
                .relationship("products", EntityProjection.builder()
                        .type(Book.class)
                        .build())
                .relationship("books", EntityProjection.builder()
                        .type(Book.class)
                        .attribute(Attribute.builder().name("title").type(String.class).build())
                        .attribute(Attribute.builder().name("awards").type(Collection.class).build())
                        .attribute(Attribute.builder().name("genre").type(String.class).build())
                        .attribute(Attribute.builder().name("language").type(String.class).build())
                        .attribute(Attribute.builder().name("publishDate").type(long.class).build())
                        .attribute(Attribute.builder().name("authorTypes").type(Collection.class).build())
                        .attribute(Attribute.builder().name("price").type(Price.class).build())
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

        projectionEquals(expected, actual);
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
                                        .attribute(Attribute.builder().name("awards").type(Collection.class).build())
                                        .attribute(Attribute.builder().name("genre").type(String.class).build())
                                        .attribute(Attribute.builder().name("language").type(String.class).build())
                                        .attribute(Attribute.builder().name("publishDate").type(long.class).build())
                                        .attribute(Attribute.builder().name("authorTypes").type(Collection.class).build())
                                        .attribute(Attribute.builder().name("price").type(Price.class).build())
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

        projectionEquals(expected, actual);
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
                                        .attribute(Attribute.builder().name("awards").type(Collection.class).build())
                                        .attribute(Attribute.builder().name("genre").type(String.class).build())
                                        .attribute(Attribute.builder().name("language").type(String.class).build())
                                        .attribute(Attribute.builder().name("publishDate").type(long.class).build())
                                        .attribute(Attribute.builder().name("authorTypes").type(Collection.class).build())
                                        .attribute(Attribute.builder().name("price").type(Price.class).build())
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
                                .pagination(PaginationImpl.getDefaultPagination(ClassType.of(Publisher.class)))
                                .build())
                        .build())
                .build();

        EntityProjection actual = maker.parsePath(path);

        projectionEquals(expected, actual);
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
                .attribute(Attribute.builder().name("vacationHomes").type(Set.class).build())
                .attribute(Attribute.builder().name("stuff").type(Map.class).build())
                .attribute(Attribute.builder().name("awards").type(Collection.class).build())
                .relationship("products", EntityProjection.builder()
                        .type(Book.class)
                        .build())
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

        projectionEquals(expected, actual);
    }

    @Test
    public void testRootCollectionWithGlobalFilter() {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        queryParams.add("filter", "genre=='Science Fiction'");
        String path = "/book";

        RequestScope scope = new TestRequestScope(dictionary, path, queryParams);

        FilterExpression expression =
                new InPredicate(new Path(Book.class, dictionary, "genre"), "Science Fiction");

        EntityProjectionMaker maker = new EntityProjectionMaker(dictionary, scope);

        EntityProjection expected = EntityProjection.builder()
                .type(Book.class)
                .attribute(Attribute.builder().name("language").type(String.class).build())
                .attribute(Attribute.builder().name("genre").type(String.class).build())
                .attribute(Attribute.builder().name("title").type(String.class).build())
                .attribute(Attribute.builder().name("awards").type(Collection.class).build())
                .attribute(Attribute.builder().name("publishDate").type(long.class).build())
                .attribute(Attribute.builder().name("authorTypes").type(Collection.class).build())
                .attribute(Attribute.builder().name("price").type(Price.class).build())
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
                .pagination(PaginationImpl.getDefaultPagination(ClassType.of(Book.class)))
                .build();

        EntityProjection actual = maker.parsePath(path);

        projectionEquals(expected, actual);
    }

    @Test
    public void testNestedCollectionWithTypedFilter() {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        queryParams.add("filter[publisher]", "name=='Foo'");
        String path = "/author/1/books/3/publisher";

        FilterExpression expression =
                new InPredicate(new Path(Publisher.class, dictionary, "name"), "Foo");

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
                                .pagination(PaginationImpl.getDefaultPagination(ClassType.of(Publisher.class)))
                                .build())
                        .build())
                .build();

        EntityProjection actual = maker.parsePath(path);

        projectionEquals(expected, actual);
    }

    @Test
    public void testRelationshipsAndIncludeWithFilterAndSort() {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        queryParams.add("include", "authors");
        queryParams.add("filter[author]", "name=='Foo'");
        queryParams.add("filter[publisher]", "name=='Foo'");
        queryParams.add("sort", "name");
        String path = "/book/1/relationships/publisher";
        Sorting sorting = SortingImpl.parseSortRule("name", ClassType.of(Publisher.class), dictionary);

        RequestScope scope = new TestRequestScope(dictionary, path, queryParams);

        EntityProjectionMaker maker = new EntityProjectionMaker(dictionary, scope);

        EntityProjection expected = EntityProjection.builder()
                .type(Book.class)
                .relationship("publisher", EntityProjection.builder()
                        .type(Publisher.class)
                        .filterExpression(new InPredicate(new Path(Publisher.class, dictionary, "name"), "Foo"))
                        .sorting(sorting)
                        .pagination(PaginationImpl.getDefaultPagination(ClassType.of(Publisher.class)))
                        .build())
                .relationship("authors", EntityProjection.builder()
                        .attribute(Attribute.builder().name("name").type(String.class).build())
                        .attribute(Attribute.builder().name("type").type(Author.AuthorType.class).build())
                        .attribute(Attribute.builder().name("homeAddress").type(Address.class).build())
                        .attribute(Attribute.builder().name("vacationHomes").type(Set.class).build())
                        .attribute(Attribute.builder().name("stuff").type(Map.class).build())
                        .attribute(Attribute.builder().name("awards").type(Collection.class).build())
                        .filterExpression(new InPredicate(new Path(Author.class, dictionary, "name"), "Foo"))
                        .relationship("books", EntityProjection.builder()
                                .type(Book.class)
                                .build())
                        .relationship("products", EntityProjection.builder()
                                .type(Book.class)
                                .build())
                        .type(Author.class)
                        .build())
                .relationship("editor", EntityProjection.builder()
                        .type(Editor.class)
                        .build())
                .build();

        EntityProjection actual = maker.parsePath(path);

        projectionEquals(expected, actual);
    }

    @Test
    public void testRootCollectionWithTypedFilter() {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        queryParams.add("filter[book]", "genre=='Science Fiction'");
        String path = "/book";

        RequestScope scope = new TestRequestScope(dictionary, path, queryParams);

        FilterExpression expression =
                new InPredicate(new Path(Book.class, dictionary, "genre"), "Science Fiction");

        EntityProjectionMaker maker = new EntityProjectionMaker(dictionary, scope);

        EntityProjection expected = EntityProjection.builder()
                .type(Book.class)
                .attribute(Attribute.builder().name("title").type(String.class).build())
                .attribute(Attribute.builder().name("awards").type(Collection.class).build())
                .attribute(Attribute.builder().name("genre").type(String.class).build())
                .attribute(Attribute.builder().name("language").type(String.class).build())
                .attribute(Attribute.builder().name("publishDate").type(long.class).build())
                .attribute(Attribute.builder().name("authorTypes").type(Collection.class).build())
                .attribute(Attribute.builder().name("price").type(Price.class).build())
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
                .pagination(PaginationImpl.getDefaultPagination(ClassType.of(Book.class)))
                .build();

        EntityProjection actual = maker.parsePath(path);

        projectionEquals(expected, actual);
    }

    @Test
    public void testInvalidSparseFields() {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        queryParams.add("fields[book]", "publisher,bookTitle,bookName"); // Invalid Fields: bookTitle & bookName
        String path = "/book";

        RequestScope scope = new TestRequestScope(dictionary, path, queryParams);
        EntityProjectionMaker maker = new EntityProjectionMaker(dictionary, scope);

        Exception e = assertThrows(InvalidValueException.class, () -> maker.parsePath(path));
        assertEquals("Invalid value: book does not contain the fields: [bookTitle, bookName]", e.getMessage());
    }

    @Test
    public void testInvalidSparseFieldsNested() {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        queryParams.add("fields[book]", "publisher,title");
        queryParams.add("fields[publisher]", "name,cost"); // Invalid Fields: cost
        queryParams.add("include", "publisher");
        String path = "/book";

        RequestScope scope = new TestRequestScope(dictionary, path, queryParams);
        EntityProjectionMaker maker = new EntityProjectionMaker(dictionary, scope);

        Exception e = assertThrows(InvalidValueException.class, () -> maker.parsePath(path));
        assertEquals("Invalid value: publisher does not contain the fields: [cost]", e.getMessage());
    }

    private void projectionEquals(EntityProjection projection1, EntityProjection projection2) {
        assertEquals(projection1.getType(), projection2.getType());
        assertEquals(projection1.getSorting(), projection2.getSorting());
        assertEquals(projection1.getFilterExpression(), projection2.getFilterExpression());
        assertEquals(projection1.getPagination(), projection2.getPagination());

        //Ignore order
        assertEquals(Sets.newHashSet(projection1.getAttributes()), Sets.newHashSet(projection2.getAttributes()));

        assertEquals(projection1.getRelationships().size(), projection2.getRelationships().size());

        projection1.getRelationships().stream().forEach((relationship -> {
            assertNotNull(projection2.getRelationship(relationship.getName()).orElse(null));
            Relationship relationship2 = projection2.getRelationship(relationship.getName()).get();

            assertEquals(relationship.getAlias(), relationship2.getAlias());
            projectionEquals(relationship.getProjection(), relationship2.getProjection());
        }));

    }
}
