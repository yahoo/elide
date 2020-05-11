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

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

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

        String cleanupDateFormatted = evaluateFormattedFilterDate(Calendar.DATE, queryCleanupDays);

        String filterExpression = "createdOn=le='" + cleanupDateFormatted + "'";

        asyncQueryDao.deleteAsyncQueryAndResultCollection(filterExpression);

    }

    /**
     * This method updates the status of long running async query which
     * were interrupted due to host crash/app shutdown to TIMEDOUT.
     * */
    @SuppressWarnings("unchecked")
    protected void timeoutAsyncQuery() {

        String filterDateFormatted = evaluateFormattedFilterDate(Calendar.MINUTE, maxRunTimeMinutes);
        String filterExpression = "status=in=(" + QueryStatus.PROCESSING.toString() + ","
                + QueryStatus.QUEUED.toString() + ");createdOn=le='" + filterDateFormatted + "'";

        asyncQueryDao.updateStatusAsyncQueryCollection(filterExpression, QueryStatus.TIMEDOUT);
    }

    /**
     * Evaluates and subtracts the amount based on the calendar unit and amount from current date.
     * @param calendarUnit Enum such as Calendar.DATE or Calendar.MINUTE
     * @param amount Amount of days to be subtracted from current time
     * @return formatted filter date
     */
     private String evaluateFormattedFilterDate(int calendarUnit, int amount) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(calendarUnit, -(amount));
        Date filterDate = cal.getTime();
        Format dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
        String filterDateFormatted = dateFormat.format(filterDate);
        log.debug("FilterDateFormatted = {}", filterDateFormatted);
        return filterDateFormatted;
    }
}
