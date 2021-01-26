/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.operation;

import com.yahoo.elide.ElideResponse;
import com.yahoo.elide.async.models.AsyncAPI;
import com.yahoo.elide.async.models.AsyncAPIResult;
import com.yahoo.elide.async.models.AsyncQuery;
import com.yahoo.elide.async.models.AsyncQueryResult;
import com.yahoo.elide.async.service.AsyncExecutorService;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.security.User;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.net.URISyntaxException;
import java.util.Date;
import java.util.concurrent.Callable;

/**
 * AsyncQuery Execute Operation Interface.
 */
@Slf4j
public abstract class AsyncQueryCallableOperation implements Callable<AsyncAPIResult> {
    @Getter private AsyncExecutorService service;
    private AsyncQuery queryObj;
    private RequestScope scope;

    public AsyncQueryCallableOperation(AsyncExecutorService service, AsyncAPI queryObj, RequestScope scope) {
        this.service = service;
        this.queryObj = (AsyncQuery) queryObj;
        this.scope = scope;
    }

    @Override
    public AsyncAPIResult call() throws URISyntaxException {
        ElideResponse response = null;
        log.debug("AsyncQuery Object from request: {}", queryObj);
        response = execute(queryObj, scope.getUser(), scope.getApiVersion());
        nullResponseCheck(response);

        AsyncQueryResult queryResult = new AsyncQueryResult();
        queryResult.setHttpStatus(response.getResponseCode());
        queryResult.setCompletedOn(new Date());
        queryResult.setResponseBody(response.getBody());
        queryResult.setContentLength(response.getBody().length());
        queryResult.setRecordCount(calculateRecordCount((AsyncQuery) queryObj, response));
        return queryResult;
    }

    /**
     * Calculate Record Count in the response.
     * @param queryObj AsyncAPI type object.
     * @param response ElideResponse object.
     * @return Integer record count
     */
    public abstract Integer calculateRecordCount(AsyncQuery queryObj, ElideResponse response);

    /**
     * Check if Elide Response is NULL.
     * @param response ElideResponse object.
     * @throws IllegalStateException IllegalStateException Exception.
     */
    public void nullResponseCheck(ElideResponse response) {
        if (response == null) {
            throw new IllegalStateException("No Response for request returned");
        }
    }

    /**
     * Execute the Async Query Request.
     * @param queryObj AsyncAPI type object.
     * @param user User object.
     * @param apiVersion Api Version.
     * @return response ElideResponse object.
     * @throws URISyntaxException URISyntaxException Exception.
     */
    public abstract ElideResponse execute(AsyncAPI queryObj, User user, String apiVersion)  throws URISyntaxException;
}
