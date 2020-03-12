/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.service;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import com.yahoo.elide.Elide;
import com.yahoo.elide.async.models.AsyncQuery;
import com.yahoo.elide.async.models.QueryStatus;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.request.EntityProjection;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Runnable thread for updating AsyncQueryThread status
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
     * This method updates the status of long running async query which
     * were not interrupted due to host crash/app shutdown to TIMEDOUT.
     * */
    @SuppressWarnings("unchecked")
    private void deleteAsyncQuery() {

        String cleanupDateFormatted = evaluateFormattedFilterDate(Calendar.DATE, queryCleanupDays);

        String filterExpression = "createdOn=le='" + cleanupDateFormatted + "'";

        Iterable<Object> loaded = getFilteredResults(filterExpression);

        asyncQueryDao.deleteAsyncQueryAndResultCollection(loaded);

    }
    
    /**
     * This method updates the status of long running async query which
     * were not interrupted due to host crash/app shutdown to TIMEDOUT.
     * */
	@SuppressWarnings("unchecked")
    private void timeoutAsyncQuery() {

        String filterDateFormatted = evaluateFormattedFilterDate(Calendar.MINUTE, maxRunTimeMinutes);
        String filterExpression = "status=in=(" + QueryStatus.PROCESSING.toString() + ","
                + QueryStatus.QUEUED.toString() + ");createdOn=le='" + filterDateFormatted + "'";

        Iterable<Object> loaded = getFilteredResults(filterExpression);

        asyncQueryDao.updateAsyncQueryCollection(loaded, (asyncQuery) -> {
            asyncQuery.setStatus(QueryStatus.TIMEDOUT);
            });
    }

    /**
     * This method uses the filter expression to evaluate a list of filtered results based on the expression
     * @param filterExpression filter expression for filtering from datastore
     * @return filtered results
     */
	@SuppressWarnings("unchecked")
	private Iterable<Object> getFilteredResults(String filterExpression) {
        EntityDictionary dictionary = elide.getElideSettings().getDictionary();
        RSQLFilterDialect filterParser = new RSQLFilterDialect(dictionary);

        Iterable<Object> loaded = (Iterable<Object>) asyncQueryDao.executeInTransaction(elide.getDataStore(), (tx, scope) -> {
            try {
                FilterExpression filter = filterParser.parseFilterExpression(filterExpression, AsyncQuery.class, false);

                EntityProjection asyncQueryCollection = EntityProjection.builder()
                        .type(AsyncQuery.class)
                        .filterExpression(filter)
                        .build();

                Iterable<Object> loadedObj = tx.loadObjects(asyncQueryCollection, scope);
                return loadedObj;
            } catch (Exception e) {
                log.error("Exception: {}", e);
            }
            return null;
        });
        return loaded;
	}

    /**
     * Evaluates and subtracts the amount based on the calendar unit and amount from current date
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