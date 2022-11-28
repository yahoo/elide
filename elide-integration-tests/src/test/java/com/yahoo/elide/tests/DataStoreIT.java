/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.tests;

import static com.yahoo.elide.core.dictionary.EntityDictionary.NO_VERSION;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.data;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.id;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.linkage;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.type;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideResponse;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.core.audit.TestAuditLogger;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.security.User;
import com.yahoo.elide.core.security.checks.Check;
import com.yahoo.elide.initialization.IntegrationTest;
import com.yahoo.elide.test.jsonapi.elements.Data;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import example.Author;
import example.Book;
import example.Chapter;
import example.Filtered;
import example.TestCheckMappings;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.MultivaluedHashMap;

@Tag("skipInMemory") // Do not test Memory Datastores
public class DataStoreIT extends IntegrationTest {
    private final ObjectMapper mapper;
    private final Elide elide;
    private final User goodUser;
    private final User badUser;

    private static final String CLASH_OF_KINGS = "A Clash of Kings";
    private static final String STORM_OF_SWORDS = "A Storm of Swords";
    private static final String SONG_OF_ICE_AND_FIRE = "A Song of Ice and Fire";
    private static final String DATA = "data";
    private static final String ATTRIBUTES = "attributes";
    private static final String TITLE = "title";
    private static final String CHAPTER_COUNT = "chapterCount";
    private static final String BASEURL = "http://localhost:8080/api";

    private static final int ICE_AND_FIRE_CHAPTER_COUNT = 10;
    private static final int CLASH_OF_KINGS_CHAPTER_COUNT = 20;
    private static final int STORM_OF_SWORDS_CHAPTER_COUNT = 30;
    private static final int ALL_BOOKS_COUNT = 3;

    public DataStoreIT() {
        goodUser = new User(() -> "1");
        badUser = new User(() -> "-1");

        mapper = new ObjectMapper();
        Map<String, Class<? extends Check>> checks = new HashMap<>(TestCheckMappings.MAPPINGS);
        checks.put("filterCheck", Filtered.FilterCheck.class);
        checks.put("filterCheck3", Filtered.FilterCheck3.class);

        elide = new Elide(new ElideSettingsBuilder(dataStore)
                .withAuditLogger(new TestAuditLogger())
                .withEntityDictionary(EntityDictionary.builder().checks(checks).build())
                .build());

        elide.doScans();
    }

    @BeforeEach
    public void setUp() throws IOException {
        try (DataStoreTransaction tx = dataStore.beginTransaction()) {

            tx.createObject(new Filtered(), null);
            tx.createObject(new Filtered(), null);
            tx.createObject(new Filtered(), null);

            Author georgeMartin = new Author();
            tx.createObject(georgeMartin, null);
            georgeMartin.setName("George R. R. Martin");

            Book book1 = new Book();
            tx.createObject(book1, null);
            book1.setTitle(SONG_OF_ICE_AND_FIRE);
            book1.setAuthors(Arrays.asList(georgeMartin));
            Book book2 = new Book();
            tx.createObject(book2, null);
            book2.setTitle(CLASH_OF_KINGS);
            book2.setAuthors(Arrays.asList(georgeMartin));
            Book book3 = new Book();
            tx.createObject(book3, null);
            book3.setTitle(STORM_OF_SWORDS);
            book3.setAuthors(Arrays.asList(georgeMartin));

            georgeMartin.setBooks(Arrays.asList(book1, book2, book3));

            addChapters(ICE_AND_FIRE_CHAPTER_COUNT, book1, tx);
            addChapters(CLASH_OF_KINGS_CHAPTER_COUNT, book2, tx);
            addChapters(STORM_OF_SWORDS_CHAPTER_COUNT, book3, tx);

            tx.save(book1, null);
            tx.save(book2, null);
            tx.save(book3, null);
            tx.save(georgeMartin, null);

            tx.commit(null);
        }
    }

    private static void addChapters(int numberOfChapters, Book book, DataStoreTransaction tx) {
        Set<Chapter> chapters = new HashSet<>();
        for (int idx = 0; idx < numberOfChapters; idx++) {
            Chapter chapter = new Chapter();
            tx.createObject(chapter, null);
            chapter.setTitle("Chapter" + idx);
            tx.save(chapter, null);
            chapters.add(chapter);
        }
        book.setChapters(chapters);
    }

