/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideResponse;
import com.yahoo.elide.async.models.AsyncQuery;
import com.yahoo.elide.async.models.QueryStatus;
import com.yahoo.elide.async.models.QueryType;
import com.yahoo.elide.graphql.QueryRunner;
import com.yahoo.elide.security.User;

public class AsyncQueryThreadTest {

    private AsyncQueryThread queryThread;
    private User user;
    private Elide elide;
    private QueryRunner runner;
    private AsyncQuery queryObj;
    private AsyncQueryDAO asyncQueryDao;

    @BeforeEach
    public void setupMocks() {
        user = mock(User.class);
        elide = mock(Elide.class);
        runner = mock(QueryRunner.class);
        queryObj = mock(AsyncQuery.class);
        asyncQueryDao = mock(DefaultAsyncQueryDAO.class);
        queryThread = new AsyncQueryThread(queryObj, user, elide, runner, asyncQueryDao);
    }

    @Test
    public void testAsyncQueryCleanerThreadSet() {
        assertEquals(queryObj, queryThread.getQueryObj());
        assertEquals(user, queryThread.getUser());
        assertEquals(elide, queryThread.getElide());
        assertEquals(runner, queryThread.getRunner());
        assertEquals(asyncQueryDao, queryThread.getAsyncQueryDao());
    }

    @Test
    public void testProcessQueryJsonApi() {
        String query = "/group?sort=commonName&fields%5Bgroup%5D=commonName,description";
        ElideResponse response = mock(ElideResponse.class);

        when(queryObj.getQuery()).thenReturn(query);
        when(queryObj.getQueryType()).thenReturn(QueryType.JSONAPI_V1_0);
        when(elide.get(anyString(), any(), any())).thenReturn(response);
        when(response.getResponseCode()).thenReturn(200);
        when(response.getBody()).thenReturn("ResponseBody");

        queryThread.processQuery();

        verify(asyncQueryDao, times(1)).updateStatus(queryObj, QueryStatus.PROCESSING);
        verify(asyncQueryDao, times(1)).updateStatus(queryObj, QueryStatus.COMPLETE);
        verify(asyncQueryDao, times(1)).createAsyncQueryResult(anyInt(), anyString(), any(), any());
    }

    @Test
    public void testProcessQueryGraphQl() {
        String query = "{\"query\":\"{ group { edges { node { name commonName description } } } }\",\"variables\":null}";
        ElideResponse response = mock(ElideResponse.class);

        when(queryObj.getQuery()).thenReturn(query);
        when(queryObj.getQueryType()).thenReturn(QueryType.GRAPHQL_V1_0);
        when(runner.run(query, user)).thenReturn(response);
        when(response.getResponseCode()).thenReturn(200);
        when(response.getBody()).thenReturn("ResponseBody");

        queryThread.processQuery();

        verify(asyncQueryDao, times(1)).updateStatus(queryObj, QueryStatus.PROCESSING);
        verify(asyncQueryDao, times(1)).updateStatus(queryObj, QueryStatus.COMPLETE);
        verify(asyncQueryDao, times(1)).createAsyncQueryResult(anyInt(), anyString(), any(), any());
    }

    @Test
    public void testProcessQueryNoResponse() {
        String query = "{\"query\":\"{ group { edges { node { name commonName description } } } }\",\"variables\":null}";

        when(queryObj.getQuery()).thenReturn(query);
        when(queryObj.getQueryType()).thenReturn(QueryType.GRAPHQL_V1_0);
        when(runner.run(query, user)).thenReturn(null);

        queryThread.processQuery();

        verify(asyncQueryDao, times(1)).updateStatus(queryObj, QueryStatus.PROCESSING);
        verify(asyncQueryDao, times(1)).updateStatus(queryObj, QueryStatus.FAILURE);
    }
}
