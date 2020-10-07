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
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.async.models.AsyncQuery;
import com.yahoo.elide.async.models.QueryStatus;
import com.yahoo.elide.async.models.QueryType;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.datastore.inmemory.HashMapDataStore;
import com.yahoo.elide.security.User;
import com.yahoo.elide.security.checks.Check;

import org.apache.http.NoHttpResponseException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AsyncExecutorServiceTest {

    private AsyncExecutorService service;
    private Elide elide;
    private AsyncQueryDAO asyncQueryDao;
    private User testUser;
    private AsyncAPIUpdateThread asyncAPIUpdateThread;
    private ResultStorageEngine resultStorageEngine;

    @BeforeAll
    public void setupMockElide() {
        HashMapDataStore inMemoryStore = new HashMapDataStore(AsyncQuery.class.getPackage());
        Map<String, Class<? extends Check>> checkMappings = new HashMap<>();
        elide = new Elide(
                new ElideSettingsBuilder(inMemoryStore)
                        .withEntityDictionary(new EntityDictionary(checkMappings))
                        .build());
        asyncQueryDao = mock(DefaultAsyncQueryDAO.class);
        testUser = mock(User.class);
        resultStorageEngine = mock(FileResultStorageEngine.class);
        AsyncExecutorService.init(elide, 5, 60, asyncQueryDao, resultStorageEngine);
        service = AsyncExecutorService.getInstance();
        asyncAPIUpdateThread = mock(AsyncAPIUpdateThread.class);
    }

    @Test
    public void testAsyncExecutorServiceSet() {
        assertEquals(elide, service.getElide());
        assertNotNull(service.getRunners());
        assertEquals(60, service.getMaxRunTime());
        assertNotNull(service.getExecutor());
        assertNotNull(service.getUpdater());
        assertEquals(asyncQueryDao, service.getAsyncQueryDao());
        assertEquals(resultStorageEngine, service.getResultStorageEngine());
    }

    //Test for executor hook execution
    @Test
    public void testExecuteQueryFail() throws ExecutionException, TimeoutException, InterruptedException,
           NoHttpResponseException, URISyntaxException {
       AsyncQuery queryObj = mock(AsyncQuery.class);
       when(queryObj.getAsyncAfterSeconds()).thenReturn(10);
       when(queryObj.executeRequest(service, testUser, NO_VERSION)).thenThrow(new NoHttpResponseException(null));
       service.executeQuery(queryObj, testUser, NO_VERSION);
       verify(queryObj, times(1)).setStatus(QueryStatus.PROCESSING);
       verify(queryObj, times(1)).setStatus(QueryStatus.FAILURE);
    }

    //Test for executor hook execution
    @Test
    public void testExecuteQueryComplete() throws InterruptedException {

        AsyncQuery queryObj = mock(AsyncQuery.class);
        String query = "/group?sort=commonName&fields%5Bgroup%5D=commonName,description";
        String id = "edc4a871-dff2-4054-804e-d80075cf827d";
        when(queryObj.getQuery()).thenReturn(query);
        when(queryObj.getId()).thenReturn(id);
        when(queryObj.getRequestId()).thenReturn(id);
        when(queryObj.getQueryType()).thenReturn(QueryType.JSONAPI_V1_0);
        when(queryObj.getAsyncAfterSeconds()).thenReturn(10);
        service.executeQuery(queryObj, testUser, NO_VERSION);
        verify(queryObj, times(1)).setStatus(QueryStatus.PROCESSING);
        verify(queryObj, times(1)).setStatus(QueryStatus.COMPLETE);
    }

    //Test for complete hook execution
    @Test
    public void testCompleteQuery() throws InterruptedException {

        AsyncQuery queryObj = mock(AsyncQuery.class);
        when(queryObj.getAsyncAfterSeconds()).thenReturn(0);
        when(queryObj.getQueryUpdateWorker()).thenReturn(asyncAPIUpdateThread);
        service.executeQuery(queryObj, testUser, NO_VERSION);
        service.completeQuery(queryObj, testUser, NO_VERSION);
        verify(queryObj, times(1)).setStatus(QueryStatus.PROCESSING);
        verify(queryObj, times(2)).getQueryUpdateWorker();
    }
}
