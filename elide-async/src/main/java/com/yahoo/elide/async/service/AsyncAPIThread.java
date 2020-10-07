/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.service;

import com.yahoo.elide.async.models.AsyncAPI;
import com.yahoo.elide.async.models.AsyncAPIResult;
import com.yahoo.elide.security.User;

import org.apache.http.NoHttpResponseException;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.net.URISyntaxException;
import java.util.concurrent.Callable;

/**
 * Callable thread for executing the query provided in AsyncQuery and TableExport.
 * It will also update the query status and result object at different
 * stages of execution.
 */
@Slf4j
@Data
public class AsyncAPIThread implements Callable<AsyncAPIResult> {

    private AsyncAPI queryObj;
    private User user;
    private AsyncExecutorService service;
    private String apiVersion;

    @Override
    public AsyncAPIResult call() throws NoHttpResponseException, URISyntaxException {
         return processQuery();
    }

    public AsyncAPIThread(AsyncAPI queryObj, User user, AsyncExecutorService service, String apiVersion) {
        this.queryObj = queryObj;
        this.user = user;
        this.service = service;
        this.apiVersion = apiVersion;
    }

   /**
    * This is the main method which processes the Async Query request, executes the query and updates
    * values for AsyncQuery and AsyncQueryResult models accordingly.
    * @return AsyncQueryResultBase AsyncQueryResultBase
    * @throws URISyntaxException URISyntaxException
    * @throws NoHttpResponseException NoHttpResponseException
    */
    protected AsyncAPIResult processQuery() throws URISyntaxException, NoHttpResponseException {
        // Create AsyncQueryResultBase Object
        AsyncAPIResult queryResultBaseObj = queryObj.executeRequest(service, user, apiVersion);

        return queryResultBaseObj;
    }
}
