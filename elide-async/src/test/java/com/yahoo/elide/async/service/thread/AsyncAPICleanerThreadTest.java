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
import static org.mockito.Mockito.when;
import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.async.models.AsyncQuery;
import com.yahoo.elide.async.models.QueryStatus;
import com.yahoo.elide.async.service.DateUtil;
import com.yahoo.elide.async.service.dao.AsyncAPIDAO;
import com.yahoo.elide.async.service.dao.DefaultAsyncAPIDAO;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.datastore.inmemory.HashMapDataStore;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.security.checks.Check;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

public class AsyncAPICleanerThreadTest {

    private AsyncAPICleanerThread cleanerThread;
    private Elide elide;
    private AsyncAPIDAO asyncAPIDao;
    private DateUtil dateUtil;
    private Date testDate = new Date(1577883661000L);

    @BeforeEach
    public void setupMocks() {
        HashMapDataStore inMemoryStore = new HashMapDataStore(AsyncQuery.class.getPackage());
        Map<String, Class<? extends Check>> checkMappings = new HashMap<>();

        elide = new Elide(
                new ElideSettingsBuilder(inMemoryStore)
                        .withEntityDictionary(new EntityDictionary(checkMappings))
                        .withISO8601Dates("yyyy-MM-dd'T'HH:mm'Z'", TimeZone.getTimeZone("UTC"))
                        .build());
        asyncAPIDao = mock(DefaultAsyncAPIDAO.class);
        dateUtil = mock(DateUtil.class);
        cleanerThread = new AsyncAPICleanerThread(7, elide, 7, asyncAPIDao, dateUtil);
    }

    @Test
    public void testAsyncQueryCleanerThreadSet() {
        assertEquals(elide, cleanerThread.getElide());
        assertEquals(asyncAPIDao, cleanerThread.getAsyncAPIDao());
        assertEquals(7, cleanerThread.getMaxRunTimeMinutes());
        assertEquals(7, cleanerThread.getQueryCleanupDays());
    }

    @Test
    public void testDeleteAsyncQuery() {
        when(dateUtil.calculateFilterDate(Calendar.DATE, cleanerThread.getQueryCleanupDays())).thenReturn(testDate);
        ArgumentCaptor<FilterExpression> filterCaptor = ArgumentCaptor.forClass(FilterExpression.class);
        cleanerThread.deleteAsyncAPI(AsyncQuery.class);
        verify(asyncAPIDao, times(1)).deleteAsyncAPIAndResultCollection(filterCaptor.capture(), any());
        assertEquals("asyncQuery.createdOn LE [" + testDate + "]", filterCaptor.getValue().toString());
    }

    @Test
    public void testTimeoutAsyncQuery() {
        when(dateUtil.calculateFilterDate(Calendar.MINUTE, cleanerThread.getMaxRunTimeMinutes())).thenReturn(testDate);
        ArgumentCaptor<FilterExpression> filterCaptor = ArgumentCaptor.forClass(FilterExpression.class);
        cleanerThread.timeoutAsyncAPI(AsyncQuery.class);
        verify(asyncAPIDao, times(1)).updateStatusAsyncAPICollection(filterCaptor.capture(), any(QueryStatus.class), any());
        assertEquals("(asyncQuery.status IN [[PROCESSING, QUEUED]] AND asyncQuery.createdOn LE [" + testDate + "])", filterCaptor.getValue().toString());
    }
}
