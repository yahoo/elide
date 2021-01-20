/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.service;

import static com.yahoo.elide.core.dictionary.EntityDictionary.NO_VERSION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.async.models.AsyncAPIResult;
import com.yahoo.elide.async.models.AsyncQuery;
import com.yahoo.elide.async.models.QueryStatus;
import com.yahoo.elide.async.models.QueryType;
import com.yahoo.elide.async.service.dao.AsyncAPIDAO;
import com.yahoo.elide.async.service.dao.DefaultAsyncAPIDAO;
import com.yahoo.elide.async.service.storageengine.FileResultStorageEngine;
import com.yahoo.elide.async.service.storageengine.ResultStorageEngine;
import com.yahoo.elide.async.service.thread.AsyncQueryCallable;
import com.yahoo.elide.core.datastore.inmemory.HashMapDataStore;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.security.User;
import com.yahoo.elide.core.security.checks.Check;
import org.apache.http.NoHttpResponseException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AsyncExecutorServiceTest {

    private AsyncExecutorService service;
    private Elide elide;
    private AsyncAPIDAO asyncAPIDao;
    private User testUser;
    private ResultStorageEngine resultStorageEngine;

    @BeforeAll
    public void setupMockElide() {
        HashMapDataStore inMemoryStore = new HashMapDataStore(AsyncQuery.class.getPackage());
        Map<String, Class<? extends Check>> checkMappings = new HashMap<>();
        elide = new Elide(
                new ElideSettingsBuilder(inMemoryStore)
                        .withEntityDictionary(new EntityDictionary(checkMappings))
                        .build());
        asyncAPIDao = mock(DefaultAsyncAPIDAO.class);
        testUser = mock(User.class);
        resultStorageEngine = mock(FileResultStorageEngine.class);
        AsyncExecutorService.init(elide, 5, asyncAPIDao, resultStorageEngine);
        service = AsyncExecutorService.getInstance();
    }

    @Test
    public void testAsyncExecutorServiceSet() {
        assertEquals(elide, service.getElide());
        assertNotNull(service.getRunners());
        assertNotNull(service.getExecutor());
        assertNotNull(service.getUpdater());
        assertEquals(asyncAPIDao, service.getAsyncAPIDao());
        assertEquals(resultStorageEngine, service.getResultStorageEngine());
    }

    //Test for executor hook execution
    @Test
    public void testExecuteQueryFail() throws Exception {
       AsyncQuery queryObj = mock(AsyncQuery.class);
       when(queryObj.getAsyncAfterSeconds()).thenReturn(10);

       Callable<AsyncAPIResult> mockCallable = mock(Callable.class);
       when(mockCallable.call()).thenThrow(new NoHttpResponseException(""));

       service.executeQuery(queryObj, mockCallable);
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
        AsyncQueryCallable queryThread = new AsyncQueryCallable(queryObj, testUser, service, NO_VERSION);
        service.executeQuery(queryObj, queryThread);
        verify(queryObj, times(1)).setStatus(QueryStatus.PROCESSING);
        verify(queryObj, times(1)).setStatus(QueryStatus.COMPLETE);
    }
}
