/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.service.thread;

import com.yahoo.elide.Elide;
import com.yahoo.elide.async.models.AsyncAPI;
import com.yahoo.elide.async.models.AsyncQuery;
import com.yahoo.elide.async.models.QueryStatus;
import com.yahoo.elide.async.service.DateUtil;
import com.yahoo.elide.async.service.dao.AsyncAPIDAO;
import com.yahoo.elide.core.Path.PathElement;
import com.yahoo.elide.core.filter.expression.AndFilterExpression;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.predicates.FilterPredicate;
import com.yahoo.elide.core.filter.predicates.InPredicate;
import com.yahoo.elide.core.filter.predicates.LEPredicate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Calendar;
import java.util.Date;

/**
 * Runnable for updating AsyncAPIThread status.
 * beyond the max run time and if not terminated by interrupt process
 * due to app/host crash or restart.
 */
@Slf4j
@Data
@AllArgsConstructor
public class AsyncAPICleanerRunnable implements Runnable {

    private int maxRunTimeMinutes;
    private Elide elide;
    private int queryCleanupDays;
    private AsyncAPIDAO asyncAPIDao;
    private DateUtil dateUtil = new DateUtil();

    @Override
    public void run() {
        deleteAsyncAPI(AsyncQuery.class);
        timeoutAsyncAPI(AsyncQuery.class);
    }

    /**
     * This method deletes the historical queries based on threshold.
     * @param type AsyncAPI Type Implementation.
     */
    protected <T extends AsyncAPI> void deleteAsyncAPI(Class<T> type) {

        try {
            Date cleanupDate = dateUtil.calculateFilterDate(Calendar.DATE, queryCleanupDays);
            PathElement createdOnPathElement = new PathElement(type, Long.class, "createdOn");
            FilterExpression fltDeleteExp = new LEPredicate(createdOnPathElement, cleanupDate);
            asyncAPIDao.deleteAsyncAPIAndResultByFilter(fltDeleteExp, type);
        } catch (Exception e) {
            log.error("Exception in scheduled cleanup: {}", e.toString());
        }
    }

    /**
     * This method updates the status of long running async query which
     * were interrupted due to host crash/app shutdown to TIMEDOUT.
     * @param type AsyncAPI Type Implementation.
     */
    protected <T extends AsyncAPI> void timeoutAsyncAPI(Class<T> type) {

        try {
            Date filterDate = dateUtil.calculateFilterDate(Calendar.MINUTE, maxRunTimeMinutes);
            PathElement createdOnPathElement = new PathElement(type, Long.class, "createdOn");
            PathElement statusPathElement = new PathElement(type, String.class, "status");
            FilterPredicate inPredicate = new InPredicate(statusPathElement, QueryStatus.PROCESSING,
                    QueryStatus.QUEUED);
            FilterPredicate lePredicate = new LEPredicate(createdOnPathElement, filterDate);
            AndFilterExpression fltTimeoutExp = new AndFilterExpression(inPredicate, lePredicate);
            asyncAPIDao.updateStatusAsyncAPIByFilter(fltTimeoutExp, QueryStatus.TIMEDOUT, type);
        } catch (Exception e) {
            log.error("Exception in scheduled cleanup: {}", e.toString());
        }
    }
}
