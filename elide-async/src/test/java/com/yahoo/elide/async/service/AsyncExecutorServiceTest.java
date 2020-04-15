/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.async.models.AsyncQuery;
import com.yahoo.elide.async.models.AsyncQueryResult;
import com.yahoo.elide.async.models.QueryStatus;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.jsonapi.JsonApiMapper;
import com.yahoo.elide.security.User;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AsyncExecutorServiceTest {

    private AsyncExecutorService service;
    private Elide elide;
    private AsyncQueryDAO asyncQueryDao;

    @BeforeAll
    public void setupMocks() {
        elide = mock(Elide.class);
        asyncQueryDao = mock(DefaultAsyncQueryDAO.class);
        ElideSettings elideSettings = mock(ElideSettings.class);
        EntityDictionary dictionary = mock(EntityDictionary.class);
        JsonApiMapper mapper = mock(JsonApiMapper.class);
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        Set<Class<?>> boundclasses = new HashSet<Class<?>>();
        boundclasses.add(AsyncQuery.class);
        boundclasses.add(AsyncQueryResult.class);

        when(elide.getElideSettings()).thenReturn(elideSettings);
        when(elideSettings.getDictionary()).thenReturn(dictionary);
        when(dictionary.getBoundClasses()).thenReturn(boundclasses);
        when(elideSettings.getMapper()).thenReturn(mapper);
        when(mapper.getObjectMapper()).thenReturn(objectMapper);

        AsyncExecutorService.init(elide, 5, 60, asyncQueryDao);

        service = AsyncExecutorService.getInstance();
    }

    @Test
    public void testAsyncExecutorServiceSet() {
        assertEquals(elide, service.getElide());
        assertNotNull(service.getRunner());
        assertEquals(60, service.getMaxRunTime());
        assertNotNull(service.getExecutor());
        assertNotNull(service.getInterruptor());
        assertEquals(asyncQueryDao, service.getAsyncQueryDao());
    }

    @Test
    public void testExecuteQuery() {
        AsyncQuery queryObj = mock(AsyncQuery.class);
        User testUser = mock(User.class);

        service.executeQuery(queryObj, testUser);

        verify(asyncQueryDao, times(1)).updateStatus(queryObj, QueryStatus.QUEUED);
    }

}
