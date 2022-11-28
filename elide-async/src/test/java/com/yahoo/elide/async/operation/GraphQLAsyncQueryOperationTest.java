/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.operation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideResponse;
import com.yahoo.elide.async.models.AsyncQuery;
import com.yahoo.elide.async.models.AsyncQueryResult;
import com.yahoo.elide.async.models.QueryType;
import com.yahoo.elide.async.service.AsyncExecutorService;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.exceptions.InvalidOperationException;
import com.yahoo.elide.core.security.User;
import com.yahoo.elide.graphql.QueryRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

public class GraphQLAsyncQueryOperationTest {

    private User user;
    private Elide elide;
    private RequestScope requestScope;
    private Map<String, QueryRunner> runners = new HashMap<>();
    private QueryRunner runner;
    private AsyncExecutorService asyncExecutorService;

    @BeforeEach
    public void setupMocks() {
        user = mock(User.class);
        elide = mock(Elide.class);
        requestScope = mock(RequestScope.class);
        runner = mock(QueryRunner.class);
        runners.put("v1", runner);
        asyncExecutorService = mock(AsyncExecutorService.class);
        when(asyncExecutorService.getElide()).thenReturn(elide);
        when(asyncExecutorService.getRunners()).thenReturn(runners);
        when(requestScope.getApiVersion()).thenReturn("v1");
    }

    @Test
    public void testProcessQueryGraphQl() throws URISyntaxException  {
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

        when(runner.run(any(), any(), any(), any(), any())).thenReturn(response);
        GraphQLAsyncQueryOperation graphQLOperation = new GraphQLAsyncQueryOperation(asyncExecutorService, queryObj, requestScope);
        AsyncQueryResult queryResultObj = (AsyncQueryResult) graphQLOperation.call();
        assertEquals(responseBody, queryResultObj.getResponseBody());
        assertEquals(200, queryResultObj.getHttpStatus());
        assertEquals(3, queryResultObj.getRecordCount());
    }

    @Test
    public void testProcessQueryGraphQlInvalidResponse() throws URISyntaxException {
        AsyncQuery queryObj = new AsyncQuery();
        String responseBody = "ResponseBody";
        ElideResponse response = new ElideResponse(200, responseBody);
        String query = "{\"query\":\"{ group { edges { node { name commonName description } } } }\",\"variables\":null}";
        String id = "edc4a871-dff2-4054-804e-d80075cf827d";
        queryObj.setId(id);
        queryObj.setQuery(query);
        queryObj.setQueryType(QueryType.GRAPHQL_V1_0);

        when(runner.run(any(), any(), any(), any(), any())).thenReturn(response);
        GraphQLAsyncQueryOperation graphQLOperation = new GraphQLAsyncQueryOperation(asyncExecutorService, queryObj, requestScope);
        AsyncQueryResult queryResultObj = (AsyncQueryResult) graphQLOperation.call();
        assertEquals(responseBody, queryResultObj.getResponseBody());
        assertEquals(200, queryResultObj.getHttpStatus());
        assertEquals(0, queryResultObj.getRecordCount());
    }

    @Test
    public void testProcessQueryGraphQlRunnerException() {
        AsyncQuery queryObj = new AsyncQuery();
        String query = "{\"query\":\"{ group { edges { node { name commonName description } } } }\",\"variables\":null}";
        String id = "edc4a871-dff2-4054-804e-d80075cf827d";
        queryObj.setId(id);
        queryObj.setQuery(query);
        queryObj.setQueryType(QueryType.GRAPHQL_V1_0);

        when(runner.run(any(), any(), any(), any())).thenThrow(RuntimeException.class);
        GraphQLAsyncQueryOperation graphQLOperation = new GraphQLAsyncQueryOperation(asyncExecutorService, queryObj, requestScope);
        assertThrows(RuntimeException.class, () -> graphQLOperation.call());
    }

    @Test
    public void testProcessQueryGraphQlApiVersionNotSupported() {
        AsyncQuery queryObj = new AsyncQuery();
        String query = "{\"query\":\"{ group { edges { node { name commonName description } } } }\",\"variables\":null}";
        String id = "edc4a871-dff2-4054-804e-d80075cf827d";
        queryObj.setId(id);
        queryObj.setQuery(query);
        queryObj.setQueryType(QueryType.GRAPHQL_V1_0);

        when(requestScope.getApiVersion()).thenReturn("v2");
        GraphQLAsyncQueryOperation graphQLOperation = new GraphQLAsyncQueryOperation(asyncExecutorService, queryObj, requestScope);
        assertThrows(InvalidOperationException.class, () -> graphQLOperation.call());
    }
}
