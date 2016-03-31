/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.tests;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideResponse;
import com.yahoo.elide.audit.TestAuditLogger;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.initialization.AbstractIntegrationTestInitializer;
import com.yahoo.elide.utils.JsonParser;
import example.Filtered;
import org.apache.http.HttpStatus;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import javax.ws.rs.core.MultivaluedHashMap;

import static org.testng.Assert.assertEquals;

public class DataStoreIT extends AbstractIntegrationTestInitializer {
    private final JsonParser jsonParser = new JsonParser();

    @BeforeTest
    public static void setup() {
        DataStoreTransaction tx = dataStore.beginTransaction();

        tx.save(tx.createObject(Filtered.class));
        tx.save(tx.createObject(Filtered.class));
        tx.save(tx.createObject(Filtered.class));

        tx.commit();
    }

    @Test
    public void testFilteredWithPassingCheck() {
        String expected = jsonParser.getJson("/ResourceIT/testFiltered.json");

        Elide elide = new Elide.Builder(new TestAuditLogger(), AbstractIntegrationTestInitializer.getDatabaseManager()).build();
        ElideResponse response = elide.get("filtered", new MultivaluedHashMap<>(), 1);
        assertEquals(response.getResponseCode(), HttpStatus.SC_OK);
        assertEquals(response.getBody(), expected);
    }

    @Test
    public void testFilteredWithFailingCheck() {
        String expected = jsonParser.getJson("/ResourceIT/testFiltered.json");

        Elide elide = new Elide.Builder(new TestAuditLogger(), AbstractIntegrationTestInitializer.getDatabaseManager()).build();
        ElideResponse response = elide.get("filtered", new MultivaluedHashMap<>(), -1);
        assertEquals(response.getResponseCode(), HttpStatus.SC_OK);
        assertEquals(response.getBody(), expected);
    }
}
