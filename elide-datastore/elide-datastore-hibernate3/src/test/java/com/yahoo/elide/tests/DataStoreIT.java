/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.tests;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideResponse;
import com.yahoo.elide.audit.TestAuditLogger;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.datastores.hibernate3.HibernateTransaction;
import com.yahoo.elide.initialization.AbstractIntegrationTestInitializer;
import com.yahoo.elide.utils.JsonParser;
import example.Filtered;
import org.apache.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.ws.rs.core.MultivaluedHashMap;

public class DataStoreIT extends AbstractIntegrationTestInitializer {
    private final JsonParser jsonParser = new JsonParser();

    @BeforeClass
    public static void setup() throws IOException {
        try (DataStoreTransaction tx = dataStore.beginTransaction()) {

            tx.save(tx.createObject(Filtered.class));
            tx.save(tx.createObject(Filtered.class));
            tx.save(tx.createObject(Filtered.class));

            tx.commit();
        }
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

    @Test
    public void testIncludeList() throws IOException {
        try (HibernateTransaction tx = (HibernateTransaction) dataStore.beginTransaction()) {
            RequestScope requestScope = mock(RequestScope.class);

            MultivaluedHashMap<String, String> params = new MultivaluedHashMap();
            params.add("include", "foo,bar");
            params.add("include", "car");
            when(requestScope.getQueryParams()).thenReturn(Optional.of(params));

            tx.setRequestScope(requestScope);
            List<String> includes = tx.getIncludeList();
            tx.commit();
            assertEquals(includes, Arrays.asList("foo", "bar", "car"));
        }
    }

    @Test
    public void testJoinQuery() throws IOException {
        try (HibernateTransaction tx = (HibernateTransaction) dataStore.beginTransaction()) {
            RequestScope requestScope = mock(RequestScope.class);

            MultivaluedHashMap<String, String> params = new MultivaluedHashMap();
            when(requestScope.getQueryParams()).thenReturn(Optional.of(params));

            tx.setRequestScope(requestScope);
            assertFalse(tx.isJoinQuery());

            params.add("join", "true");
            assertFalse(tx.isJoinQuery());

            params.remove("join");
            params.add("join", "false");
            assertFalse(tx.isJoinQuery());

            params.remove("join");
            params.add("include", "foo,bar");
            assertFalse(tx.isJoinQuery());

            params.add("join", "false");
            assertFalse(tx.isJoinQuery());

            tx.commit();
        }
    }
}
