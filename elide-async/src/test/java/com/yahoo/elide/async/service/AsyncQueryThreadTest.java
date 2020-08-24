/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideResponse;
import com.yahoo.elide.async.models.AsyncQuery;
import com.yahoo.elide.async.models.AsyncQueryResult;
import com.yahoo.elide.async.models.QueryType;
import com.yahoo.elide.async.models.ResultType;
import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.graphql.QueryRunner;
import com.yahoo.elide.security.User;

import com.jayway.jsonpath.InvalidJsonException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URL;

public class AsyncQueryThreadTest {
    private static final String BASE_URL_ENDPOINT = "http://localhost:8080/api/v1";
    private static final String DOWNLOAD_BASE_PATH = "/api/v1/download";

    private User user;
    private Elide elide;
    private QueryRunner runner;
    private AsyncQueryResult queryResultObj;
    private AsyncQueryDAO asyncQueryDao;
    private ResultStorageEngine resultStorageEngine;

    @BeforeEach
    public void setupMocks() {
        user = mock(User.class);
        elide = mock(Elide.class);
        runner = mock(QueryRunner.class);
        queryResultObj = mock(AsyncQueryResult.class);
        asyncQueryDao = mock(DefaultAsyncQueryDAO.class);
        resultStorageEngine = mock(DefaultResultStorageEngine.class);
    }

    //Test with record count = 3
    @Test
    public void testProcessQueryJsonApi() throws Exception {
        AsyncQuery queryObj = new AsyncQuery();
        String responseBody = "{\"data\":"
                + "[{\"type\":\"book\",\"id\":\"3\",\"attributes\":{\"title\":\"For Whom the Bell Tolls\"}}"
                + ",{\"type\":\"book\",\"id\":\"2\",\"attributes\":{\"title\":\"Song of Ice and Fire\"}},"
                + "{\"type\":\"book\",\"id\":\"1\",\"attributes\":{\"title\":\"Ender's Game\"}}]}";
        ElideResponse response = new ElideResponse(200, responseBody);
        String query = "/group?sort=commonName&fields%5Bgroup%5D=commonName,description";
        String id = "edc4a871-dff2-4054-804e-d80075cf827d";
        queryObj.setId(id);
        queryObj.setQuery(query);
        queryObj.setQueryType(QueryType.JSONAPI_V1_0);
        queryObj.setResultType(ResultType.EMBEDDED);

        when(elide.get(any(), anyString(), any(), any(), anyString(), any())).thenReturn(response);
        AsyncQueryThread queryThread = new AsyncQueryThread(queryObj, user, elide, runner, asyncQueryDao, "v1",
                resultStorageEngine, BASE_URL_ENDPOINT);
        queryResultObj = queryThread.processQuery();
        assertEquals(queryResultObj.getRecordCount(), 3);
        assertEquals(queryResultObj.getResponseBody(), responseBody);
        assertEquals(queryResultObj.getHttpStatus(), 200);
    }

    //Test with record count = 0
    @Test
    public void testProcessQueryJsonApi2() throws Exception {
        AsyncQuery queryObj = new AsyncQuery();
        String responseBody = "{\"data\":[]}";
        ElideResponse response = new ElideResponse(200, responseBody);
        String query = "/group?sort=commonName&fields%5Bgroup%5D=commonName,description";
        String id = "edc4a871-dff2-4054-804e-d80075cf827d";
        queryObj.setId(id);
        queryObj.setQuery(query);
        queryObj.setQueryType(QueryType.JSONAPI_V1_0);
        queryObj.setResultType(ResultType.EMBEDDED);

        when(elide.get(any(), anyString(), any(), any(), anyString(), any())).thenReturn(response);
        AsyncQueryThread queryThread = new AsyncQueryThread(queryObj, user, elide, runner, asyncQueryDao, "v1",
                resultStorageEngine, BASE_URL_ENDPOINT);
        queryResultObj = queryThread.processQuery();

        assertEquals(queryResultObj.getRecordCount(), 0);
        assertEquals(queryResultObj.getResponseBody(), responseBody);
        assertEquals(queryResultObj.getHttpStatus(), 200);
    }

