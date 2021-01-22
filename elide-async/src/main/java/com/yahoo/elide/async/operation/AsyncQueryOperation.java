/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.operation;

import com.yahoo.elide.ElideResponse;
import com.yahoo.elide.async.models.AsyncQuery;
import com.yahoo.elide.async.service.AsyncExecutorService;

import org.apache.http.NoHttpResponseException;

import lombok.Getter;

/**
 * AsyncQuery Execute Operation Interface.
 */
public abstract class AsyncQueryOperation implements AsyncAPIOperation<AsyncQuery> {
    @Getter private AsyncExecutorService service;

    public AsyncQueryOperation(AsyncExecutorService service) {
        this.service = service;
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
}
