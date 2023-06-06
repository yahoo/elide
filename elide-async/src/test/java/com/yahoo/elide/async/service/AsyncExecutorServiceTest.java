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
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.async.models.AsyncApiResult;
import com.yahoo.elide.async.models.AsyncQuery;
import com.yahoo.elide.async.models.QueryStatus;
import com.yahoo.elide.async.models.QueryType;
import com.yahoo.elide.async.operation.JsonApiAsyncQueryOperation;
import com.yahoo.elide.async.service.dao.AsyncApiDao;
import com.yahoo.elide.async.service.dao.DefaultAsyncApiDao;
import com.yahoo.elide.async.service.storageengine.FileResultStorageEngine;
import com.yahoo.elide.async.service.storageengine.ResultStorageEngine;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.datastore.inmemory.HashMapDataStore;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.request.route.Route;
import com.yahoo.elide.core.security.User;
import com.yahoo.elide.core.security.checks.Check;
import com.yahoo.elide.core.utils.DefaultClassScanner;

import org.apache.http.NoHttpResponseException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import graphql.execution.DataFetcherExceptionHandler;
import graphql.execution.SimpleDataFetcherExceptionHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AsyncExecutorServiceTest {

    private AsyncExecutorService service;
    private Elide elide;
    private AsyncApiDao asyncApiDao;
    private User testUser;
    private RequestScope scope;
    private ResultStorageEngine resultStorageEngine;
    private DataFetcherExceptionHandler dataFetcherExceptionHandler = spy(new SimpleDataFetcherExceptionHandler());

    @BeforeEach
    void resetMocks() {
        reset(dataFetcherExceptionHandler);
    }

    @BeforeAll
    public void setupMockElide() {
        HashMapDataStore inMemoryStore = new HashMapDataStore(
                new DefaultClassScanner(),
                AsyncQuery.class.getPackage()
        );
        Map<String, Class<? extends Check>> checkMappings = new HashMap<>();
        elide = new Elide(
                new ElideSettingsBuilder(inMemoryStore)
                        .withEntityDictionary(EntityDictionary.builder().checks(checkMappings).build())
                        .build());
        asyncApiDao = mock(DefaultAsyncApiDao.class);
        testUser = mock(User.class);
        scope = mock(RequestScope.class);
        resultStorageEngine = mock(FileResultStorageEngine.class);
        service = new AsyncExecutorService(elide, Executors.newFixedThreadPool(5), Executors.newFixedThreadPool(5),
                        asyncApiDao, Optional.of(dataFetcherExceptionHandler));

    }

    @Test
    public void testAsyncExecutorServiceSet() {
        assertEquals(elide, service.getElide());
        assertNotNull(service.getRunners());
        assertNotNull(service.getExecutor());
        assertNotNull(service.getUpdater());
        assertEquals(asyncApiDao, service.getAsyncApiDao());
        assertEquals(resultStorageEngine, resultStorageEngine);
    }

    //Test for executor hook execution
    @Test
    public void testExecuteQueryFail() throws Exception {
       AsyncQuery queryObj = mock(AsyncQuery.class);
       when(queryObj.getAsyncAfterSeconds()).thenReturn(10);

       Callable<AsyncApiResult> mockCallable = mock(Callable.class);
       when(mockCallable.call()).thenThrow(new NoHttpResponseException(""));

       service.executeQuery(queryObj, mockCallable);
       verify(queryObj, times(1)).setStatus(QueryStatus.PROCESSING);
       verify(queryObj, times(1)).setStatus(QueryStatus.FAILURE);
    }

    //Test for executor hook execution
    @Test
    public void testExecuteQueryComplete() {

        AsyncQuery queryObj = mock(AsyncQuery.class);
        String query = "/group?sort=commonName&fields%5Bgroup%5D=commonName,description";
        String id = "edc4a871-dff2-4054-804e-d80075cf827d";
        when(queryObj.getQuery()).thenReturn(query);
        when(queryObj.getId()).thenReturn(id);
        when(queryObj.getRequestId()).thenReturn(id);
        when(queryObj.getQueryType()).thenReturn(QueryType.JSONAPI_V1_0);
        when(queryObj.getAsyncAfterSeconds()).thenReturn(10);
        when(scope.getRoute()).thenReturn(Route.builder().apiVersion(NO_VERSION).build());
        when(scope.getUser()).thenReturn(testUser);
        JsonApiAsyncQueryOperation jsonOperation = new JsonApiAsyncQueryOperation(service, queryObj, scope);
        service.executeQuery(queryObj, jsonOperation);
        verify(queryObj, times(1)).setStatus(QueryStatus.PROCESSING);
        verify(queryObj, times(1)).setStatus(QueryStatus.COMPLETE);
    }
}
