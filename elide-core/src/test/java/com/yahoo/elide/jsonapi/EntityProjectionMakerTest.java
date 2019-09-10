/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.jsonapi;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.request.Attribute;
import com.yahoo.elide.request.EntityProjection;
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

        EntityProjectionMaker maker = new EntityProjectionMaker(dictionary, queryParams);

        EntityProjection expected = EntityProjection.builder()
                .type(Book.class)
                .dictionary(dictionary)
                .attribute(Attribute.builder().name("title").type(String.class).build())
                .attribute(Attribute.builder().name("genre").type(String.class).build())
                .attribute(Attribute.builder().name("language").type(String.class).build())
                .attribute(Attribute.builder().name("publishDate").type(long.class).build())
                .relationship("authors", EntityProjection.builder()
                        .type(Author.class)
                        .dictionary(dictionary)
                        .build())
                .relationship("publisher", EntityProjection.builder()
                        .type(Publisher.class)
                        .dictionary(dictionary)
                        .build())
                .relationship("editor", EntityProjection.builder()
                        .type(Editor.class)
                        .dictionary(dictionary)
                        .build())
                .build();

        EntityProjection actual = maker.make(path);

        assertEquals(expected, actual);
    }

    @Test
    public void testRootCollectionSparseFields() {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        queryParams.add("fields[book]", "title,publishDate,authors");
        String path = "/book";

        EntityProjectionMaker maker = new EntityProjectionMaker(dictionary, queryParams);

        EntityProjection expected = EntityProjection.builder()
                .type(Book.class)
                .dictionary(dictionary)
                .attribute(Attribute.builder().name("title").type(String.class).build())
                .attribute(Attribute.builder().name("publishDate").type(long.class).build())
                .relationship("authors", EntityProjection.builder()
                        .type(Author.class)
                        .dictionary(dictionary)
                        .build())
                .build();

        EntityProjection actual = maker.make(path);

        assertEquals(expected, actual);
    }

    @Test
    public void testRootEntityNoQueryParams() {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        String path = "/book/1";

        EntityProjectionMaker maker = new EntityProjectionMaker(dictionary, queryParams);

        EntityProjection expected = EntityProjection.builder()
                .type(Book.class)
                .dictionary(dictionary)
                .attribute(Attribute.builder().name("title").type(String.class).build())
                .attribute(Attribute.builder().name("genre").type(String.class).build())
                .attribute(Attribute.builder().name("language").type(String.class).build())
                .attribute(Attribute.builder().name("publishDate").type(long.class).build())
                .relationship("authors", EntityProjection.builder()
                        .type(Author.class)
                        .dictionary(dictionary)
                        .build())
                .relationship("publisher", EntityProjection.builder()
                        .type(Publisher.class)
                        .dictionary(dictionary)
                        .build())
                .relationship("editor", EntityProjection.builder()
                        .type(Editor.class)
                        .dictionary(dictionary)
                        .build())
                .build();

        EntityProjection actual = maker.make(path);

        assertEquals(expected, actual);
    }

    @Test
    public void testNestedCollectionNoQueryParams() {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        String path = "/author/1/books/3/publisher";

        EntityProjectionMaker maker = new EntityProjectionMaker(dictionary, queryParams);

        EntityProjection expected = EntityProjection.builder()
                .type(Author.class)
                .dictionary(dictionary)
                .relationship("books", EntityProjection.builder()
                        .dictionary(dictionary)
                        .type(Book.class)
                        .relationship("publisher", EntityProjection.builder()
                                .dictionary(dictionary)
                                .type(Publisher.class)
                                .attribute(Attribute.builder().name("name").type(String.class).build())
                                .attribute(Attribute.builder().name("updateHookInvoked").type(boolean.class).build())
                                .relationship("books", EntityProjection.builder()
                                        .dictionary(dictionary)
                                        .type(Book.class)
                                        .build())
                                .relationship("editor", EntityProjection.builder()
                                        .dictionary(dictionary)
                                        .type(Editor.class)
                                        .build())
                                .build())
                        .build())
                .build();

        EntityProjection actual = maker.make(path);

        assertEquals(expected, actual);
    }

    @Test
    public void testNestedEntityNoQueryParams() {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        String path = "/author/1/books/3/publisher/1";

        EntityProjectionMaker maker = new EntityProjectionMaker(dictionary, queryParams);

        EntityProjection expected = EntityProjection.builder()
                .type(Author.class)
                .dictionary(dictionary)
                .relationship("books", EntityProjection.builder()
                        .dictionary(dictionary)
                        .type(Book.class)
                        .relationship("publisher", EntityProjection.builder()
                                .dictionary(dictionary)
                                .type(Publisher.class)
                                .attribute(Attribute.builder().name("name").type(String.class).build())
                                .attribute(Attribute.builder().name("updateHookInvoked").type(boolean.class).build())
                                .relationship("books", EntityProjection.builder()
                                        .dictionary(dictionary)
                                        .type(Book.class)
                                        .build())
                                .relationship("editor", EntityProjection.builder()
                                        .dictionary(dictionary)
                                        .type(Editor.class)
                                        .build())
                                .build())
                        .build())
                .build();

        EntityProjection actual = maker.make(path);

        assertEquals(expected, actual);
    }

    @Test
    public void testRelationshipNoQueryParams() {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        String path = "/author/1/relationships/books";

        EntityProjectionMaker maker = new EntityProjectionMaker(dictionary, queryParams);

        EntityProjection expected = EntityProjection.builder()
                .type(Author.class)
                .dictionary(dictionary)
                .relationship("books", EntityProjection.builder()
                        .dictionary(dictionary)
                        .type(Book.class)
                        .build())
                .build();

        EntityProjection actual = maker.make(path);

        assertEquals(expected, actual);
    }

    @Test
    public void testRelationshipWithSingleInclude() {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        queryParams.add("include", "authors");
        String path = "/book/1/relationships/publisher";

        EntityProjectionMaker maker = new EntityProjectionMaker(dictionary, queryParams);

        EntityProjection expected = EntityProjection.builder()
                .type(Book.class)
                .dictionary(dictionary)
                .relationship("publisher", EntityProjection.builder()
                        .dictionary(dictionary)
                        .type(Publisher.class)
                        .build())
                .relationship("authors", EntityProjection.builder()
                        .dictionary(dictionary)
                        .attribute(Attribute.builder().name("name").type(String.class).build())
                        .attribute(Attribute.builder().name("type").type(Author.AuthorType.class).build())
                        .attribute(Attribute.builder().name("homeAddress").type(Address.class).build())
                        .relationship("books", EntityProjection.builder()
                                .type(Book.class)
                                .dictionary(dictionary)
                                .build())
                        .type(Author.class)
                        .build())
                .relationship("editor", EntityProjection.builder()
                        .dictionary(dictionary)
                        .type(Editor.class)
                        .build())
                .build();

        EntityProjection actual = maker.make(path);

        assertEquals(expected, actual);
    }

    @Test
    public void testRootCollectionWithSingleInclude() {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        queryParams.add("include", "authors");
        String path = "/book";

        EntityProjectionMaker maker = new EntityProjectionMaker(dictionary, queryParams);

        EntityProjection expected = EntityProjection.builder()
                .type(Book.class)
                .dictionary(dictionary)
                .attribute(Attribute.builder().name("title").type(String.class).build())
                .attribute(Attribute.builder().name("genre").type(String.class).build())
                .attribute(Attribute.builder().name("language").type(String.class).build())
                .attribute(Attribute.builder().name("publishDate").type(long.class).build())
                .relationship("authors", EntityProjection.builder()
                        .type(Author.class)
                        .dictionary(dictionary)
                        .attribute(Attribute.builder().name("name").type(String.class).build())
                        .attribute(Attribute.builder().name("type").type(Author.AuthorType.class).build())
                        .attribute(Attribute.builder().name("homeAddress").type(Address.class).build())
                        .relationship("books", EntityProjection.builder()
                                .type(Book.class)
                                .dictionary(dictionary)
                                .build())
                        .build())
                .relationship("publisher", EntityProjection.builder()
                        .type(Publisher.class)
                        .dictionary(dictionary)
                        .build())
                .relationship("editor", EntityProjection.builder()
                        .type(Editor.class)
                        .dictionary(dictionary)
                        .build())
                .build();

        EntityProjection actual = maker.make(path);

        assertEquals(expected, actual);
    }

    @Test
    public void testRootEntityWithSingleInclude() {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        queryParams.add("include", "authors");
        String path = "/book/1";

        EntityProjectionMaker maker = new EntityProjectionMaker(dictionary, queryParams);

        EntityProjection expected = EntityProjection.builder()
                .type(Book.class)
                .dictionary(dictionary)
                .attribute(Attribute.builder().name("title").type(String.class).build())
                .attribute(Attribute.builder().name("genre").type(String.class).build())
                .attribute(Attribute.builder().name("language").type(String.class).build())
                .attribute(Attribute.builder().name("publishDate").type(long.class).build())
                .relationship("authors", EntityProjection.builder()
                        .type(Author.class)
                        .dictionary(dictionary)
                        .attribute(Attribute.builder().name("name").type(String.class).build())
                        .attribute(Attribute.builder().name("type").type(Author.AuthorType.class).build())
                        .attribute(Attribute.builder().name("homeAddress").type(Address.class).build())
                        .relationship("books", EntityProjection.builder()
                                .type(Book.class)
                                .dictionary(dictionary)
                                .build())
                        .build())
                .relationship("publisher", EntityProjection.builder()
                        .type(Publisher.class)
                        .dictionary(dictionary)
                        .build())
                .relationship("editor", EntityProjection.builder()
                        .type(Editor.class)
                        .dictionary(dictionary)
                        .build())
                .build();

        EntityProjection actual = maker.make(path);

        assertEquals(expected, actual);
    }

    @Test
    public void testRootCollectionWithNestedInclude() throws Exception {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        queryParams.add("include", "books");
        queryParams.add("include", "books.publisher,books.editor");
        String path = "/author";

        EntityProjectionMaker maker = new EntityProjectionMaker(dictionary, queryParams);

        EntityProjection expected = EntityProjection.builder()
                .type(Author.class)
                .dictionary(dictionary)
                .attribute(Attribute.builder().name("homeAddress").type(Address.class).build())
                .attribute(Attribute.builder().name("name").type(String.class).build())
                .attribute(Attribute.builder().name("type").type(Author.AuthorType.class).build())
                .relationship("books", EntityProjection.builder()
                        .type(Book.class)
                        .dictionary(dictionary)
                        .attribute(Attribute.builder().name("title").type(String.class).build())
                        .attribute(Attribute.builder().name("genre").type(String.class).build())
                        .attribute(Attribute.builder().name("language").type(String.class).build())
                        .attribute(Attribute.builder().name("publishDate").type(long.class).build())
                        .relationship("editor", EntityProjection.builder()
                                .type(Editor.class)
                                .dictionary(dictionary)
                                .attribute(Attribute.builder().name("firstName").type(String.class).build())
                                .attribute(Attribute.builder().name("lastName").type(String.class).build())
                                .attribute(Attribute.builder().name("fullName").type(String.class).build())
                                .relationship("editor", EntityProjection.builder()
                                        .dictionary(dictionary)
                                        .type(Editor.class)
                                        .build())
                                .build())
                        .relationship("publisher", EntityProjection.builder()
                                .type(Publisher.class)
                                .dictionary(dictionary)
                                .attribute(Attribute.builder().name("name").type(String.class).build())
                                .attribute(Attribute.builder().name("updateHookInvoked").type(boolean.class).build())
                                .relationship("books", EntityProjection.builder()
                                        .dictionary(dictionary)
                                        .type(Book.class)
                                        .build())
                                .relationship("editor", EntityProjection.builder()
                                        .dictionary(dictionary)
                                        .type(Editor.class)
                                        .build())
                                .build())
                        .relationship("authors", EntityProjection.builder()
                                .dictionary(dictionary)
                                .type(Author.class)
                                .build())
                        .build())
                .build();

        EntityProjection actual = maker.make(path);

        assertEquals(expected, actual);
    }

    @Test
    public void testRootEntityWithNestedInclude() {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        queryParams.add("include", "books");
        queryParams.add("include", "books.publisher,books.editor");
        String path = "/author/1";

        EntityProjectionMaker maker = new EntityProjectionMaker(dictionary, queryParams);

        EntityProjection expected = EntityProjection.builder()
                .type(Author.class)
                .dictionary(dictionary)
                .attribute(Attribute.builder().name("name").type(String.class).build())
                .attribute(Attribute.builder().name("type").type(Author.AuthorType.class).build())
                .attribute(Attribute.builder().name("homeAddress").type(Address.class).build())
                .relationship("books", EntityProjection.builder()
                        .type(Book.class)
                        .dictionary(dictionary)
                        .attribute(Attribute.builder().name("title").type(String.class).build())
                        .attribute(Attribute.builder().name("genre").type(String.class).build())
                        .attribute(Attribute.builder().name("language").type(String.class).build())
                        .attribute(Attribute.builder().name("publishDate").type(long.class).build())
                        .relationship("publisher", EntityProjection.builder()
                                .type(Publisher.class)
                                .dictionary(dictionary)
                                .attribute(Attribute.builder().name("name").type(String.class).build())
                                .attribute(Attribute.builder().name("updateHookInvoked").type(boolean.class).build())
                                .relationship("books", EntityProjection.builder()
                                        .dictionary(dictionary)
                                        .type(Book.class)
                                        .build())
                                .relationship("editor", EntityProjection.builder()
                                        .dictionary(dictionary)
                                        .type(Editor.class)
                                        .build())
                                .build())
                        .relationship("editor", EntityProjection.builder()
                                .type(Editor.class)
                                .dictionary(dictionary)
                                .attribute(Attribute.builder().name("firstName").type(String.class).build())
                                .attribute(Attribute.builder().name("lastName").type(String.class).build())
                                .attribute(Attribute.builder().name("fullName").type(String.class).build())
                                .relationship("editor", EntityProjection.builder()
                                        .dictionary(dictionary)
                                        .type(Editor.class)
                                        .build())
                                .build())
                        .relationship("authors", EntityProjection.builder()
                                .dictionary(dictionary)
                                .type(Author.class)
                                .build())
                        .build())
                .build();

        EntityProjection actual = maker.make(path);

        assertEquals(expected, actual);
    }

    @Test
    public void testNestedEntityWithSingleInclude() {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        queryParams.add("include", "books");
        String path = "/author/1/books/3/publisher/1";

        EntityProjectionMaker maker = new EntityProjectionMaker(dictionary, queryParams);

        EntityProjection expected = EntityProjection.builder()
                .type(Author.class)
                .dictionary(dictionary)
                .relationship("books", EntityProjection.builder()
                        .dictionary(dictionary)
                        .type(Book.class)
                        .relationship("publisher", EntityProjection.builder()
                                .dictionary(dictionary)
                                .type(Publisher.class)
                                .attribute(Attribute.builder().name("name").type(String.class).build())
                                .attribute(Attribute.builder().name("updateHookInvoked").type(boolean.class).build())
                                .relationship("books", EntityProjection.builder()
                                        .dictionary(dictionary)
                                        .type(Book.class)
                                        .attribute(Attribute.builder().name("title").type(String.class).build())
                                        .attribute(Attribute.builder().name("genre").type(String.class).build())
                                        .attribute(Attribute.builder().name("language").type(String.class).build())
                                        .attribute(Attribute.builder().name("publishDate").type(long.class).build())
                                        .relationship("authors", EntityProjection.builder()
                                                .dictionary(dictionary)
                                                .type(Author.class)
                                                .build())
                                        .relationship("publisher", EntityProjection.builder()
                                                .dictionary(dictionary)
                                                .type(Publisher.class)
                                                .build())
                                        .relationship("editor", EntityProjection.builder()
                                                .dictionary(dictionary)
                                                .type(Editor.class)
                                                .build())
                                        .build())
                                .relationship("editor", EntityProjection.builder()
                                        .dictionary(dictionary)
                                        .type(Editor.class)
                                        .build())
                                .build())
                        .build())
                .build();

        EntityProjection actual = maker.make(path);

        assertEquals(expected, actual);
    }

    @Test
    public void testNestedCollectionWithSingleInclude() {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        queryParams.add("include", "books");
        String path = "/author/1/books/3/publisher";

        EntityProjectionMaker maker = new EntityProjectionMaker(dictionary, queryParams);

        EntityProjection expected = EntityProjection.builder()
                .type(Author.class)
                .dictionary(dictionary)
                .relationship("books", EntityProjection.builder()
                        .dictionary(dictionary)
                        .type(Book.class)
                        .relationship("publisher", EntityProjection.builder()
                                .dictionary(dictionary)
                                .type(Publisher.class)
                                .attribute(Attribute.builder().name("name").type(String.class).build())
                                .attribute(Attribute.builder().name("updateHookInvoked").type(boolean.class).build())
                                .relationship("books", EntityProjection.builder()
                                        .dictionary(dictionary)
                                        .attribute(Attribute.builder().name("title").type(String.class).build())
                                        .attribute(Attribute.builder().name("genre").type(String.class).build())
                                        .attribute(Attribute.builder().name("language").type(String.class).build())
                                        .attribute(Attribute.builder().name("publishDate").type(long.class).build())
                                        .relationship("authors", EntityProjection.builder()
                                                .dictionary(dictionary)
                                                .type(Author.class)
                                                .build())
                                        .relationship("publisher", EntityProjection.builder()
                                                .dictionary(dictionary)
                                                .type(Publisher.class)
                                                .build())
                                        .relationship("editor", EntityProjection.builder()
                                                .dictionary(dictionary)
                                                .type(Editor.class)
                                                .build())
                                        .type(Book.class)
                                        .build())
                                .relationship("editor", EntityProjection.builder()
                                        .dictionary(dictionary)
                                        .type(Editor.class)
                                        .build())
                                .build())
                        .build())
                .build();

        EntityProjection actual = maker.make(path);

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

        EntityProjectionMaker maker = new EntityProjectionMaker(dictionary, queryParams);

        EntityProjection expected = EntityProjection.builder()
                .type(Author.class)
                .dictionary(dictionary)
                .attribute(Attribute.builder().name("name").type(String.class).build())
                .attribute(Attribute.builder().name("type").type(Author.AuthorType.class).build())
                .attribute(Attribute.builder().name("homeAddress").type(Address.class).build())
                .relationship("books", EntityProjection.builder()
                        .type(Book.class)
                        .dictionary(dictionary)
                        .attribute(Attribute.builder().name("title").type(String.class).build())
                        .relationship("publisher", EntityProjection.builder()
                                .type(Publisher.class)
                                .dictionary(dictionary)
                                .attribute(Attribute.builder().name("name").type(String.class).build())
                                .build())
                        .relationship("editor", EntityProjection.builder()
                                .type(Editor.class)
                                .dictionary(dictionary)
                                .attribute(Attribute.builder().name("fullName").type(String.class).build())
                                .build())
                        .build())
                .build();

        EntityProjection actual = maker.make(path);

        assertEquals(expected, actual);
    }
}
