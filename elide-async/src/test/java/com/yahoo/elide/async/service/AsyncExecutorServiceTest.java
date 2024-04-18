/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.async.service;

import static com.paiondata.elide.core.dictionary.EntityDictionary.NO_VERSION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.paiondata.elide.Elide;
import com.paiondata.elide.ElideSettings;
import com.paiondata.elide.async.models.AsyncApiResult;
import com.paiondata.elide.async.models.AsyncQuery;
import com.paiondata.elide.async.models.QueryStatus;
import com.paiondata.elide.async.models.QueryType;
import com.paiondata.elide.async.operation.JsonApiAsyncQueryOperation;
import com.paiondata.elide.async.service.dao.AsyncApiDao;
import com.paiondata.elide.async.service.dao.DefaultAsyncApiDao;
import com.paiondata.elide.async.service.storageengine.FileResultStorageEngine;
import com.paiondata.elide.async.service.storageengine.ResultStorageEngine;
import com.paiondata.elide.core.RequestScope;
import com.paiondata.elide.core.datastore.inmemory.HashMapDataStore;
import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.request.route.Route;
import com.paiondata.elide.core.security.User;
import com.paiondata.elide.core.security.checks.Check;
import com.paiondata.elide.core.utils.DefaultClassScanner;
import com.paiondata.elide.jsonapi.JsonApiSettings;

import org.apache.hc.core5.http.NoHttpResponseException;
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
                ElideSettings.builder().dataStore(inMemoryStore)
                        .entityDictionary(EntityDictionary.builder().checks(checkMappings).build())
                        .settings(JsonApiSettings.builder())
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
