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
import java.util.HashMap;
import java.util.Map;

public class AsyncAPIThreadTest {

    private User user;
    private Elide elide;
    private Map<String, QueryRunner> runners = new HashMap();
    private QueryRunner runner;
    private AsyncExecutorService asyncExecutorService;

    @BeforeEach
    public void setupMocks() {
        user = mock(User.class);
        elide = mock(Elide.class);
        runner = mock(QueryRunner.class);
        runners.put("v1", runner);
        asyncExecutorService = mock(AsyncExecutorService.class);
        when(asyncExecutorService.getElide()).thenReturn(elide);
        when(asyncExecutorService.getRunners()).thenReturn(runners);
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

        when(elide.get(anyString(), anyString(), any(), any(), anyString(), any())).thenReturn(response);
        AsyncAPIThread queryThread = new AsyncAPIThread(queryObj, user, asyncExecutorService, "v1");
        AsyncQueryResult queryResultObj = (AsyncQueryResult) queryThread.processQuery();
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

        when(runner.run(anyString(), eq(query), eq(user), any())).thenReturn(response);
        AsyncAPIThread queryThread = new AsyncAPIThread(queryObj, user, asyncExecutorService, "v1");
        AsyncQueryResult queryResultObj = (AsyncQueryResult) queryThread.processQuery();
        assertEquals(queryResultObj.getResponseBody(), "ResponseBody");
        assertEquals(queryResultObj.getHttpStatus(), 200);
    }
}