    //Test with invalid input for responseBody. An exception will be thrown.
    @Test
    public void testProcessQueryJsonApi3() throws Exception {
        assertThrows(InvalidJsonException.class, () -> {
        AsyncQuery queryObj = new AsyncQuery();
        ElideResponse response = new ElideResponse(200, "ResponseBody");
        String query = "/group?sort=commonName&fields%5Bgroup%5D=commonName,description";
        String id = "edc4a871-dff2-4054-804e-d80075cf827d";
        queryObj.setId(id);
        queryObj.setQuery(query);
        queryObj.setQueryType(QueryType.JSONAPI_V1_0);
        queryObj.setResultType(ResultType.EMBEDDED);

        when(elide.get(any(), anyString(), any(), any(), anyString(), any())).thenReturn(response);
        AsyncQueryThread queryThread = new AsyncQueryThread(queryObj, user, elide, runner, asyncQueryDao, "v1",
                resultStorageEngine, BASE_URL_ENDPOINT);
        queryResultObj = queryThread.processQuery();

        assertEquals(queryResultObj.getRecordCount(), null);
        assertEquals(queryResultObj.getResponseBody(), "ResponseBody");
        });

    }

    //Test with invalid input for responseBody. An exception will be thrown.
    @Test
    public void testProcessQueryGraphQl() throws Exception {
        assertThrows(InvalidJsonException.class, () -> {
            AsyncQuery queryObj = new AsyncQuery();
            ElideResponse response = new ElideResponse(200, "ResponseBody");
            String query = "{\"query\":\"{ group { edges { node { name commonName description } } } }\",\"variables\":null}";
            String id = "edc4a871-dff2-4054-804e-d80075cf827d";
            queryObj.setId(id);
            queryObj.setQuery(query);
            queryObj.setQueryType(QueryType.GRAPHQL_V1_0);
            queryObj.setResultType(ResultType.EMBEDDED);

            when(runner.run(any(), eq(query), eq(user), any())).thenReturn(response);
            AsyncQueryThread queryThread = new AsyncQueryThread(queryObj, user, elide, runner, asyncQueryDao, "v1",
                    resultStorageEngine, BASE_URL_ENDPOINT);
            queryResultObj = queryThread.processQuery();
            assertEquals(queryResultObj.getResponseBody(), "ResponseBody");
            assertEquals(queryResultObj.getHttpStatus(), 200);
        });
    }

    //Test for when resultStorageEngine fails to store
    @Test
    public void testProcessQueryGraphQlStoreException() throws Exception {
        assertThrows(IllegalStateException.class, () -> {
            AsyncQuery queryObj = new AsyncQuery();
            String responseBody = "{\"data\":{\"book\":{\"edges\":[{\"node\":{\"id\":\"1\",\"title\":\"Ender's Game\"}},"
                    + "{\"node\":{\"id\":\"2\",\"title\":\"Song of Ice and Fire\"}},"
                    + "{\"node\":{\"id\":\"3\",\"title\":\"For Whom the Bell Tolls\"}}]}}}";
            ElideResponse response = new ElideResponse(200, responseBody);
            String query = "{\"query\":\"{ group { edges { node { name commonName description } } } }\",\"variables\":null}";
            String id = "edc4a871-dff2-4054-804e-d80075cf827d";
            queryObj.setId(id);
            queryObj.setQuery(query);
            queryObj.setQueryType(QueryType.GRAPHQL_V1_0);
            queryObj.setResultType(ResultType.DOWNLOAD);

            when(runner.run(any(), eq(query), eq(user), any())).thenReturn(response);
            when(resultStorageEngine.storeResults(any(), any(), any())).thenThrow(IllegalStateException.class);
            AsyncQueryThread queryThread = new AsyncQueryThread(queryObj, user, elide, runner, asyncQueryDao, "v1",
                    resultStorageEngine, BASE_URL_ENDPOINT);
            queryResultObj = queryThread.processQuery();
        });
    }

