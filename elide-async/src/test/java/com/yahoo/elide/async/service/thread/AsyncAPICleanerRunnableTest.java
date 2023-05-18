/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.service.thread;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.async.models.AsyncQuery;
import com.yahoo.elide.async.models.QueryStatus;
import com.yahoo.elide.async.service.dao.AsyncAPIDAO;
import com.yahoo.elide.async.service.dao.DefaultAsyncAPIDAO;
import com.yahoo.elide.core.datastore.inmemory.HashMapDataStore;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.security.checks.Check;
import com.yahoo.elide.core.utils.DefaultClassScanner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

class AsyncAPICleanerRunnableTest {

    private AsyncAPICleanerRunnable cleanerThread;
    private Elide elide;
    private AsyncAPIDAO asyncAPIDao;
    private Clock clock = Clock.fixed(Instant.ofEpochMilli(0), ZoneId.of("Z"));

    @BeforeEach
    public void setupMocks() {
        HashMapDataStore inMemoryStore = new HashMapDataStore(DefaultClassScanner.getInstance(),
                AsyncQuery.class.getPackage());
        Map<String, Class<? extends Check>> checkMappings = new HashMap<>();

        elide = new Elide(
                new ElideSettingsBuilder(inMemoryStore)
                        .withEntityDictionary(EntityDictionary.builder().checks(checkMappings).build())
                        .withISO8601Dates("yyyy-MM-dd'T'HH:mm'Z'", TimeZone.getTimeZone("UTC"))
                        .build());
        asyncAPIDao = mock(DefaultAsyncAPIDAO.class);
        cleanerThread = new AsyncAPICleanerRunnable(Duration.ofMinutes(7), elide, Duration.ofDays(7), asyncAPIDao, clock);
    }

    @Test
    void testAsyncQueryCleanerThreadSet() {
        assertEquals(elide, cleanerThread.getElide());
        assertEquals(asyncAPIDao, cleanerThread.getAsyncAPIDao());
        assertEquals(7, cleanerThread.getQueryMaxRunTime().toMinutes());
        assertEquals(7, cleanerThread.getQueryRetentionDuration().toDays());
    }

    @Test
    void testDeleteAsyncQuery() {
        Date testDate = Date.from(Instant.now(clock).plus(Duration.ofDays(7)));
        ArgumentCaptor<FilterExpression> filterCaptor = ArgumentCaptor.forClass(FilterExpression.class);
        cleanerThread.deleteAsyncAPI(AsyncQuery.class);
        verify(asyncAPIDao, times(1)).deleteAsyncAPIAndResultByFilter(filterCaptor.capture(), any());
        assertEquals("asyncQuery.createdOn LE [" + testDate + "]", filterCaptor.getValue().toString());
    }

    @Test
    void testTimeoutAsyncQuery() {
        Date testDate = Date.from(Instant.now(clock).plus(Duration.ofMinutes(7)));
        ArgumentCaptor<FilterExpression> filterCaptor = ArgumentCaptor.forClass(FilterExpression.class);
        cleanerThread.timeoutAsyncAPI(AsyncQuery.class);
        verify(asyncAPIDao, times(1)).updateStatusAsyncAPIByFilter(filterCaptor.capture(), any(QueryStatus.class), any());
        assertEquals("(asyncQuery.status IN [PROCESSING, QUEUED] AND asyncQuery.createdOn LE [" + testDate + "])", filterCaptor.getValue().toString());
    }
}
