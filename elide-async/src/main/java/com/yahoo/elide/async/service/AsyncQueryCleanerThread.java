/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.service;

import com.yahoo.elide.Elide;
import com.yahoo.elide.async.models.AsyncAPI;
import com.yahoo.elide.async.models.QueryStatus;

import com.yahoo.elide.core.Path.PathElement;
import com.yahoo.elide.core.filter.FilterPredicate;
import com.yahoo.elide.core.filter.InPredicate;
import com.yahoo.elide.core.filter.LEPredicate;
import com.yahoo.elide.core.filter.expression.AndFilterExpression;
import com.yahoo.elide.core.filter.expression.FilterExpression;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Runnable thread for updating AsyncQueryThread status.
 * beyond the max run time and if not terminated by interrupt process
 * due to app/host crash or restart.
 */
@Slf4j
@Data
@AllArgsConstructor
public class AsyncQueryCleanerThread implements Runnable {

    private int maxRunTimeMinutes;
    private Elide elide;
    private int queryCleanupDays;
    private AsyncQueryDAO asyncQueryDao;
    private DateUtil dateUtil = new DateUtil();

    @Override
    public void run() {
        deleteAsyncQuery();
        timeoutAsyncQuery();
    }

    /**
     * This method deletes the historical queries based on threshold.
     * */
    protected void deleteAsyncQuery() {

        try {
            Date cleanupDate = dateUtil.calculateFilterDate(Calendar.DATE, queryCleanupDays);
            PathElement createdOnPathElement = new PathElement(AsyncAPI.class, Long.class, "createdOn");
            FilterExpression fltDeleteExp = new LEPredicate(createdOnPathElement, cleanupDate);
            asyncQueryDao.deleteAsyncQueryAndResultCollection(fltDeleteExp, AsyncAPI.class);
        } catch (Exception e) {
            log.error("Exception in scheduled cleanup: {}", e);
        }
    }

    /**
     * This method updates the status of long running async query which
     * were interrupted due to host crash/app shutdown to TIMEDOUT.
     * */
    protected void timeoutAsyncQuery() {

        try {
            Date filterDate = dateUtil.calculateFilterDate(Calendar.MINUTE, maxRunTimeMinutes);
            PathElement createdOnPathElement = new PathElement(AsyncAPI.class, Long.class, "createdOn");
            PathElement statusPathElement = new PathElement(AsyncAPI.class, String.class, "status");
            List<QueryStatus> statusList = new ArrayList<QueryStatus>();
            statusList.add(QueryStatus.PROCESSING);
            statusList.add(QueryStatus.QUEUED);
            FilterPredicate inPredicate = new InPredicate(statusPathElement, statusList);
            FilterPredicate lePredicate = new LEPredicate(createdOnPathElement, filterDate);
            AndFilterExpression fltTimeoutExp = new AndFilterExpression(inPredicate, lePredicate);
            asyncQueryDao.updateStatusAsyncQueryCollection(fltTimeoutExp, QueryStatus.TIMEDOUT, AsyncAPI.class);
        } catch (Exception e) {
            log.error("Exception in scheduled cleanup: {}", e);
        }
    }
}
