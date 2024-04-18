/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.async.operation;

import com.paiondata.elide.Elide;
import com.paiondata.elide.async.models.AsyncApi;
import com.paiondata.elide.async.models.AsyncApiResult;
import com.paiondata.elide.async.models.QueryStatus;
import com.paiondata.elide.async.service.dao.AsyncApiDao;

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
public class AsyncApiUpdateOperation implements Runnable {

    private Elide elide;
    private Future<AsyncApiResult> task;
    private AsyncApi queryObj;
    private AsyncApiDao asyncApiDao;

    /**
     * This is the main method which updates the Async API request.
     */
    @Override
    public void run() {
        try {
            AsyncApiResult queryResultObj = task.get();
            // add queryResult object to query object
            asyncApiDao.updateAsyncApiResult(queryResultObj, queryObj.getId(), queryObj.getClass());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("InterruptedException: {}", e.toString());
            asyncApiDao.updateStatus(queryObj.getId(), QueryStatus.FAILURE, queryObj.getClass());
        } catch (Exception e) {
            log.error("Exception: {}", e.toString());
            asyncApiDao.updateStatus(queryObj.getId(), QueryStatus.FAILURE, queryObj.getClass());
        }
    }
}
