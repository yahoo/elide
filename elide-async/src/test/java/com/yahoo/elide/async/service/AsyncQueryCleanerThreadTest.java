/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.async.models.AsyncQuery;
import com.yahoo.elide.async.models.QueryStatus;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.datastore.inmemory.HashMapDataStore;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.security.checks.Check;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

public class AsyncQueryCleanerThreadTest {

    private AsyncQueryCleanerThread cleanerThread;
    private Elide elide;
    private AsyncQueryDAO asyncQueryDao;
    private ResultStorageEngine resultStorageEngine;

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
        resultStorageEngine = mock(DefaultResultStorageEngine.class);
        cleanerThread = new AsyncQueryCleanerThread(7, elide, 7, asyncQueryDao,
                resultStorageEngine);
    }

    @Test
    public void testAsyncQueryCleanerThreadSet() {
        assertEquals(elide, cleanerThread.getElide());
        assertEquals(asyncQueryDao, cleanerThread.getAsyncQueryDao());
        assertEquals(7, cleanerThread.getMaxRunTimeMinutes());
        assertEquals(7, cleanerThread.getQueryCleanupDays());
        assertEquals(resultStorageEngine, cleanerThread.getResultStorageEngine());
    }

    @Test
    public void testDeleteAsyncQuery() {
        ArgumentCaptor<FilterExpression> filterCaptor = ArgumentCaptor.forClass(FilterExpression.class);
        cleanerThread.deleteAsyncQuery();

        verify(asyncQueryDao, times(1)).deleteAsyncQueryAndResultCollection(filterCaptor.capture());
        Long date = System.currentTimeMillis() - (cleanerThread.getQueryCleanupDays() * 24 * 3600 * 1000);
        assertEquals("asyncQuery.createdOn LE [" + new Date(date) + "]", filterCaptor.getValue().toString());

        verify(resultStorageEngine, times(1)).deleteResultsCollection(any());
    }

    @Test
    public void testTimeoutAsyncQuery() {
        ArgumentCaptor<FilterExpression> filterCaptor = ArgumentCaptor.forClass(FilterExpression.class);
        cleanerThread.timeoutAsyncQuery();
        verify(asyncQueryDao, times(1)).updateStatusAsyncQueryCollection(filterCaptor.capture(), any(QueryStatus.class));
        Long date = System.currentTimeMillis() - (cleanerThread.getMaxRunTimeMinutes() * 60 * 1000);
        assertEquals("(asyncQuery.status IN [[PROCESSING, QUEUED]] AND asyncQuery.createdOn LE [" + new Date(date) + "])", filterCaptor.getValue().toString());
    }
}
