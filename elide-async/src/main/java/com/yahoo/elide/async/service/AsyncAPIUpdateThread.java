/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.service;

import com.yahoo.elide.Elide;
import com.yahoo.elide.async.models.AsyncAPI;
import com.yahoo.elide.async.models.AsyncAPIResult;
import com.yahoo.elide.async.models.QueryStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Future;

/**
 * Runnable thread for updating AsyncQueryResult.
 */
@Slf4j
@Data
@AllArgsConstructor
public class AsyncAPIUpdateThread implements Runnable {

    private Elide elide;
    private Future<AsyncAPIResult> task;
    private AsyncAPI queryObj;
    private AsyncQueryDAO asyncQueryDao;

    @Override
    public void run() {
        updateQuery();
    }

    /**
     * This is the main method which updates the Async Query request.
     */
    protected void updateQuery() {
        try {
            AsyncAPIResult queryResultObj = task.get();
            // add queryResult object to query object
            asyncQueryDao.updateAsyncQueryResult(queryResultObj, queryObj.getId(), queryObj.getClass());

        } catch (InterruptedException e) {
            log.error("InterruptedException: {}", e);
            asyncQueryDao.updateStatus(queryObj.getId(), QueryStatus.FAILURE, queryObj.getClass());
        } catch (Exception e) {
            log.error("Exception: {}", e);
            asyncQueryDao.updateStatus(queryObj.getId(), QueryStatus.FAILURE, queryObj.getClass());
        }
    }
}
