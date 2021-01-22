/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.service.thread;

import com.yahoo.elide.async.models.AsyncAPI;
import com.yahoo.elide.async.models.AsyncAPIResult;
import com.yahoo.elide.async.operation.AsyncAPIOperation;
import com.yahoo.elide.core.RequestScope;
import org.apache.http.NoHttpResponseException;
import lombok.extern.slf4j.Slf4j;

import java.net.URISyntaxException;
import java.util.concurrent.Callable;

/**
 * AsyncAPI implementation of Callable for executing the query provided in AsyncQuery.
 * It will also update the query status and result object at different stages of execution.
 */
@Slf4j
public class AsyncAPICallable implements Callable<AsyncAPIResult> {
    private AsyncAPI queryObj;
    private AsyncAPIOperation<?> operation;
    private RequestScope scope;

    public AsyncAPICallable(AsyncAPI queryObj, AsyncAPIOperation<?> operation, RequestScope scope) {
        this.queryObj = queryObj;
        this.operation = operation;
        this.scope = scope;
    }

    @Override
    public AsyncAPIResult call() throws URISyntaxException, NoHttpResponseException {
        log.debug("AsyncQuery Object from request: {}", this);

        AsyncAPIResult queryResult = operation.execute(queryObj, scope);
        return queryResult;
    }
}
