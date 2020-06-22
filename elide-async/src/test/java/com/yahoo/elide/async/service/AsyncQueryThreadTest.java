/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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

    private AsyncQueryThread queryThread;
    private User user;
    private Elide elide;
    private QueryRunner runner;
    private AsyncQuery queryObj;
    private AsyncQueryResult queryResultObj;
    private AsyncQueryDAO asyncQueryDao;
    private ResultStorageEngine resultStorageEngine;

    @BeforeEach
    public void setupMocks() {
        user = mock(User.class);
        elide = mock(Elide.class);
        runner = mock(QueryRunner.class);
        queryObj = mock(AsyncQuery.class);
        queryResultObj = mock(AsyncQueryResult.class);
        asyncQueryDao = mock(DefaultAsyncQueryDAO.class);
        resultStorageEngine = mock(DefaultResultStorageEngine.class);
        queryThread = new AsyncQueryThread(queryObj, user, elide, runner, asyncQueryDao, "v1");
    }

    @Test
    public void testAsyncQueryThreadSet() {
        assertEquals(queryObj, queryThread.getQueryObj());
        assertEquals(user, queryThread.getUser());
        assertEquals(elide, queryThread.getElide());
        assertEquals(runner, queryThread.getRunner());
        assertEquals(asyncQueryDao, queryThread.getAsyncQueryDao());
    }

    @Test
    public void testProcessQueryJsonApi() throws NoHttpResponseException, URISyntaxException {
        String query = "/group?sort=commonName&fields%5Bgroup%5D=commonName,description";
        String id = "edc4a871-dff2-4054-804e-d80075cf827d";
        ElideResponse response = mock(ElideResponse.class);

        when(queryObj.getQuery()).thenReturn(query);
        when(queryObj.getId()).thenReturn(id);
        when(queryObj.getQueryType()).thenReturn(QueryType.JSONAPI_V1_0);
        when(elide.get(anyString(), any(), any(), anyString())).thenReturn(response);
        when(response.getResponseCode()).thenReturn(200);
        when(response.getBody()).thenReturn("responseBody");

        queryResultObj = queryThread.processQuery();

        assertEquals(queryResultObj.getResponseBody(), "responseBody");
        assertEquals(queryResultObj.getHttpStatus(), 200);
    }

    @Test
    public void testProcessQueryGraphQl() throws NoHttpResponseException, URISyntaxException {
        String query = "{\"query\":\"{ group { edges { node { name commonName description } } } }\",\"variables\":null}";
        String id = "edc4a871-dff2-4054-804e-d80075cf827d";
        ElideResponse response = mock(ElideResponse.class);

        when(queryObj.getQuery()).thenReturn(query);
        when(queryObj.getId()).thenReturn(id);
        when(queryObj.getQueryType()).thenReturn(QueryType.GRAPHQL_V1_0);
        when(runner.run(query, user)).thenReturn(response);
        when(response.getResponseCode()).thenReturn(200);

        when(response.getBody()).thenReturn("responseBody");

        queryResultObj = queryThread.processQuery();

        assertEquals(queryResultObj.getResponseBody(), "responseBody");
        assertEquals(queryResultObj.getHttpStatus(), 200);
    }
}
