/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.operation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import com.yahoo.elide.core.security.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;

public class JSONAPIAsyncQueryOperationTest {

    private User user;
    private Elide elide;
    private RequestScope requestScope;
    private AsyncExecutorService asyncExecutorService;

    @BeforeEach
    public void setupMocks() {
        user = mock(User.class);
        elide = mock(Elide.class);
        requestScope = mock(RequestScope.class);
        asyncExecutorService = mock(AsyncExecutorService.class);
        when(asyncExecutorService.getElide()).thenReturn(elide);
    }

    @Test
    public void testProcessQueryJsonApi() throws URISyntaxException {
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

        when(elide.get(any(), any(), any(), any(), any(), any(), any())).thenReturn(response);
        JSONAPIAsyncQueryOperation jsonOperation = new JSONAPIAsyncQueryOperation(asyncExecutorService, queryObj, requestScope);
        AsyncQueryResult queryResultObj = (AsyncQueryResult) jsonOperation.call();
        assertEquals(responseBody, queryResultObj.getResponseBody());
        assertEquals(200, queryResultObj.getHttpStatus());
        assertEquals(3, queryResultObj.getRecordCount());
    }

    @Test
    public void testProcessQueryNonSuccessResponse() throws URISyntaxException {
        AsyncQuery queryObj = new AsyncQuery();
        String responseBody = "ResponseBody";
        ElideResponse response = new ElideResponse(201, responseBody);
        String query = "/group?sort=commonName&fields%5Bgroup%5D=commonName,description";
        String id = "edc4a871-dff2-4054-804e-d80075cf827d";
        queryObj.setId(id);
        queryObj.setQuery(query);
        queryObj.setQueryType(QueryType.JSONAPI_V1_0);

        when(elide.get(any(), any(), any(), any(), any(), any(), any())).thenReturn(response);
        JSONAPIAsyncQueryOperation jsonOperation = new JSONAPIAsyncQueryOperation(asyncExecutorService, queryObj, requestScope);
        AsyncQueryResult queryResultObj = (AsyncQueryResult) jsonOperation.call();
        assertEquals(responseBody, queryResultObj.getResponseBody());
        assertEquals(201, queryResultObj.getHttpStatus());
        assertNull(queryResultObj.getRecordCount());
    }
}
