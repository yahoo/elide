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
import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.async.models.AsyncQuery;
import com.yahoo.elide.async.models.QueryStatus;
import com.yahoo.elide.async.service.dao.AsyncApiDao;
import com.yahoo.elide.async.service.dao.DefaultAsyncApiDao;
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

    private AsyncApiCleanerRunnable cleanerThread;
    private Elide elide;
    private AsyncApiDao asyncAPIDao;
    private Clock clock = Clock.fixed(Instant.ofEpochMilli(0), ZoneId.of("Z"));

    @BeforeEach
    public void setupMocks() {
        HashMapDataStore inMemoryStore = new HashMapDataStore(new DefaultClassScanner(),
                AsyncQuery.class.getPackage());
        Map<String, Class<? extends Check>> checkMappings = new HashMap<>();

        elide = new Elide(
                ElideSettings.builder().dataStore(inMemoryStore)
                        .entityDictionary(EntityDictionary.builder().checks(checkMappings).build())
                        .serdes(serdes -> serdes.withISO8601Dates("yyyy-MM-dd'T'HH:mm'Z'", TimeZone.getTimeZone("UTC")))
                        .build());
        asyncAPIDao = mock(DefaultAsyncApiDao.class);
        cleanerThread = new AsyncApiCleanerRunnable(Duration.ofMinutes(7), elide, Duration.ofDays(7), asyncAPIDao, clock);
    }

    @Test
    void testAsyncQueryCleanerThreadSet() {
        assertEquals(elide, cleanerThread.getElide());
        assertEquals(asyncAPIDao, cleanerThread.getAsyncApiDao());
        assertEquals(7, cleanerThread.getQueryMaxRunTime().toMinutes());
        assertEquals(7, cleanerThread.getQueryRetentionDuration().toDays());
    }

    @Test
    void testDeleteAsyncQuery() {
        Date testDate = Date.from(Instant.now(clock).plus(Duration.ofDays(7)));
        ArgumentCaptor<FilterExpression> filterCaptor = ArgumentCaptor.forClass(FilterExpression.class);
        cleanerThread.deleteAsyncApi(AsyncQuery.class);
        verify(asyncAPIDao, times(1)).deleteAsyncApiAndResultByFilter(filterCaptor.capture(), any());
        assertEquals("asyncQuery.createdOn LE [" + testDate + "]", filterCaptor.getValue().toString());
    }

    @Test
    void testTimeoutAsyncQuery() {
        Date testDate = Date.from(Instant.now(clock).plus(Duration.ofMinutes(7)));
        ArgumentCaptor<FilterExpression> filterCaptor = ArgumentCaptor.forClass(FilterExpression.class);
        cleanerThread.timeoutAsyncApi(AsyncQuery.class);
        verify(asyncAPIDao, times(1)).updateStatusAsyncApiByFilter(filterCaptor.capture(), any(QueryStatus.class), any());
        assertEquals("(asyncQuery.status IN [PROCESSING, QUEUED] AND asyncQuery.createdOn LE [" + testDate + "])", filterCaptor.getValue().toString());
    }
}
