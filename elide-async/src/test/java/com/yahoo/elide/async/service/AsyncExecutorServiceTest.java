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
import com.yahoo.elide.async.models.AsyncQueryResult;
import com.yahoo.elide.async.models.QueryStatus;
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
import java.util.concurrent.TimeoutException;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AsyncExecutorServiceTest {

    private AsyncExecutorService service;
    private Elide elide;
    private AsyncQueryDAO asyncQueryDao;
    private AsyncQuery queryObj;
    private AsyncQueryResult queryResultObj;
    private User testUser;
    private AsyncQueryUpdateThread asyncQueryUpdateThread;

    @BeforeAll
    public void setupMockElide() {
        HashMapDataStore inMemoryStore = new HashMapDataStore(AsyncQuery.class.getPackage());
        Map<String, Class<? extends Check>> checkMappings = new HashMap<>();

        elide = new Elide(
                new ElideSettingsBuilder(inMemoryStore)
                        .withEntityDictionary(new EntityDictionary(checkMappings))
                        .build());
        asyncQueryDao = mock(DefaultAsyncQueryDAO.class);
        queryObj = mock(AsyncQuery.class);
        testUser = mock(User.class);
        AsyncExecutorService.init(elide, 5, 60, asyncQueryDao);
        service = AsyncExecutorService.getInstance();
        asyncQueryUpdateThread = mock(AsyncQueryUpdateThread.class);

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

    //Test for regular query execution before asyncAfterSeconds expires
    @Test
    public void testExecuteQuery() throws ExecutionException, TimeoutException, InterruptedException {

       when(queryObj.getAsyncAfterSeconds()).thenReturn(10);
       service.executeQuery(queryObj, testUser, NO_VERSION);
       verify(queryObj, times(1)).setStatus(QueryStatus.PROCESSING);
       verify(queryObj, times(1)).setStatus(QueryStatus.COMPLETE);

    }

    //Test for TimeoutException after asyncAfterSeconds
    @Test
    public void testAsyncAfterExecuteQuery() throws InterruptedException {

        AsyncQuery query = new AsyncQuery();
        query.setAsyncAfterSeconds(0);
        String id = "edc4a871-dff2-4054-804e-d80075cf827d";
        query.setId(id);
        User user = new User(null);
        service.executeQuery(query, user, NO_VERSION);
        service.completeQuery(query, user, NO_VERSION);
        //verify(asyncQueryDao, times(1)).updateAsyncQueryResult(queryResultObj, query.getId());
    }
}
