/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import com.yahoo.elide.graphql.QueryRunner;
import com.yahoo.elide.security.User;

import org.apache.http.NoHttpResponseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;

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

    @Test
    public void testProcessQueryJsonApi() throws NoHttpResponseException, URISyntaxException {
        AsyncQuery queryObj = new AsyncQuery();
        ElideResponse response = new ElideResponse(200, "ResponseBody");
        String query = "/group?sort=commonName&fields%5Bgroup%5D=commonName,description";
        String id = "edc4a871-dff2-4054-804e-d80075cf827d";
        queryObj.setId(id);
        queryObj.setQuery(query);
        queryObj.setQueryType(QueryType.JSONAPI_V1_0);

        when(elide.get(anyString(), any(), any(), anyString(), any())).thenReturn(response);
        AsyncQueryThread queryThread = new AsyncQueryThread(queryObj, user, elide, runner, asyncQueryDao, "v1",
                resultStorageEngine);
        queryResultObj = queryThread.processQuery();
        assertEquals(queryResultObj.getResponseBody(), "ResponseBody");
        assertEquals(queryResultObj.getHttpStatus(), 200);
    }

    @Test
    public void testProcessQueryGraphQl() throws NoHttpResponseException, URISyntaxException {
        AsyncQuery queryObj = new AsyncQuery();
        ElideResponse response = new ElideResponse(200, "ResponseBody");
        String query = "{\"query\":\"{ group { edges { node { name commonName description } } } }\",\"variables\":null}";
        String id = "edc4a871-dff2-4054-804e-d80075cf827d";
        queryObj.setId(id);
        queryObj.setQuery(query);
        queryObj.setQueryType(QueryType.GRAPHQL_V1_0);

        when(runner.run(eq(query), eq(user), any())).thenReturn(response);
        AsyncQueryThread queryThread = new AsyncQueryThread(queryObj, user, elide, runner, asyncQueryDao, "v1",
                resultStorageEngine);
        queryResultObj = queryThread.processQuery();
        assertEquals(queryResultObj.getResponseBody(), "ResponseBody");
        assertEquals(queryResultObj.getHttpStatus(), 200);
    }

    @Test
    public void testConvertJsonToCSV() throws URISyntaxException, NoHttpResponseException {

        String csvStr = "[key][\"value\"]";
        String jsonStr = "{\"key\":\"value\"}";
        AsyncQueryThread queryThread = mock(AsyncQueryThread.class);
        when(queryResultObj.getResponseBody()).thenReturn(jsonStr);
        when(queryThread.processQuery()).thenReturn(queryResultObj);
        when(queryThread.convertJsonToCSV(queryResultObj.getResponseBody())).thenReturn(csvStr);

        String jsonToCSV = queryThread.convertJsonToCSV(jsonStr);

        assertEquals(queryThread.convertJsonToCSV(jsonStr), csvStr);
    }
}
