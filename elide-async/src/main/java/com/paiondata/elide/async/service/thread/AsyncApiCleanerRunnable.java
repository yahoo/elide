/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.async.service.thread;

import com.paiondata.elide.Elide;
import com.paiondata.elide.async.models.AsyncApi;
import com.paiondata.elide.async.models.AsyncQuery;
import com.paiondata.elide.async.models.QueryStatus;
import com.paiondata.elide.async.service.dao.AsyncApiDao;
import com.paiondata.elide.core.Path.PathElement;
import com.paiondata.elide.core.filter.expression.AndFilterExpression;
import com.paiondata.elide.core.filter.expression.FilterExpression;
import com.paiondata.elide.core.filter.predicates.FilterPredicate;
import com.paiondata.elide.core.filter.predicates.InPredicate;
import com.paiondata.elide.core.filter.predicates.LEPredicate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * Runnable for updating AsyncApiThread status.
 * beyond the max run time and if not terminated by interrupt process
 * due to app/host crash or restart.
 */
@Slf4j
@Data
@AllArgsConstructor
public class AsyncApiCleanerRunnable implements Runnable {

    private Duration queryMaxRunTime;
    private Elide elide;
    private Duration queryRetentionDuration;
    private AsyncApiDao asyncApiDao;
    private Clock clock;

    @Override
    public void run() {
        deleteAsyncApi(AsyncQuery.class);
        timeoutAsyncApi(AsyncQuery.class);
    }

    /**
     * This method deletes the historical queries based on threshold.
     * @param type AsyncApi Type Implementation.
     */
    protected <T extends AsyncApi> void deleteAsyncApi(Class<T> type) {

        try {
            Date cleanupDate = Date.from(Instant.now(clock).plus(queryRetentionDuration));
            PathElement createdOnPathElement = new PathElement(type, Long.class, "createdOn");
            FilterExpression fltDeleteExp = new LEPredicate(createdOnPathElement, cleanupDate);
            asyncApiDao.deleteAsyncApiAndResultByFilter(fltDeleteExp, type);
        } catch (Exception e) {
            log.error("Exception in scheduled cleanup: {}", e.toString());
        }
    }

    /**
     * This method updates the status of long running async query which
     * were interrupted due to host crash/app shutdown to TIMEDOUT.
     * @param type AsyncApi Type Implementation.
     */
    protected <T extends AsyncApi> void timeoutAsyncApi(Class<T> type) {

        try {
            Date filterDate = Date.from(Instant.now(clock).plus(queryMaxRunTime));
            PathElement createdOnPathElement = new PathElement(type, Long.class, "createdOn");
            PathElement statusPathElement = new PathElement(type, String.class, "status");
            FilterPredicate inPredicate = new InPredicate(statusPathElement, QueryStatus.PROCESSING,
                    QueryStatus.QUEUED);
            FilterPredicate lePredicate = new LEPredicate(createdOnPathElement, filterDate);
            AndFilterExpression fltTimeoutExp = new AndFilterExpression(inPredicate, lePredicate);
            asyncApiDao.updateStatusAsyncApiByFilter(fltTimeoutExp, QueryStatus.TIMEDOUT, type);
        } catch (Exception e) {
            log.error("Exception in scheduled cleanup: {}", e.toString());
        }
    }
}
