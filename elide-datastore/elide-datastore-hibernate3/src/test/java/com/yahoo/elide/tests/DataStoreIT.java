/*
 * Copyright 2015, Yahoo Inc.
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

import example.Author;
import example.Book;
import example.Chapter;
import example.TestCheckMappings;
import org.apache.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import example.Filtered;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.MultivaluedHashMap;

public class DataStoreIT extends AbstractIntegrationTestInitializer {
    private final JsonParser jsonParser = new JsonParser();

    @BeforeClass
    public static void setup() throws IOException {
        try (DataStoreTransaction tx = dataStore.beginTransaction()) {

            tx.save(tx.createNewObject(Filtered.class), null);
            tx.save(tx.createNewObject(Filtered.class), null);
            tx.save(tx.createNewObject(Filtered.class), null);

            Author georgeMartin = new Author();
            georgeMartin.setName("George R. R. Martin");

            Book book1 = new Book();
            book1.setTitle("A Song of Ice and Fire");
            Book book2 = new Book();
            book2.setTitle("A Clash of Kings");
            Book book3 = new Book();
            book3.setTitle("A Storm of Swords");

            georgeMartin.setBooks(Arrays.asList(book1, book2, book3));

            addChapters(10, book1, tx);
            addChapters(20, book2, tx);
            addChapters(30, book3, tx);

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
    public void testFilteredWithPassingCheck() {
        String expected = jsonParser.getJson("/ResourceIT/testFilteredPass.json");

        Elide elide = new Elide(new ElideSettingsBuilder(AbstractIntegrationTestInitializer.getDatabaseManager())
                .withAuditLogger(new TestAuditLogger())
                .withEntityDictionary(new EntityDictionary(TestCheckMappings.MAPPINGS))
                .build());
        ElideResponse response = elide.get("filtered", new MultivaluedHashMap<>(), 1);
        assertEquals(response.getResponseCode(), HttpStatus.SC_OK);
        assertEquals(response.getBody(), expected);
    }

    @Test
    public void testFilteredWithFailingCheck() {
        String expected = jsonParser.getJson("/ResourceIT/testFilteredFail.json");

        Elide elide = new Elide(new ElideSettingsBuilder(AbstractIntegrationTestInitializer.getDatabaseManager())
                .withAuditLogger(new TestAuditLogger())
                .withEntityDictionary(new EntityDictionary(TestCheckMappings.MAPPINGS))
                .build());
        ElideResponse response = elide.get("filtered", new MultivaluedHashMap<>(), -1);
        assertEquals(response.getResponseCode(), HttpStatus.SC_OK);
        assertEquals(response.getBody(), expected);
    }
}
