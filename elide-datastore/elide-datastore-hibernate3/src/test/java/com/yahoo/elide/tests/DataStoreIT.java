/*
 * Copyright 2017, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.tests;

import static org.testng.Assert.assertEquals;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideResponse;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.audit.TestAuditLogger;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.initialization.AbstractIntegrationTestInitializer;
import com.yahoo.elide.utils.JsonParser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import example.Author;
import example.Book;
import example.Chapter;
import example.Filtered;
import example.TestCheckMappings;

import org.apache.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.MultivaluedHashMap;

public class DataStoreIT extends AbstractIntegrationTestInitializer {
    private final JsonParser jsonParser;
    private final ObjectMapper mapper;
    private final Elide elide;

    private static final String CLASH_OF_KINGS = "A Clash of Kings";
    private static final String STORM_OF_SWORDS = "A Storm of Swords";
    private static final String SONG_OF_ICE_AND_FIRE = "A Song of Ice and Fire";
    private static final String DATA = "data";
    private static final String ATTRIBUTES = "attributes";
    private static final String TITLE = "title";
    private static final String SORT = "sort";
    private static final String CHAPTER_COUNT = "chapterCount";

    private static final int ICE_AND_FIRE_CHAPTER_COUNT = 10;
    private static final int CLASH_OF_KINGS_CHAPTER_COUNT = 20;
    private static final int STORM_OF_SWORDS_CHAPTER_COUNT = 30;
    private static final int ALL_BOOKS_COUNT = 3;

    public DataStoreIT() {
        jsonParser = new JsonParser();
        mapper = new ObjectMapper();
        elide = new Elide(new ElideSettingsBuilder(AbstractIntegrationTestInitializer.getDatabaseManager())
                .withAuditLogger(new TestAuditLogger())
                .withEntityDictionary(new EntityDictionary(TestCheckMappings.MAPPINGS))
                .build());
    }

    @BeforeClass
    public static void setUp() throws IOException {
        try (DataStoreTransaction tx = dataStore.beginTransaction()) {

            tx.save(tx.createNewObject(Filtered.class), null);
            tx.save(tx.createNewObject(Filtered.class), null);
            tx.save(tx.createNewObject(Filtered.class), null);

            Author georgeMartin = new Author();
            georgeMartin.setName("George R. R. Martin");

            Book book1 = new Book();
            book1.setTitle(SONG_OF_ICE_AND_FIRE);
            book1.setAuthors(Arrays.asList(georgeMartin));
            Book book2 = new Book();
            book2.setTitle(CLASH_OF_KINGS);
            book2.setAuthors(Arrays.asList(georgeMartin));
            Book book3 = new Book();
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
        ElideResponse response = elide.get("/book", queryParams, 1);

        JsonNode result = mapper.readTree(response.getBody());
        assertEquals(result.get(DATA).size(), ALL_BOOKS_COUNT);
        assertEquals(result.get(DATA).get(0).get(ATTRIBUTES).get(TITLE).asText(), SONG_OF_ICE_AND_FIRE);
        assertEquals(result.get(DATA).get(1).get(ATTRIBUTES).get(TITLE).asText(), CLASH_OF_KINGS);
        assertEquals(result.get(DATA).get(2).get(ATTRIBUTES).get(TITLE).asText(), STORM_OF_SWORDS);

        assertEquals(result.get(DATA).get(0).get(ATTRIBUTES).get(CHAPTER_COUNT).asInt(), ICE_AND_FIRE_CHAPTER_COUNT);
        assertEquals(result.get(DATA).get(1).get(ATTRIBUTES).get(CHAPTER_COUNT).asInt(), CLASH_OF_KINGS_CHAPTER_COUNT);
        assertEquals(result.get(DATA).get(2).get(ATTRIBUTES).get(CHAPTER_COUNT).asInt(), STORM_OF_SWORDS_CHAPTER_COUNT);
    }

    @Test
    public void testSubcollectionEntityFormulaFetch() throws Exception {
        MultivaluedHashMap<String, String> queryParams = new MultivaluedHashMap<>();
        queryParams.put("fields[book]", Arrays.asList("title,chapterCount"));
        ElideResponse response = elide.get("/author/1/books", queryParams, 1);

        JsonNode result = mapper.readTree(response.getBody());
        assertEquals(result.get(DATA).size(), ALL_BOOKS_COUNT);
        assertEquals(result.get(DATA).get(0).get(ATTRIBUTES).get(TITLE).asText(), SONG_OF_ICE_AND_FIRE);
        assertEquals(result.get(DATA).get(1).get(ATTRIBUTES).get(TITLE).asText(), CLASH_OF_KINGS);
        assertEquals(result.get(DATA).get(2).get(ATTRIBUTES).get(TITLE).asText(), STORM_OF_SWORDS);

        assertEquals(result.get(DATA).get(0).get(ATTRIBUTES).get(CHAPTER_COUNT).asInt(), ICE_AND_FIRE_CHAPTER_COUNT);
        assertEquals(result.get(DATA).get(1).get(ATTRIBUTES).get(CHAPTER_COUNT).asInt(), CLASH_OF_KINGS_CHAPTER_COUNT);
        assertEquals(result.get(DATA).get(2).get(ATTRIBUTES).get(CHAPTER_COUNT).asInt(), STORM_OF_SWORDS_CHAPTER_COUNT);
    }

    @Test
    public void testRootEntityFormulaWithFilter() throws Exception {
        MultivaluedHashMap<String, String> queryParams = new MultivaluedHashMap<>();
        queryParams.put("fields[book]", Arrays.asList("title,chapterCount"));
        queryParams.put("filter[book.chapterCount]", Arrays.asList("20"));
        ElideResponse response = elide.get("/book", queryParams, 1);

        JsonNode result = mapper.readTree(response.getBody());
        assertEquals(result.get(DATA).size(), 1);
        assertEquals(result.get(DATA).get(0).get(ATTRIBUTES).get(TITLE).asText(), CLASH_OF_KINGS);

        assertEquals(result.get(DATA).get(0).get(ATTRIBUTES).get(CHAPTER_COUNT).asInt(), CLASH_OF_KINGS_CHAPTER_COUNT);
    }

    @Test
    public void testSubCollectionEntityFormulaWithFilter() throws Exception {
        MultivaluedHashMap<String, String> queryParams = new MultivaluedHashMap<>();
        queryParams.put("fields[book]", Arrays.asList("title,chapterCount"));
        queryParams.put("filter[book.chapterCount]", Arrays.asList("20"));
        ElideResponse response = elide.get("/author/1/books", queryParams, 1);

        JsonNode result = mapper.readTree(response.getBody());
        assertEquals(result.get(DATA).size(), 1);
        assertEquals(result.get(DATA).get(0).get(ATTRIBUTES).get(TITLE).asText(), CLASH_OF_KINGS);

        assertEquals(result.get(DATA).get(0).get(ATTRIBUTES).get(CHAPTER_COUNT).asInt(), CLASH_OF_KINGS_CHAPTER_COUNT);
    }

    @Test
    public void testRootEntityFormulaWithSorting() throws Exception {
        MultivaluedHashMap<String, String> queryParams = new MultivaluedHashMap<>();
        queryParams.put("fields[book]", Arrays.asList("title,chapterCount"));
        queryParams.put(SORT, Arrays.asList("-chapterCount"));
        ElideResponse response = elide.get("/book", queryParams, 1);

        JsonNode result = mapper.readTree(response.getBody());
        assertEquals(result.get(DATA).size(), ALL_BOOKS_COUNT);
        assertEquals(result.get(DATA).get(0).get(ATTRIBUTES).get(TITLE).asText(), STORM_OF_SWORDS);
        assertEquals(result.get(DATA).get(1).get(ATTRIBUTES).get(TITLE).asText(), CLASH_OF_KINGS);
        assertEquals(result.get(DATA).get(2).get(ATTRIBUTES).get(TITLE).asText(), SONG_OF_ICE_AND_FIRE);

        assertEquals(result.get(DATA).get(0).get(ATTRIBUTES).get(CHAPTER_COUNT).asInt(), STORM_OF_SWORDS_CHAPTER_COUNT);
        assertEquals(result.get(DATA).get(1).get(ATTRIBUTES).get(CHAPTER_COUNT).asInt(), CLASH_OF_KINGS_CHAPTER_COUNT);
        assertEquals(result.get(DATA).get(2).get(ATTRIBUTES).get(CHAPTER_COUNT).asInt(), ICE_AND_FIRE_CHAPTER_COUNT);
    }

    @Test
    public void testSubcollectionEntityFormulaWithSorting() throws Exception {
        MultivaluedHashMap<String, String> queryParams = new MultivaluedHashMap<>();
        queryParams.put("fields[book]", Arrays.asList("title,chapterCount"));
        queryParams.put(SORT, Arrays.asList("-chapterCount"));
        ElideResponse response = elide.get("/author/1/books", queryParams, 1);

        JsonNode result = mapper.readTree(response.getBody());
        assertEquals(result.get(DATA).size(), 3);
        assertEquals(result.get(DATA).get(0).get(ATTRIBUTES).get(TITLE).asText(), STORM_OF_SWORDS);
        assertEquals(result.get(DATA).get(1).get(ATTRIBUTES).get(TITLE).asText(), CLASH_OF_KINGS);
        assertEquals(result.get(DATA).get(2).get(ATTRIBUTES).get(TITLE).asText(), SONG_OF_ICE_AND_FIRE);

        assertEquals(result.get(DATA).get(0).get(ATTRIBUTES).get(CHAPTER_COUNT).asInt(), STORM_OF_SWORDS_CHAPTER_COUNT);
        assertEquals(result.get(DATA).get(1).get(ATTRIBUTES).get(CHAPTER_COUNT).asInt(), CLASH_OF_KINGS_CHAPTER_COUNT);
        assertEquals(result.get(DATA).get(2).get(ATTRIBUTES).get(CHAPTER_COUNT).asInt(), ICE_AND_FIRE_CHAPTER_COUNT);
    }

    @Test
    public void testFilteredWithPassingCheck() {
        String expected = jsonParser.getJson("/ResourceIT/testFilteredPass.json");

        ElideResponse response = elide.get("filtered", new MultivaluedHashMap<>(), 1);
        assertEquals(response.getResponseCode(), HttpStatus.SC_OK);
        assertEquals(response.getBody(), expected);
    }

    @Test
    public void testFilteredWithFailingCheck() {
        String expected = jsonParser.getJson("/ResourceIT/testFilteredFail.json");

        ElideResponse response = elide.get("filtered", new MultivaluedHashMap<>(), -1);
        assertEquals(response.getResponseCode(), HttpStatus.SC_OK);
        assertEquals(response.getBody(), expected);
    }
}
