/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.async.models.AsyncQuery;
import com.yahoo.elide.async.models.QueryStatus;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.datastore.inmemory.HashMapDataStore;
import com.yahoo.elide.security.checks.Check;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

public class AsyncQueryCleanerThreadTest {

    private AsyncQueryCleanerThread cleanerThread;
    private Elide elide;
    private AsyncQueryDAO asyncQueryDao;


    @BeforeEach
    public void setupMocks() {

        HashMapDataStore inMemoryStore = new HashMapDataStore(AsyncQuery.class.getPackage());
        Map<String, Class<? extends Check>> checkMappings = new HashMap<>();

        elide = new Elide(
                new ElideSettingsBuilder(inMemoryStore)
                        .withEntityDictionary(new EntityDictionary(checkMappings))
                        .withISO8601Dates("yyyy-MM-dd'T'HH:mm'Z'", TimeZone.getTimeZone("UTC"))
                        .build());

        asyncQueryDao = mock(DefaultAsyncQueryDAO.class);
        ResultStorageEngine resultStorageEngine = mock(DefaultResultStorageEngine.class);
        cleanerThread = new AsyncQueryCleanerThread(7, elide, 7, asyncQueryDao,
                resultStorageEngine);
    }

    @Test
    public void testAsyncQueryCleanerThreadSet() {
        assertEquals(elide, cleanerThread.getElide());
        assertEquals(asyncQueryDao, cleanerThread.getAsyncQueryDao());
        assertEquals(7, cleanerThread.getMaxRunTimeMinutes());
        assertEquals(7, cleanerThread.getQueryCleanupDays());
    }

    @Test
    public void testDeleteAsyncQuery() {
        cleanerThread.deleteAsyncQuery();

        verify(asyncQueryDao, times(1)).deleteAsyncQueryAndResultCollection(anyString());
    }

    @Test
    public void timeoutAsyncQuery() {
        cleanerThread.timeoutAsyncQuery();

        verify(asyncQueryDao, times(1)).updateStatusAsyncQueryCollection(anyString(), any(QueryStatus.class));
    }
}