    //Test with record count = 3
    @Test
    public void testProcessQueryGraphQl2() throws Exception {

        AsyncQuery queryObj = new AsyncQuery();
        String responseBody = "{\"data\":{\"book\":{\"edges\":[{\"node\":{\"id\":\"1\",\"title\":\"Ender's Game\"}},"
                + "{\"node\":{\"id\":\"2\",\"title\":\"Song of Ice and Fire\"}},"
                + "{\"node\":{\"id\":\"3\",\"title\":\"For Whom the Bell Tolls\"}}]}}}";
        ElideResponse response = new ElideResponse(200, responseBody);
        String query = "{\"query\":\"{ group { edges { node { name commonName description } } } }\",\"variables\":null}";
        String id = "edc4a871-dff2-4054-804e-d80075cf827d";
        queryObj.setId(id);
        queryObj.setQuery(query);
        queryObj.setQueryType(QueryType.GRAPHQL_V1_0);
        queryObj.setResultType(ResultType.EMBEDDED);

        when(runner.run(any(), eq(query), eq(user), any())).thenReturn(response);
        AsyncQueryThread queryThread = new AsyncQueryThread(queryObj, user, elide, runner, asyncQueryDao, "v1",
                resultStorageEngine, BASE_URL_ENDPOINT);
        queryResultObj = queryThread.processQuery();
        assertEquals(queryResultObj.getRecordCount(), 3);
        assertEquals(queryResultObj.getResponseBody(), responseBody);
        assertEquals(queryResultObj.getHttpStatus(), 200);

    }

    //Test with record count = 0
    @Test
    public void testProcessQueryGraphQl3() throws Exception {

        AsyncQuery queryObj = new AsyncQuery();
        String responseBody = "{\"data\":[]}";
        ElideResponse response = new ElideResponse(200, responseBody);
        String query = "{\"query\":\"{ group { edges { node { name commonName description } } } }\",\"variables\":null}";
        String id = "edc4a871-dff2-4054-804e-d80075cf827d";
        queryObj.setId(id);
        queryObj.setQuery(query);
        queryObj.setQueryType(QueryType.GRAPHQL_V1_0);
        queryObj.setResultType(ResultType.EMBEDDED);

        when(runner.run(any(), eq(query), eq(user), any())).thenReturn(response);
        AsyncQueryThread queryThread = new AsyncQueryThread(queryObj, user, elide, runner, asyncQueryDao, "v1",
                resultStorageEngine, BASE_URL_ENDPOINT);
        queryResultObj = queryThread.processQuery();
        assertEquals(queryResultObj.getRecordCount(), 0);
        assertEquals(queryResultObj.getResponseBody(), responseBody);
        assertEquals(queryResultObj.getHttpStatus(), 200);

    }

    //Test with record count = 3 and Download
    @Test
    public void testProcessQueryGraphQlDownload() throws Exception {

        AsyncQuery queryObj = new AsyncQuery();
        String responseBody = "{\"data\":{\"book\":{\"edges\":[{\"node\":{\"id\":\"1\",\"title\":\"Ender's Game\"}},"
                + "{\"node\":{\"id\":\"2\",\"title\":\"Song of Ice and Fire\"}},"
                + "{\"node\":{\"id\":\"3\",\"title\":\"For Whom the Bell Tolls\"}}]}}}";
        ElideResponse response = new ElideResponse(200, responseBody);
        String query = "{\"query\":\"{ group { edges { node { name commonName description } } } }\",\"variables\":null}";
        String id = "edc4a871-dff2-4054-804e-d80075cf827d";
        queryObj.setId(id);
        queryObj.setQuery(query);
        queryObj.setQueryType(QueryType.GRAPHQL_V1_0);
        queryObj.setResultType(ResultType.DOWNLOAD);
        URL url = new URL(BASE_URL_ENDPOINT + "/download/" + id);

        when(runner.run(any(), eq(query), eq(user), any())).thenReturn(response);
        resultStorageEngine = new DefaultResultStorageEngine(DOWNLOAD_BASE_PATH, elide.getElideSettings(), mock(DataStore.class),
                asyncQueryDao);
        AsyncQueryThread queryThread = new AsyncQueryThread(queryObj, user, elide, runner, asyncQueryDao, "v1",
                resultStorageEngine, BASE_URL_ENDPOINT);
        queryResultObj = queryThread.processQuery();
        assertEquals(queryResultObj.getRecordCount(), 3);
        assertEquals(queryResultObj.getResponseBody(), url.toString());
        assertEquals(queryResultObj.getHttpStatus(), 200);

    }

