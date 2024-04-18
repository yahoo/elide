/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.async.service.thread;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.paiondata.elide.Elide;
import com.paiondata.elide.ElideSettings;
import com.paiondata.elide.async.models.AsyncQuery;
import com.paiondata.elide.async.models.QueryStatus;
import com.paiondata.elide.async.service.dao.AsyncApiDao;
import com.paiondata.elide.async.service.dao.DefaultAsyncApiDao;
import com.paiondata.elide.core.datastore.inmemory.HashMapDataStore;
import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.filter.expression.FilterExpression;
import com.paiondata.elide.core.security.checks.Check;
import com.paiondata.elide.core.utils.DefaultClassScanner;
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

class AsyncApiCleanerRunnableTest {

    private AsyncApiCleanerRunnable cleanerThread;
    private Elide elide;
    private AsyncApiDao asyncApiDao;
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
        asyncApiDao = mock(DefaultAsyncApiDao.class);
        cleanerThread = new AsyncApiCleanerRunnable(Duration.ofMinutes(7), elide, Duration.ofDays(7), asyncApiDao, clock);
    }

    @Test
    void testAsyncQueryCleanerThreadSet() {
        assertEquals(elide, cleanerThread.getElide());
        assertEquals(asyncApiDao, cleanerThread.getAsyncApiDao());
        assertEquals(7, cleanerThread.getQueryMaxRunTime().toMinutes());
        assertEquals(7, cleanerThread.getQueryRetentionDuration().toDays());
    }

    @Test
    void testDeleteAsyncQuery() {
        Date testDate = Date.from(Instant.now(clock).plus(Duration.ofDays(7)));
        ArgumentCaptor<FilterExpression> filterCaptor = ArgumentCaptor.forClass(FilterExpression.class);
        cleanerThread.deleteAsyncApi(AsyncQuery.class);
        verify(asyncApiDao, times(1)).deleteAsyncApiAndResultByFilter(filterCaptor.capture(), any());
        assertEquals("asyncQuery.createdOn LE [" + testDate + "]", filterCaptor.getValue().toString());
    }

    @Test
    void testTimeoutAsyncQuery() {
        Date testDate = Date.from(Instant.now(clock).plus(Duration.ofMinutes(7)));
        ArgumentCaptor<FilterExpression> filterCaptor = ArgumentCaptor.forClass(FilterExpression.class);
        cleanerThread.timeoutAsyncApi(AsyncQuery.class);
        verify(asyncApiDao, times(1)).updateStatusAsyncApiByFilter(filterCaptor.capture(), any(QueryStatus.class), any());
        assertEquals("(asyncQuery.status IN [PROCESSING, QUEUED] AND asyncQuery.createdOn LE [" + testDate + "])", filterCaptor.getValue().toString());
    }
}
