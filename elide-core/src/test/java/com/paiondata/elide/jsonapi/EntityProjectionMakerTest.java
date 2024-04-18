/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.jsonapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.paiondata.elide.core.Path;
import com.paiondata.elide.core.RequestScope;
import com.paiondata.elide.core.TestRequestScope;
import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.exceptions.InvalidValueException;
import com.paiondata.elide.core.filter.expression.FilterExpression;
import com.paiondata.elide.core.filter.predicates.InPredicate;
import com.paiondata.elide.core.pagination.PaginationImpl;
import com.paiondata.elide.core.request.Attribute;
import com.paiondata.elide.core.request.EntityProjection;
import com.paiondata.elide.core.request.Relationship;
import com.paiondata.elide.core.request.Sorting;
import com.paiondata.elide.core.sort.SortingImpl;
import com.paiondata.elide.core.type.ClassType;
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


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

    static void add(Map<String, List<String>> params, String key, String value) {
        params.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
    }

    @Test
    public void testRootCollectionNoQueryParams() {
        Map<String, List<String>> queryParams = new LinkedHashMap<>();
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
        Map<String, List<String>> queryParams = new LinkedHashMap<>();
        add(queryParams, "fields[book]", "title,publishDate,authors");
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
        Map<String, List<String>> queryParams = new LinkedHashMap<>();
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
        Map<String, List<String>> queryParams = new LinkedHashMap<>();
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
        Map<String, List<String>> queryParams = new LinkedHashMap<>();
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
        Map<String, List<String>> queryParams = new LinkedHashMap<>();
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
        Map<String, List<String>> queryParams = new LinkedHashMap<>();
        add(queryParams, "include", "authors");
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
        Map<String, List<String>> queryParams = new LinkedHashMap<>();
        add(queryParams, "include", "authors");
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
        Map<String, List<String>> queryParams = new LinkedHashMap<>();
        add(queryParams, "include", "authors");
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
        Map<String, List<String>> queryParams = new LinkedHashMap<>();
        add(queryParams, "include", "books");
        add(queryParams, "include", "books.publisher,books.editor");
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
        Map<String, List<String>> queryParams = new LinkedHashMap<>();
        add(queryParams, "include", "books");
        add(queryParams, "include", "books.publisher,books.editor");
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
        Map<String, List<String>> queryParams = new LinkedHashMap<>();
        add(queryParams, "include", "books");
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
        Map<String, List<String>> queryParams = new LinkedHashMap<>();
        add(queryParams, "include", "books");
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
        Map<String, List<String>> queryParams = new LinkedHashMap<>();
        add(queryParams, "include", "books");
        add(queryParams, "include", "books.publisher,books.editor");
        add(queryParams, "fields[publisher]", "name");
        add(queryParams, "fields[editor]", "fullName");
        add(queryParams, "fields[book]", "publisher,editor,title");
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
        Map<String, List<String>> queryParams = new LinkedHashMap<>();
        add(queryParams, "filter", "genre=='Science Fiction'");
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
        Map<String, List<String>> queryParams = new LinkedHashMap<>();
        add(queryParams, "filter[publisher]", "name=='Foo'");
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
        Map<String, List<String>> queryParams = new LinkedHashMap<>();
        add(queryParams, "include", "authors");
        add(queryParams, "filter[author]", "name=='Foo'");
        add(queryParams, "filter[publisher]", "name=='Foo'");
        add(queryParams, "sort", "name");
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
        Map<String, List<String>> queryParams = new LinkedHashMap<>();
        add(queryParams, "filter[book]", "genre=='Science Fiction'");
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
        Map<String, List<String>> queryParams = new LinkedHashMap<>();
        add(queryParams, "fields[book]", "publisher,bookTitle,bookName"); // Invalid Fields: bookTitle & bookName
        String path = "/book";

        RequestScope scope = new TestRequestScope(dictionary, path, queryParams);
        EntityProjectionMaker maker = new EntityProjectionMaker(dictionary, scope);

        Exception e = assertThrows(InvalidValueException.class, () -> maker.parsePath(path));
        assertEquals("Invalid value: book does not contain the fields: [bookTitle, bookName]", e.getMessage());
    }

    @Test
    public void testInvalidSparseFieldsNested() {
        Map<String, List<String>> queryParams = new LinkedHashMap<>();
        add(queryParams, "fields[book]", "publisher,title");
        add(queryParams, "fields[publisher]", "name,cost"); // Invalid Fields: cost
        add(queryParams, "include", "publisher");
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