    // Standard positive test case that converts json to csv format.
    @Test
    public void testConvertJsonToCSV() throws Exception {

        String csvStr = "key\n\"value\"\n";
        String jsonStr = "{\"key\":\"value\"}";

        AsyncQuery queryObj = new AsyncQuery();
        AsyncQueryThread queryThread = new AsyncQueryThread(queryObj, user, elide, runner, asyncQueryDao, "v1",
                resultStorageEngine, BASE_URL_ENDPOINT);

        assertEquals(queryThread.convertJsonToCSV(jsonStr), csvStr);
    }

    // Null json to csv format.
    @Test
    public void testConvertJsonToCSVNull() throws Exception {

        String jsonStr = null;

        AsyncQuery queryObj = new AsyncQuery();
        AsyncQueryThread queryThread = new AsyncQueryThread(queryObj, user, elide, runner, asyncQueryDao, "v1",
                resultStorageEngine, BASE_URL_ENDPOINT);

        assertNull(queryThread.convertJsonToCSV(jsonStr));
    }

    //Invalid input for the json to csv conversion. This throws an exception.
    @Test
    public void testConvertJsonToCSV2() throws Exception {

        String csvStr = "ResponseBody";
        assertThrows(IllegalStateException.class, () -> {
            String jsonStr = "ResponseBody";

            AsyncQuery queryObj = new AsyncQuery();
            AsyncQueryThread queryThread = new AsyncQueryThread(queryObj, user, elide, runner, asyncQueryDao, "v1",
                    resultStorageEngine, BASE_URL_ENDPOINT);

            assertEquals(queryThread.convertJsonToCSV(jsonStr), csvStr);
        });
    }

    @Test
    public void testCheckJsonStrErrorMessageValid() {
        AsyncQuery queryObj = new AsyncQuery();
        AsyncQueryThread queryThread = new AsyncQueryThread(queryObj, user, elide, runner, asyncQueryDao, "v1",
                resultStorageEngine, BASE_URL_ENDPOINT);
        String message = "{\"data\": [{\"type \": \"book \",\"id \": \"3 \",\"attributes \": {\"title \": \"For Whom the Bell Tolls \"}}]}";
        assertEquals(false, queryThread.checkJsonStrErrorMessage(message));
    }

    @Test
    public void testCheckJsonStrErrorMessageInValid() {
        AsyncQuery queryObj = new AsyncQuery();
        AsyncQueryThread queryThread = new AsyncQueryThread(queryObj, user, elide, runner, asyncQueryDao, "v1",
                resultStorageEngine, BASE_URL_ENDPOINT);
        String message = "{\"errors\": [{\"message \": \"error\"}]}";
        assertEquals(true, queryThread.checkJsonStrErrorMessage(message));
    }

    //Test with null resultStorageEngine
    @Test
    public void testProcessQueryNullResultEngine() throws Exception {

        AsyncQuery queryObj = new AsyncQuery();
        String responseBody = "{\"data\":{\"book\":{\"edges\":[{\"node\":{\"id\":\"1\",\"title\":\"Ender's Game\"}},"
                + "{\"node\":{\"id\":\"2\",\"title\":\"Song of Ice and Fire\"}},"
                + "{\"node\":{\"id\":\"3\",\"title\":\"For Whom the Bell Tolls\"}}]}}}";
        ElideResponse response = new ElideResponse(200, responseBody);
        String query = "{\"query\":\"{ group { edges { node { name commonName description } } } }\",\"variables\":null}";
        String id = "edc4a871-dff2-4054-804e-d80075cf827d";
        queryObj.setId(id);
        queryObj.setQuery(query);
        queryObj.setQueryType(QueryType.GRAPHQL_V1_0);
        queryObj.setResultType(ResultType.DOWNLOAD);

        when(runner.run(any(), eq(query), eq(user), any())).thenReturn(response);
        resultStorageEngine = new DefaultResultStorageEngine(DOWNLOAD_BASE_PATH, elide.getElideSettings(), mock(DataStore.class),
                asyncQueryDao);
        AsyncQueryThread queryThread = new AsyncQueryThread(queryObj, user, elide, runner, asyncQueryDao, "v1",
                null, BASE_URL_ENDPOINT);
        assertThrows(IllegalStateException.class, () -> {
            queryResultObj = queryThread.processQuery();
        });
    }
}
