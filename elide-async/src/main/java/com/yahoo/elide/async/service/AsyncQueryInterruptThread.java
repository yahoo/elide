/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.service;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.yahoo.elide.Elide;
import com.yahoo.elide.async.models.QueryStatus;

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
            
            if(interruptTimeMillies > 0) {
               log.debug("Waiting on the future with the given timeout for {}", interruptTimeMillies);
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
            asyncQueryDao.updateAsyncQuery(id, (asyncQueryObj) -> {
                asyncQueryObj.setStatus(QueryStatus.TIMEDOUT);
                });
        }
    }
    
    /**
     * Method to calculate the time left to interrupt since submission of thread 
     * in Milliseconds.
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
