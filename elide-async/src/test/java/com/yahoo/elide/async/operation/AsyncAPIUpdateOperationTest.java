/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.operation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yahoo.elide.Elide;
import com.yahoo.elide.async.models.AsyncAPI;
import com.yahoo.elide.async.models.AsyncAPIResult;
import com.yahoo.elide.async.service.dao.AsyncAPIDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Future;

public class AsyncAPIUpdateOperationTest {

    private AsyncAPIUpdateOperation updateThread;
    private Elide elide;
    private AsyncAPI queryObj;
    private AsyncAPIResult queryResultObj;
    private AsyncAPIDAO asyncAPIDao;
    private Future<AsyncAPIResult> task;

    @BeforeEach
    public void setupMocks() {
        elide = mock(Elide.class);
        queryObj = mock(AsyncAPI.class);
        queryResultObj = mock(AsyncAPIResult.class);
        updateThread = new AsyncAPIUpdateOperation(elide, task, queryObj, asyncAPIDao);
    }

    @Test
    public void testAsyncAPIUpdateRunnableSet() {
        assertEquals(elide, updateThread.getElide());
        assertEquals(task, updateThread.getTask());
        assertEquals(queryObj, updateThread.getQueryObj());
        assertEquals(asyncAPIDao, updateThread.getAsyncAPIDao());
    }

    public void testUpdateQuery() {
        String id = "edc4a871-dff2-4054-804e-d80075cf827d";
        when(queryObj.getId()).thenReturn(id);
        updateThread.run();
        verify(asyncAPIDao, times(1)).updateAsyncAPIResult(queryResultObj, queryObj.getId(), queryObj.getClass());

    }
}
