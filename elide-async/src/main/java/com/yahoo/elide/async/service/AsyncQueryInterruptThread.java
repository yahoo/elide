/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.service;

import java.io.IOException;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.yahoo.elide.Elide;
import com.yahoo.elide.async.models.AsyncQuery;
import com.yahoo.elide.async.models.QueryStatus;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.request.EntityProjection;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Runnable thread for terminating AsyncQueryThread executing
 * beyond the max run time and update status.
 */
@Slf4j
@Data
@AllArgsConstructor
public class AsyncQueryInterruptThread implements Runnable {

    private Elide elide;
    private Future<?> task;
    private UUID id;
    private Date submittedOn;
    private int interruptTime;

    @Override
    public void run() {
        interruptQuery();
    }

    /**
     * This is the main method which interrupts the Async Query request, if it has executed beyond
     * the maximum run time.
     */
    protected void interruptQuery() {
        try {
            long interruptTimeInMillies = interruptTime * 60 * 1000;
            long differenceInMillies = interruptTimeInMillies - ((new Date()).getTime() - submittedOn.getTime());
            if(differenceInMillies > 0) {
               log.debug("Waiting on the future with the given timeout for {}", differenceInMillies);
               task.get(differenceInMillies, TimeUnit.MILLISECONDS);
            }
        } catch (InterruptedException e) {
            // Incase the future.get is interrupted , the underlying query may still have succeeded
            log.error("InterruptedException: {}", e.getMessage());
        } catch (ExecutionException e) {
            // Query Status set to failure will be handled by the processQuery method
            log.error("ExecutionException: {}", e.getMessage());
        } catch (TimeoutException e) {
            log.error("TimeoutException: {}", e.getMessage());
            task.cancel(true);
            updateAsyncQueryStatus(QueryStatus.TIMEDOUT, id);
        }
    }

    /**
     * This method updates the model for AsyncQuery with passed query status value.
     * @param status new status based on the enum QueryStatus
     * @param asyncQueryId queryId from asyncQuery request
     */
    protected void updateAsyncQueryStatus(QueryStatus status, UUID asyncQueryId) {
        log.debug("Updating AsyncQuery status to {}", status);
        DataStoreTransaction tx = elide.getDataStore().beginTransaction();

        // Creating new RequestScope for Datastore transaction
        RequestScope scope = new RequestScope(null, null, tx, null, null, elide.getElideSettings());

        try {
            EntityProjection asyncQueryCollection = EntityProjection.builder()
            .type(AsyncQuery.class)
            .build();
            AsyncQuery query = (AsyncQuery) tx.loadObject(asyncQueryCollection, asyncQueryId, scope);
            query.setStatus(status);
            tx.save(query, scope);
            tx.commit(scope);
            tx.flush(scope);
            tx.close();
        } catch (Exception e) {
            log.error("Exception: {}", e.getMessage());
        } finally {
            try {
                tx.close();
            } catch (IOException e) {
                log.error("IOException: {}", e.getMessage());
            }
        }
    }
}