    @Test
    public void testRootEntityFormulaFetch() throws Exception {
        MultivaluedHashMap<String, String> queryParams = new MultivaluedHashMap<>();
        queryParams.put("fields[book]", Arrays.asList("title,chapterCount"));
        ElideResponse response = elide.get(BASEURL, "/book", queryParams, goodUser, NO_VERSION);
        assertEquals(HttpStatus.SC_OK, response.getResponseCode());

        JsonNode result = mapper.readTree(response.getBody());
        assertEquals(ALL_BOOKS_COUNT, result.get(DATA).size());
        assertEquals(SONG_OF_ICE_AND_FIRE, result.get(DATA).get(0).get(ATTRIBUTES).get(TITLE).asText());
        assertEquals(CLASH_OF_KINGS, result.get(DATA).get(1).get(ATTRIBUTES).get(TITLE).asText());
        assertEquals(STORM_OF_SWORDS, result.get(DATA).get(2).get(ATTRIBUTES).get(TITLE).asText());

        assertEquals(ICE_AND_FIRE_CHAPTER_COUNT, result.get(DATA).get(0).get(ATTRIBUTES).get(CHAPTER_COUNT).asInt());
        assertEquals(CLASH_OF_KINGS_CHAPTER_COUNT, result.get(DATA).get(1).get(ATTRIBUTES).get(CHAPTER_COUNT).asInt());
        assertEquals(STORM_OF_SWORDS_CHAPTER_COUNT, result.get(DATA).get(2).get(ATTRIBUTES).get(CHAPTER_COUNT).asInt());
    }

    @Test
    public void testSubcollectionEntityFormulaFetch() throws Exception {
        MultivaluedHashMap<String, String> queryParams = new MultivaluedHashMap<>();
        queryParams.put("fields[book]", Arrays.asList("title,chapterCount"));
        ElideResponse response = elide.get(BASEURL, "/author/1/books", queryParams, goodUser, NO_VERSION);
        assertEquals(HttpStatus.SC_OK, response.getResponseCode());

        JsonNode result = mapper.readTree(response.getBody());
        assertEquals(ALL_BOOKS_COUNT, result.get(DATA).size());
        assertEquals(SONG_OF_ICE_AND_FIRE, result.get(DATA).get(0).get(ATTRIBUTES).get(TITLE).asText());
        assertEquals(CLASH_OF_KINGS, result.get(DATA).get(1).get(ATTRIBUTES).get(TITLE).asText());
        assertEquals(STORM_OF_SWORDS, result.get(DATA).get(2).get(ATTRIBUTES).get(TITLE).asText());

        assertEquals(ICE_AND_FIRE_CHAPTER_COUNT, result.get(DATA).get(0).get(ATTRIBUTES).get(CHAPTER_COUNT).asInt());
        assertEquals(CLASH_OF_KINGS_CHAPTER_COUNT, result.get(DATA).get(1).get(ATTRIBUTES).get(CHAPTER_COUNT).asInt());
        assertEquals(STORM_OF_SWORDS_CHAPTER_COUNT, result.get(DATA).get(2).get(ATTRIBUTES).get(CHAPTER_COUNT).asInt());
    }

    @Test
    public void testRootEntityFormulaWithFilter() throws Exception {
        MultivaluedHashMap<String, String> queryParams = new MultivaluedHashMap<>();
        queryParams.put("fields[book]", Arrays.asList("title,chapterCount"));
        queryParams.put("filter[book.chapterCount]", Arrays.asList("20"));
        ElideResponse response = elide.get(BASEURL, "/book", queryParams, goodUser, NO_VERSION);
        assertEquals(HttpStatus.SC_OK, response.getResponseCode());

        JsonNode result = mapper.readTree(response.getBody());
        assertEquals(1, result.get(DATA).size());
        assertEquals(CLASH_OF_KINGS, result.get(DATA).get(0).get(ATTRIBUTES).get(TITLE).asText());

        assertEquals(CLASH_OF_KINGS_CHAPTER_COUNT, result.get(DATA).get(0).get(ATTRIBUTES).get(CHAPTER_COUNT).asInt());
    }

    @Test
    public void testSubCollectionEntityFormulaWithFilter() throws Exception {
        MultivaluedHashMap<String, String> queryParams = new MultivaluedHashMap<>();
        queryParams.put("fields[book]", Arrays.asList("title,chapterCount"));
        queryParams.put("filter[book.chapterCount]", Arrays.asList("20"));
        ElideResponse response = elide.get(BASEURL, "/author/1/books", queryParams, goodUser, NO_VERSION);
        assertEquals(HttpStatus.SC_OK, response.getResponseCode());

        JsonNode result = mapper.readTree(response.getBody());
        assertEquals(1, result.get(DATA).size());
        assertEquals(CLASH_OF_KINGS, result.get(DATA).get(0).get(ATTRIBUTES).get(TITLE).asText());

        assertEquals(CLASH_OF_KINGS_CHAPTER_COUNT, result.get(DATA).get(0).get(ATTRIBUTES).get(CHAPTER_COUNT).asInt());
    }

