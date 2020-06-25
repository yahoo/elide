/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yahoo.elide.Elide;
import com.yahoo.elide.async.models.AsyncQuery;
import com.yahoo.elide.async.models.AsyncQueryResult;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Future;

public class AsyncQueryUpdateThreadTest {

    private AsyncQueryUpdateThread updateThread;
    private Elide elide;
    private AsyncQuery queryObj;
    private AsyncQueryResult queryResultObj;
    private AsyncQueryDAO asyncQueryDao;
    private Future<AsyncQueryResult> task;

    @BeforeEach
    public void setupMocks() {
        elide = mock(Elide.class);
        queryObj = mock(AsyncQuery.class);
        queryResultObj = mock(AsyncQueryResult.class);
        updateThread = new AsyncQueryUpdateThread(elide, task, queryObj, asyncQueryDao);
    }

    @Test
    public void testAsyncQueryUpdateThreadSet() {
        assertEquals(elide, updateThread.getElide());
        assertEquals(task, updateThread.getTask());
        assertEquals(queryObj, updateThread.getQueryObj());
        assertEquals(asyncQueryDao, updateThread.getAsyncQueryDao());
    }

    public void testUpdateQuery() {
        String id = "edc4a871-dff2-4054-804e-d80075cf827d";
        when(queryObj.getId()).thenReturn(id);
        updateThread.updateQuery();
        verify(asyncQueryDao, times(1)).updateAsyncQueryResult(queryResultObj, queryObj.getId());

    }
}
