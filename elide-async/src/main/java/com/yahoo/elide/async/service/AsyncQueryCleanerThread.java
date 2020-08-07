/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.service;

import com.yahoo.elide.Elide;
import com.yahoo.elide.async.models.QueryStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Calendar;

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

    @Override
    public void run() {
        deleteAsyncQuery();
        timeoutAsyncQuery();
    }

    /**
     * This method deletes the historical queries based on threshold.
     * */
    @SuppressWarnings("unchecked")
    protected void deleteAsyncQuery() {

        String cleanupDateFormatted = DateFilterUtil.evaluateFormattedFilterDate(elide,
                Calendar.DATE, queryCleanupDays);

        String filterExpression = "createdOn=le='" + cleanupDateFormatted + "'";
        asyncQueryDao.deleteAsyncQueryAndResultCollection(filterExpression);

    }

    /**
     * This method updates the status of long running async query which
     * were interrupted due to host crash/app shutdown to TIMEDOUT.
     * */
    @SuppressWarnings("unchecked")
    protected void timeoutAsyncQuery() {

        String filterDateFormatted = DateFilterUtil.evaluateFormattedFilterDate(elide,
                Calendar.MINUTE, maxRunTimeMinutes);
        String filterExpression = "status=in=(" + QueryStatus.PROCESSING.toString() + ","
                + QueryStatus.QUEUED.toString() + ");createdOn=le='" + filterDateFormatted + "'";
        asyncQueryDao.updateStatusAsyncQueryCollection(filterExpression, QueryStatus.TIMEDOUT);
    }
}
