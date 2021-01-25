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

import org.apache.http.NoHttpResponseException;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.net.URISyntaxException;
import java.util.Date;

/**
 * AsyncQuery Execute Operation Interface.
 */
@Slf4j
public abstract class AsyncQueryOperation implements AsyncAPIOperation<AsyncQuery> {
    @Getter private AsyncExecutorService service;
    private AsyncAPI queryObj;
    private RequestScope scope;

    public AsyncQueryOperation(AsyncExecutorService service, AsyncAPI queryObj, RequestScope scope) {
        this.service = service;
        this.queryObj = queryObj;
        this.scope = scope;
    }

    @Override
    public AsyncAPIResult call() throws URISyntaxException, NoHttpResponseException {
        ElideResponse response = null;
        log.debug("AsyncQuery Object from request: {}", this);
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
     * @throws NoHttpResponseException NoHttpResponseException Exception.
     */
    public void nullResponseCheck(ElideResponse response) throws NoHttpResponseException {
        if (response == null) {
            throw new NoHttpResponseException("Response for request returned as null");
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
