/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.operation;

import com.yahoo.elide.Elide;
import com.yahoo.elide.async.models.AsyncAPI;
import com.yahoo.elide.async.models.AsyncAPIResult;
import com.yahoo.elide.async.models.QueryStatus;
import com.yahoo.elide.async.service.dao.AsyncAPIDAO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Future;

/**
 * Runnable Operation for updating AsyncQueryResult.
 */
@Slf4j
@Data
@AllArgsConstructor
public class AsyncAPIUpdateOperation implements Runnable {

    private Elide elide;
    private Future<AsyncAPIResult> task;
    private AsyncAPI queryObj;
    private AsyncAPIDAO asyncAPIDao;

    /**
     * This is the main method which updates the Async API request.
     */
    @Override
    public void run() {
        try {
            AsyncAPIResult queryResultObj = task.get();
            // add queryResult object to query object
            asyncAPIDao.updateAsyncAPIResult(queryResultObj, queryObj.getId(), queryObj.getClass());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("InterruptedException: {}", e.toString());
            asyncAPIDao.updateStatus(queryObj.getId(), QueryStatus.FAILURE, queryObj.getClass());
        } catch (Exception e) {
            log.error("Exception: {}", e.toString());
            asyncAPIDao.updateStatus(queryObj.getId(), QueryStatus.FAILURE, queryObj.getClass());
        }
    }
}
