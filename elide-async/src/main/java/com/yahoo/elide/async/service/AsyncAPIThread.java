/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.service;

import com.yahoo.elide.async.models.AsyncAPIResult;

import org.apache.http.NoHttpResponseException;

import lombok.Data;

import java.net.URISyntaxException;
import java.util.concurrent.Callable;

/**
 * Callable thread for executing the query provided in AsyncQuery and TableExport.
 * It will also update the query status and result object at different
 * stages of execution.
 */
@Data
public abstract class AsyncAPIThread implements Callable<AsyncAPIResult> {

   /**
    * This is the main method which processes the AsyncAPI request.
    * It executes the query and updates values for AsyncQuery and AsyncQueryResult models accordingly.
    * @return AsyncQueryResultBase AsyncQueryResultBase
    * @throws URISyntaxException URISyntaxException
    * @throws NoHttpResponseException NoHttpResponseException
    */
    @Override
    public abstract AsyncAPIResult call() throws NoHttpResponseException, URISyntaxException;
}
