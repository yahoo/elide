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

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.async.models.AsyncQuery;
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

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AsyncExecutorServiceTest {

    private AsyncExecutorService service;
    private Elide elide;
    private AsyncQueryDAO asyncQueryDao;

    @BeforeAll
    public void setupMocks() {
        HashMapDataStore inMemoryStore = new HashMapDataStore(AsyncQuery.class.getPackage());
        Map<String, Class<? extends Check>> checkMappings = new HashMap<>();

        elide = new Elide(
                new ElideSettingsBuilder(inMemoryStore)
                        .withEntityDictionary(new EntityDictionary(checkMappings))
                        .build());

        asyncQueryDao = mock(DefaultAsyncQueryDAO.class);

        AsyncExecutorService.init(elide, 5, 60, asyncQueryDao);

        service = AsyncExecutorService.getInstance();
    }

    @Test
    public void testAsyncExecutorServiceSet() {
        assertEquals(elide, service.getElide());
        assertNotNull(service.getRunners());
        assertEquals(60, service.getMaxRunTime());
        assertNotNull(service.getExecutor());
        assertNotNull(service.getInterruptor());
        assertEquals(asyncQueryDao, service.getAsyncQueryDao());
    }

    @Test
    public void testExecuteQuery() {
        AsyncQuery queryObj = mock(AsyncQuery.class);
        User testUser = mock(User.class);

        service.executeQuery(queryObj, testUser, NO_VERSION);

        verify(asyncQueryDao, times(0)).updateStatus(queryObj, QueryStatus.QUEUED);
    }
}