    @Test
    public void testRootEntityFormulaWithSorting() throws Exception {
        MultivaluedHashMap<String, String> queryParams = new MultivaluedHashMap<>();
        queryParams.put("fields[book]", Arrays.asList("title,chapterCount"));
        queryParams.put("sort", Arrays.asList("-chapterCount"));
        ElideResponse response = elide.get(BASEURL, "/book", queryParams, goodUser, NO_VERSION);
        assertEquals(HttpStatus.SC_OK, response.getResponseCode());

        JsonNode result = mapper.readTree(response.getBody());
        assertEquals(ALL_BOOKS_COUNT, result.get(DATA).size());
        assertEquals(STORM_OF_SWORDS, result.get(DATA).get(0).get(ATTRIBUTES).get(TITLE).asText());
        assertEquals(CLASH_OF_KINGS, result.get(DATA).get(1).get(ATTRIBUTES).get(TITLE).asText());
        assertEquals(SONG_OF_ICE_AND_FIRE, result.get(DATA).get(2).get(ATTRIBUTES).get(TITLE).asText());

        assertEquals(STORM_OF_SWORDS_CHAPTER_COUNT, result.get(DATA).get(0).get(ATTRIBUTES).get(CHAPTER_COUNT).asInt());
        assertEquals(CLASH_OF_KINGS_CHAPTER_COUNT, result.get(DATA).get(1).get(ATTRIBUTES).get(CHAPTER_COUNT).asInt());
        assertEquals(ICE_AND_FIRE_CHAPTER_COUNT, result.get(DATA).get(2).get(ATTRIBUTES).get(CHAPTER_COUNT).asInt());
    }

    @Test
    public void testSubcollectionEntityFormulaWithSorting() throws Exception {
        MultivaluedHashMap<String, String> queryParams = new MultivaluedHashMap<>();
        queryParams.put("fields[book]", Arrays.asList("title,chapterCount"));
        queryParams.put("sort", Arrays.asList("-chapterCount"));
        ElideResponse response = elide.get(BASEURL, "/author/1/books", queryParams, goodUser, NO_VERSION);
        assertEquals(HttpStatus.SC_OK, response.getResponseCode());

        JsonNode result = mapper.readTree(response.getBody());
        assertEquals(ALL_BOOKS_COUNT, result.get(DATA).size());
        assertEquals(STORM_OF_SWORDS, result.get(DATA).get(0).get(ATTRIBUTES).get(TITLE).asText());
        assertEquals(CLASH_OF_KINGS, result.get(DATA).get(1).get(ATTRIBUTES).get(TITLE).asText());
        assertEquals(SONG_OF_ICE_AND_FIRE, result.get(DATA).get(2).get(ATTRIBUTES).get(TITLE).asText());

        assertEquals(STORM_OF_SWORDS_CHAPTER_COUNT, result.get(DATA).get(0).get(ATTRIBUTES).get(CHAPTER_COUNT).asInt());
        assertEquals(CLASH_OF_KINGS_CHAPTER_COUNT, result.get(DATA).get(1).get(ATTRIBUTES).get(CHAPTER_COUNT).asInt());
        assertEquals(ICE_AND_FIRE_CHAPTER_COUNT, result.get(DATA).get(2).get(ATTRIBUTES).get(CHAPTER_COUNT).asInt());
    }

    @Test
    public void testFilteredWithPassingCheck() {
        Data data = data(
                linkage(type("filtered"), id("1")),
                linkage(type("filtered"), id("2")),
                linkage(type("filtered"), id("3"))
        );

        ElideResponse response = elide.get(BASEURL, "filtered", new MultivaluedHashMap<>(), goodUser, NO_VERSION);
        assertEquals(HttpStatus.SC_OK, response.getResponseCode());
        assertEquals(data.toJSON(), response.getBody());
    }

    @Test
    public void testFilteredWithFailingCheck() {
        Data data = data(
                linkage(type("filtered"), id("1")),
                linkage(type("filtered"), id("3"))
        );

        ElideResponse response = elide.get(BASEURL, "filtered", new MultivaluedHashMap<>(), badUser, NO_VERSION);
        assertEquals(HttpStatus.SC_OK, response.getResponseCode());
        assertEquals(data.toJSON(), response.getBody());
    }
}
