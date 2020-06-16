/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.service;

import static com.yahoo.elide.core.EntityDictionary.NO_VERSION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideResponse;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.async.models.AsyncQuery;
import com.yahoo.elide.async.models.AsyncQueryResult;
import com.yahoo.elide.async.models.QueryStatus;
import com.yahoo.elide.async.models.QueryType;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.datastore.inmemory.HashMapDataStore;
import com.yahoo.elide.security.User;
import com.yahoo.elide.security.checks.Check;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AsyncExecutorServiceTest {

    private AsyncExecutorService service;
    private Elide elide;
    private AsyncQueryDAO asyncQueryDao;
    private AsyncQuery queryObj1;
    private AsyncQuery queryObj2;
    private AsyncQuery queryObj3;
    private User testUser;
    private AsyncQueryUpdateThread asyncQueryUpdateThread;
    Future<AsyncQueryResult> task;
    @BeforeAll
    public void setupMockElide() {
        HashMapDataStore inMemoryStore = new HashMapDataStore(AsyncQuery.class.getPackage());
        Map<String, Class<? extends Check>> checkMappings = new HashMap<>();

        elide = new Elide(
                new ElideSettingsBuilder(inMemoryStore)
                        .withEntityDictionary(new EntityDictionary(checkMappings))
                        .build());
        asyncQueryDao = mock(DefaultAsyncQueryDAO.class);
        queryObj1 = mock(AsyncQuery.class);
        queryObj2 = mock(AsyncQuery.class);
        queryObj3 = mock(AsyncQuery.class);
        testUser = mock(User.class);
        AsyncExecutorService.init(elide, 5, 60, asyncQueryDao);
        service = AsyncExecutorService.getInstance();
        asyncQueryUpdateThread = mock(AsyncQueryUpdateThread.class);
        task = mock(Future.class);
    }

    @Test
    public void testAsyncExecutorServiceSet() {
        assertEquals(elide, service.getElide());
        assertNotNull(service.getRunners());
        assertEquals(60, service.getMaxRunTime());
        assertNotNull(service.getExecutor());
        assertNotNull(service.getUpdater());
        assertEquals(asyncQueryDao, service.getAsyncQueryDao());
    }

    //Test for executor hook execution
    @Test
    public void testExecuteQueryFail() throws ExecutionException, TimeoutException, InterruptedException {

       when(queryObj1.getAsyncAfterSeconds()).thenReturn(10);
       service.executeQuery(queryObj1, testUser, NO_VERSION);
       verify(queryObj1, times(1)).setStatus(QueryStatus.PROCESSING);
       verify(queryObj1, times(1)).setStatus(QueryStatus.FAILURE);
    }

    //Test for executor hook execution
    @Test
    public void testExecuteQueryComplete() throws InterruptedException {

        String query = "/group?sort=commonName&fields%5Bgroup%5D=commonName,description";
        String id = "edc4a871-dff2-4054-804e-d80075cf827d";
        ElideResponse response = mock(ElideResponse.class);
        when(queryObj2.getQuery()).thenReturn(query);
        when(queryObj2.getId()).thenReturn(id);
        when(queryObj2.getQueryType()).thenReturn(QueryType.JSONAPI_V1_0);
        when(response.getResponseCode()).thenReturn(200);
        when(response.getBody()).thenReturn("ResponseBody");
        when(queryObj2.getAsyncAfterSeconds()).thenReturn(10);
        service.executeQuery(queryObj2, testUser, NO_VERSION);
        verify(queryObj2, times(1)).setStatus(QueryStatus.PROCESSING);
        verify(queryObj2, times(1)).setStatus(QueryStatus.COMPLETE);
    }

    //Test for complete hook execution
    @Test
    public void testCompleteQuery() throws InterruptedException {

        when(queryObj3.getAsyncAfterSeconds()).thenReturn(0);
        when(queryObj3.getQueryUpdateWorker()).thenReturn(asyncQueryUpdateThread);
        service.executeQuery(queryObj3, testUser, NO_VERSION);
        service.completeQuery(queryObj3, testUser, NO_VERSION);
        verify(queryObj3, times(1)).setStatus(QueryStatus.PROCESSING);
        verify(queryObj3, times(2)).getQueryUpdateWorker();
    }
}
