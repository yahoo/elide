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
import com.yahoo.elide.async.models.AsyncApi;
import com.yahoo.elide.async.models.AsyncApiResult;
import com.yahoo.elide.async.service.dao.AsyncApiDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Future;

public class AsyncApiUpdateOperationTest {

    private AsyncApiUpdateOperation updateThread;
    private Elide elide;
    private AsyncApi queryObj;
    private AsyncApiResult queryResultObj;
    private AsyncApiDao asyncAPIDao;
    private Future<AsyncApiResult> task;

    @BeforeEach
    public void setupMocks() {
        elide = mock(Elide.class);
        queryObj = mock(AsyncApi.class);
        queryResultObj = mock(AsyncApiResult.class);
        updateThread = new AsyncApiUpdateOperation(elide, task, queryObj, asyncAPIDao);
    }

    @Test
    public void testAsyncAPIUpdateRunnableSet() {
        assertEquals(elide, updateThread.getElide());
        assertEquals(task, updateThread.getTask());
        assertEquals(queryObj, updateThread.getQueryObj());
        assertEquals(asyncAPIDao, updateThread.getAsyncApiDao());
    }

    public void testUpdateQuery() {
        String id = "edc4a871-dff2-4054-804e-d80075cf827d";
        when(queryObj.getId()).thenReturn(id);
        updateThread.run();
        verify(asyncAPIDao, times(1)).updateAsyncApiResult(queryResultObj, queryObj.getId(), queryObj.getClass());

    }
}
