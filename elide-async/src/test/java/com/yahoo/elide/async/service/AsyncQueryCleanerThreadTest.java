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
import com.yahoo.elide.async.models.QueryStatus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AsyncQueryCleanerThreadTest {

    private AsyncQueryCleanerThread cleanerThread;
    private Elide elide;
    private AsyncQueryDAO asyncQueryDao;

    @BeforeEach
    public void setupMocks() {
        elide = mock(Elide.class);
        asyncQueryDao = mock(DefaultAsyncQueryDAO.class);
        cleanerThread = new AsyncQueryCleanerThread(7, elide, 7, asyncQueryDao);
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
