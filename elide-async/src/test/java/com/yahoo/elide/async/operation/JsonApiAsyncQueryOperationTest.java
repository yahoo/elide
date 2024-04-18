/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.async.operation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.paiondata.elide.Elide;
import com.paiondata.elide.ElideResponse;
import com.paiondata.elide.async.models.AsyncQuery;
import com.paiondata.elide.async.models.AsyncQueryResult;
import com.paiondata.elide.async.models.QueryType;
import com.paiondata.elide.async.service.AsyncExecutorService;
import com.paiondata.elide.core.RequestScope;
import com.paiondata.elide.core.request.route.Route;
import com.paiondata.elide.jsonapi.JsonApi;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;

public class JsonApiAsyncQueryOperationTest {

    private Elide elide;
    private JsonApi jsonApi;
    private RequestScope requestScope;
    private AsyncExecutorService asyncExecutorService;

    @BeforeEach
    public void setupMocks() {
        elide = mock(Elide.class);
        jsonApi = mock(JsonApi.class);
        requestScope = mock(RequestScope.class);
        asyncExecutorService = mock(AsyncExecutorService.class);
        when(requestScope.getRoute()).thenReturn(Route.builder().build());
        when(asyncExecutorService.getElide()).thenReturn(elide);
        when(asyncExecutorService.getJsonApi()).thenReturn(jsonApi);
    }

    @Test
    public void testProcessQueryJsonApi() throws URISyntaxException {
        AsyncQuery queryObj = new AsyncQuery();
        String responseBody = "{\"data\":"
                + "[{\"type\":\"book\",\"id\":\"3\",\"attributes\":{\"title\":\"For Whom the Bell Tolls\"}}"
                + ",{\"type\":\"book\",\"id\":\"2\",\"attributes\":{\"title\":\"Song of Ice and Fire\"}},"
                + "{\"type\":\"book\",\"id\":\"1\",\"attributes\":{\"title\":\"Ender's Game\"}}]}";
        ElideResponse<String> response = ElideResponse.status(200).body(responseBody);
        String query = "/group?sort=commonName&fields%5Bgroup%5D=commonName,description";
        String id = "edc4a871-dff2-4054-804e-d80075cf827d";
        queryObj.setId(id);
        queryObj.setQuery(query);
        queryObj.setQueryType(QueryType.JSONAPI_V1_0);

        when(jsonApi.get(any(), any(), any())).thenReturn(response);
        JsonApiAsyncQueryOperation jsonOperation = new JsonApiAsyncQueryOperation(asyncExecutorService, queryObj, requestScope);
        AsyncQueryResult queryResultObj = (AsyncQueryResult) jsonOperation.call();
        assertEquals(responseBody, queryResultObj.getResponseBody());
        assertEquals(200, queryResultObj.getHttpStatus());
        assertEquals(3, queryResultObj.getRecordCount());
    }

    @Test
    public void testProcessQueryNonSuccessResponse() throws URISyntaxException {
        AsyncQuery queryObj = new AsyncQuery();
        String responseBody = "ResponseBody";
        ElideResponse<String> response = ElideResponse.status(201).body(responseBody);
        String query = "/group?sort=commonName&fields%5Bgroup%5D=commonName,description";
        String id = "edc4a871-dff2-4054-804e-d80075cf827d";
        queryObj.setId(id);
        queryObj.setQuery(query);
        queryObj.setQueryType(QueryType.JSONAPI_V1_0);

        when(jsonApi.get(any(), any(), any())).thenReturn(response);
        JsonApiAsyncQueryOperation jsonOperation = new JsonApiAsyncQueryOperation(asyncExecutorService, queryObj, requestScope);
        AsyncQueryResult queryResultObj = (AsyncQueryResult) jsonOperation.call();
        assertEquals(responseBody, queryResultObj.getResponseBody());
        assertEquals(201, queryResultObj.getHttpStatus());
        assertNull(queryResultObj.getRecordCount());
    }
}
