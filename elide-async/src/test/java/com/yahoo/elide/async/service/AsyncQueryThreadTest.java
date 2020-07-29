/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import com.yahoo.elide.graphql.QueryRunner;
import com.yahoo.elide.security.User;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.jayway.jsonpath.InvalidJsonException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


public class AsyncQueryThreadTest {

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

        when(elide.get(anyString(), any(), any(), anyString(), any())).thenReturn(response);
        AsyncQueryThread queryThread = new AsyncQueryThread(queryObj, user, elide, runner, asyncQueryDao, "v1",
                resultStorageEngine);
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

        when(elide.get(anyString(), any(), any(), anyString(), any())).thenReturn(response);
        AsyncQueryThread queryThread = new AsyncQueryThread(queryObj, user, elide, runner, asyncQueryDao, "v1",
                resultStorageEngine);
        queryResultObj = queryThread.processQuery();

        assertEquals(queryResultObj.getRecordCount(), 0);
        assertEquals(queryResultObj.getResponseBody(), responseBody);
        assertEquals(queryResultObj.getHttpStatus(), 200);
    }

    //Test with invalid input for responseBody. An exception will be thrown.
    @Test
    public void testProcessQueryJsonApi3() throws Exception {
        assertThrows(JsonProcessingException.class, () -> {
        AsyncQuery queryObj = new AsyncQuery();
        ElideResponse response = new ElideResponse(200, "ResponseBody");
        String query = "/group?sort=commonName&fields%5Bgroup%5D=commonName,description";
        String id = "edc4a871-dff2-4054-804e-d80075cf827d";
        queryObj.setId(id);
        queryObj.setQuery(query);
        queryObj.setQueryType(QueryType.JSONAPI_V1_0);
        queryObj.setResultType(ResultType.EMBEDDED);

        when(elide.get(anyString(), any(), any(), anyString(), any())).thenReturn(response);
        AsyncQueryThread queryThread = new AsyncQueryThread(queryObj, user, elide, runner, asyncQueryDao, "v1",
                resultStorageEngine);
        queryResultObj = queryThread.processQuery();

        assertEquals(queryResultObj.getRecordCount(), null);
        assertEquals(queryResultObj.getResponseBody(), "ResponseBody");
        });

    }

    //Test with invalid input for responseBody. An exception will be thrown.
    @Test
    public void testProcessQueryGraphQl() throws Exception {
        assertThrows(JsonParseException.class, () -> {
            AsyncQuery queryObj = new AsyncQuery();
            ElideResponse response = new ElideResponse(200, "ResponseBody");
            String query = "{\"query\":\"{ group { edges { node { name commonName description } } } }\",\"variables\":null}";
            String id = "edc4a871-dff2-4054-804e-d80075cf827d";
            queryObj.setId(id);
            queryObj.setQuery(query);
            queryObj.setQueryType(QueryType.GRAPHQL_V1_0);
            queryObj.setResultType(ResultType.EMBEDDED);

            when(runner.run(eq(query), eq(user), any())).thenReturn(response);
            AsyncQueryThread queryThread = new AsyncQueryThread(queryObj, user, elide, runner, asyncQueryDao, "v1",
                    resultStorageEngine);
            queryResultObj = queryThread.processQuery();
            assertEquals(queryResultObj.getResponseBody(), "ResponseBody");
            assertEquals(queryResultObj.getHttpStatus(), 200);
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

        when(runner.run(eq(query), eq(user), any())).thenReturn(response);
        AsyncQueryThread queryThread = new AsyncQueryThread(queryObj, user, elide, runner, asyncQueryDao, "v1",
                resultStorageEngine);
        queryResultObj = queryThread.processQuery();
        assertEquals(queryResultObj.getRecordCount(), 3);
        assertEquals(queryResultObj.getResponseBody(), responseBody);
        assertEquals(queryResultObj.getHttpStatus(), 200);

    }

    // Standard positive test case that converts json to csv format.
    @Test
    public void testConvertJsonToCSV() throws Exception {

        String csvStr = "[key][\"value\"]";
        String jsonStr = "{\"key\":\"value\"}";

        AsyncQuery queryObj = new AsyncQuery();
        AsyncQueryThread queryThread = new AsyncQueryThread(queryObj, user, elide, runner, asyncQueryDao, "v1",
                resultStorageEngine);

        assertEquals(queryThread.convertJsonToCSV(jsonStr), csvStr);
    }

    //Invalid input for the json to csv conversion. This throws an exception.
    @Test
    public void testConvertJsonToCSV2() throws Exception {

        String csvStr = "ResponseBody";
        assertThrows(InvalidJsonException.class, () -> {
            String jsonStr = "ResponseBody";

            AsyncQuery queryObj = new AsyncQuery();
            AsyncQueryThread queryThread = new AsyncQueryThread(queryObj, user, elide, runner, asyncQueryDao, "v1",
                    resultStorageEngine);

            assertEquals(queryThread.convertJsonToCSV(jsonStr), csvStr);
        });
    }
}
