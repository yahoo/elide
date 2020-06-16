/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.service;

import com.yahoo.elide.Elide;
import com.yahoo.elide.async.models.AsyncQuery;
import com.yahoo.elide.async.models.QueryStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
    private AsyncQuery asyncQuery;
    private Date submittedOn;
    private int maxRunTimeMinutes;
    private AsyncQueryDAO asyncQueryDao;

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
            long interruptTimeMillies = calculateTimeOut(maxRunTimeMinutes, submittedOn);
            log.debug("Waiting on the future with the given timeout for {}", interruptTimeMillies);
            if (interruptTimeMillies > 0) {
               task.get(interruptTimeMillies, TimeUnit.MILLISECONDS);
            }
        } catch (InterruptedException e) {
            // Incase the future.get is interrupted , the underlying query may still have succeeded
            log.error("InterruptedException: {}", e);
        } catch (ExecutionException e) {
            // Query Status set to failure will be handled by the processQuery method
            log.error("ExecutionException: {}", e);
        } catch (TimeoutException e) {
            log.error("TimeoutException: {}", e);
            task.cancel(true);
            asyncQueryDao.updateStatus(asyncQuery, QueryStatus.TIMEDOUT);
        }
    }

    /**
     * Method to calculate the time left to interrupt since submission of thread in Milliseconds.
     * @param interruptTimeMinutes max duration to run the query
     * @param submittedOn time when query was submitted
     * @return Interrupt time left
     */
    private long calculateTimeOut(long maxRunTimeMinutes, Date submittedOn) {
        long maxRunTimeMinutesMillies = maxRunTimeMinutes * 60 * 1000;
        long interruptTimeMillies = maxRunTimeMinutesMillies - ((new Date()).getTime() - submittedOn.getTime());

        return interruptTimeMillies;
    }
}
