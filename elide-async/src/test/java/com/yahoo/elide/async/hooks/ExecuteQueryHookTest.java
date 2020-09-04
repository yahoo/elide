/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.hooks;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yahoo.elide.async.models.AsyncQuery;
import com.yahoo.elide.async.models.QueryType;
import com.yahoo.elide.async.models.ResultFormatType;
import com.yahoo.elide.async.models.ResultType;
import com.yahoo.elide.async.service.AsyncExecutorService;
import com.yahoo.elide.async.service.ResultStorageEngine;
import com.yahoo.elide.core.exceptions.InvalidValueException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ExecuteQueryHookTest {

    private AsyncExecutorService asyncExecutorService;
    private ResultStorageEngine resultStorageEngine;

    @BeforeEach
    public void setupMocks() {
        asyncExecutorService = mock(AsyncExecutorService.class);
        resultStorageEngine = mock(ResultStorageEngine.class);
    }

    @Test
    public void testWithMutationQuery() {
        AsyncQuery queryObj = new AsyncQuery();
        String query = "{ \"query\": \"mutation {table(op: UPSERT ... \", \"variables\":null}";
        String id = "edc4a871-dff2-4054-804e-d80075cf827d";
        queryObj.setId(id);
        queryObj.setQuery(query);
        queryObj.setQueryType(QueryType.GRAPHQL_V1_0);
        queryObj.setResultType(ResultType.DOWNLOAD);
        queryObj.setResultFormatType(ResultFormatType.CSV);

        when(asyncExecutorService.getResultStorageEngine()).thenReturn(resultStorageEngine);

        ExecuteQueryHook queryHook = new ExecuteQueryHook(asyncExecutorService);
        assertThrows(InvalidValueException.class, () -> {
            queryHook.validateOptions(queryObj, resultStorageEngine);
        });
    }

    @Test
    public void testWithResultFormatTypeNull() {
        AsyncQuery queryObj = new AsyncQuery();
        String query = "{ \"query\":\"{ group { edges { node { name commonName } } } }\",\"variables\":null}";
        String id = "edc4a871-dff2-4054-804e-d80075cf827d";
        queryObj.setId(id);
        queryObj.setQuery(query);
        queryObj.setQueryType(QueryType.GRAPHQL_V1_0);
        queryObj.setResultType(ResultType.DOWNLOAD);

        when(asyncExecutorService.getResultStorageEngine()).thenReturn(resultStorageEngine);

        ExecuteQueryHook queryHook = new ExecuteQueryHook(asyncExecutorService);
        assertThrows(InvalidValueException.class, () -> {
            queryHook.validateOptions(queryObj, resultStorageEngine);
        });
    }

    @Test
    public void testWithResultTypeNull() {
        AsyncQuery queryObj = new AsyncQuery();
        String query = "{ \"query\":\"{ group { edges { node { name commonName } } } }\",\"variables\":null}";
        String id = "edc4a871-dff2-4054-804e-d80075cf827d";
        queryObj.setId(id);
        queryObj.setQuery(query);
        queryObj.setQueryType(QueryType.GRAPHQL_V1_0);
        queryObj.setResultFormatType(ResultFormatType.CSV);

        when(asyncExecutorService.getResultStorageEngine()).thenReturn(resultStorageEngine);

        ExecuteQueryHook queryHook = new ExecuteQueryHook(asyncExecutorService);
        assertThrows(InvalidValueException.class, () -> {
            queryHook.validateOptions(queryObj, resultStorageEngine);
        });
    }

    @Test
    public void testWithStorageEngineNull() {
        AsyncQuery queryObj = new AsyncQuery();
        String query = "{ \"query\":\"{ group { edges { node { name commonName } } } }\",\"variables\":null}";
        String id = "edc4a871-dff2-4054-804e-d80075cf827d";
        queryObj.setId(id);
        queryObj.setQuery(query);
        queryObj.setQueryType(QueryType.GRAPHQL_V1_0);
        queryObj.setResultType(ResultType.DOWNLOAD);
        queryObj.setResultFormatType(ResultFormatType.CSV);

        when(asyncExecutorService.getResultStorageEngine()).thenReturn(resultStorageEngine);

        ExecuteQueryHook queryHook = new ExecuteQueryHook(asyncExecutorService);
        assertThrows(InvalidValueException.class, () -> {
            queryHook.validateOptions(queryObj, null);
        });
    }

    @Test
    public void testWithDownloadJSONAPI() {
        AsyncQuery queryObj = new AsyncQuery();
        String query = "{\"query\":\"{ group { edges { node { name commonName } } } }\",\"variables\":null}";
        String id = "edc4a871-dff2-4054-804e-d80075cf827d";
        queryObj.setId(id);
        queryObj.setQuery(query);
        queryObj.setQueryType(QueryType.GRAPHQL_V1_0);
        queryObj.setResultType(ResultType.DOWNLOAD);
        queryObj.setResultFormatType(ResultFormatType.GRAPHQLAPI);

        when(asyncExecutorService.getResultStorageEngine()).thenReturn(resultStorageEngine);

        ExecuteQueryHook queryHook = new ExecuteQueryHook(asyncExecutorService);
        assertThrows(InvalidValueException.class, () -> {
            queryHook.validateOptions(queryObj, resultStorageEngine);
        });
    }

    @Test
    public void testWithGraphqlDownloadValid() {
        AsyncQuery queryObj = new AsyncQuery();
        String query = "{ \"query\":\"{ group { edges { node { name commonName } } } }\",\"variables\":null}";
        String id = "edc4a871-dff2-4054-804e-d80075cf827d";
        queryObj.setId(id);
        queryObj.setQuery(query);
        queryObj.setQueryType(QueryType.GRAPHQL_V1_0);
        queryObj.setResultType(ResultType.DOWNLOAD);
        queryObj.setResultFormatType(ResultFormatType.CSV);

        when(asyncExecutorService.getResultStorageEngine()).thenReturn(resultStorageEngine);

        ExecuteQueryHook queryHook = new ExecuteQueryHook(asyncExecutorService);
        assertDoesNotThrow(() -> queryHook.validateOptions(queryObj, resultStorageEngine));
    }

    @Test
    public void testWithJsonDownloadValid() {
        AsyncQuery queryObj = new AsyncQuery();
        String query = "/group?sort=commonName&fields%5Bgroup%5D=commonName,description";
        String id = "edc4a871-dff2-4054-804e-d80075cf827d";
        queryObj.setId(id);
        queryObj.setQuery(query);
        queryObj.setQueryType(QueryType.JSONAPI_V1_0);
        queryObj.setResultType(ResultType.DOWNLOAD);
        queryObj.setResultFormatType(ResultFormatType.CSV);

        when(asyncExecutorService.getResultStorageEngine()).thenReturn(resultStorageEngine);

        ExecuteQueryHook queryHook = new ExecuteQueryHook(asyncExecutorService);
        assertDoesNotThrow(() -> queryHook.validateOptions(queryObj, resultStorageEngine));
    }

    @Test
    public void testWithJsonEmbeddedValid() {
        AsyncQuery queryObj = new AsyncQuery();
        String query = "/group?sort=commonName&fields%5Bgroup%5D=commonName,description";
        String id = "edc4a871-dff2-4054-804e-d80075cf827d";
        queryObj.setId(id);
        queryObj.setQuery(query);
        queryObj.setQueryType(QueryType.JSONAPI_V1_0);
        queryObj.setResultType(ResultType.EMBEDDED);
        queryObj.setResultFormatType(ResultFormatType.CSV);

        when(asyncExecutorService.getResultStorageEngine()).thenReturn(resultStorageEngine);

        ExecuteQueryHook queryHook = new ExecuteQueryHook(asyncExecutorService);
        assertDoesNotThrow(() -> queryHook.validateOptions(queryObj, resultStorageEngine));
    }
}
