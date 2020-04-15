/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.yahoo.elide.Elide;
import com.yahoo.elide.async.models.AsyncQuery;
import com.yahoo.elide.async.models.QueryStatus;

public class AsyncQueryInterruptThreadTest {

    private AsyncQueryInterruptThread interruptThread;
    private Elide elide;
    private Future<?> task;
    private AsyncQuery asyncQuery;
    private AsyncQueryDAO asyncQueryDao;

    @BeforeEach
    public void setupMocks() {
        elide = mock(Elide.class);
        task = mock(Future.class);
        asyncQuery = mock(AsyncQuery.class);
        Date submittedOn = new Date();
        asyncQueryDao = mock(DefaultAsyncQueryDAO.class);
        interruptThread = new AsyncQueryInterruptThread(elide, task, asyncQuery, submittedOn, 10, asyncQueryDao);
    }

    @Test
    public void testAsyncQueryCleanerThreadSet() {
        assertEquals(elide, interruptThread.getElide());
        assertEquals(task, interruptThread.getTask());
        assertEquals(asyncQuery, interruptThread.getAsyncQuery());
        assertEquals(10, interruptThread.getMaxRunTimeMinutes());
        assertEquals(asyncQueryDao, interruptThread.getAsyncQueryDao());
    }

    @Test
    public void testInterruptQueryInterruptedException() throws InterruptedException, ExecutionException, TimeoutException {
        when(task.get(anyLong(), any(TimeUnit.class))).thenThrow(InterruptedException.class);

        interruptThread.interruptQuery();

        verify(asyncQueryDao, times(0)).updateStatus(asyncQuery, QueryStatus.TIMEDOUT);
    }

    @Test
    public void testInterruptQueryExecutionException() throws InterruptedException, ExecutionException, TimeoutException {
        when(task.get(anyLong(), any(TimeUnit.class))).thenThrow(ExecutionException.class);

        interruptThread.interruptQuery();

        verify(asyncQueryDao, times(0)).updateStatus(asyncQuery, QueryStatus.TIMEDOUT);
    }

    @Test
    public void testInterruptQueryTimeoutException() throws InterruptedException, ExecutionException, TimeoutException {
        when(task.get(anyLong(), any(TimeUnit.class))).thenThrow(TimeoutException.class);

        interruptThread.interruptQuery();

        verify(task, times(1)).cancel(true);
        verify(asyncQueryDao, times(1)).updateStatus(asyncQuery, QueryStatus.TIMEDOUT);
    }
}
