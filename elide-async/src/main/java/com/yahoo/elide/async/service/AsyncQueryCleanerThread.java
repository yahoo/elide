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
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

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
        AsyncDbUtil asyncDbUtil = AsyncDbUtil.getInstance(elide);
        Iterable<Object> loaded = (Iterable<Object>) asyncDbUtil.executeInTransaction(elide.getDataStore(), (tx, scope) -> {
            try {
                EntityDictionary dictionary = elide.getElideSettings().getDictionary();
                RSQLFilterDialect filterParser = new RSQLFilterDialect(dictionary);

                //Calculate date to clean up
                Calendar cal = Calendar.getInstance();
                cal.setTime(new Date());
                cal.add(Calendar.DATE, -(queryCleanupDays));
                Date cleanupDate = cal.getTime();
                Format dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
                String cleanupDateFormatted = dateFormat.format(cleanupDate);
                log.debug("cleanupDateFormatted = {}", cleanupDateFormatted);

                FilterExpression filter = filterParser.parseFilterExpression("createdOn=le='" + cleanupDateFormatted + "'",
                        AsyncQuery.class, false);
                log.debug("filter = {}", filter.toString());

                EntityProjection asyncQueryCollection = EntityProjection.builder()
                        .type(AsyncQuery.class)
                        .filterExpression(filter)
                        .build();

                Iterable<Object> loadedObjects = tx.loadObjects(asyncQueryCollection, scope);
                return loadedObjects;
            } catch (Exception e) {
                log.error("Exception: {}", e.getMessage());
            }
            return null;
        });
        Iterator<Object> itr = loaded.iterator();
        while(itr.hasNext()) {
            AsyncQuery query = (AsyncQuery) itr.next();

            log.info("Found a query to DELETE");
            asyncDbUtil.deleteAsyncQueryAndResult(query.getId());
        }
    }
    
    /**
     * This method updates the status of long running async query which
     * were not interrupted due to host crash/app shutdown to TIMEDOUT.
     * */
	@SuppressWarnings("unchecked")
    private void timeoutAsyncQuery() {
        AsyncDbUtil asyncDbUtil = AsyncDbUtil.getInstance(elide);
        Iterable<Object> loaded = (Iterable<Object>) asyncDbUtil.executeInTransaction(elide.getDataStore(), (tx, scope) -> {
            try {
                EntityDictionary dictionary = elide.getElideSettings().getDictionary();
                RSQLFilterDialect filterParser = new RSQLFilterDialect(dictionary);
                FilterExpression filter = filterParser.parseFilterExpression("status=in=(" + QueryStatus.PROCESSING.toString() + ","
                        + QueryStatus.QUEUED.toString() + ")", AsyncQuery.class, false);

                EntityProjection asyncQueryCollection = EntityProjection.builder()
                        .type(AsyncQuery.class)
                        .filterExpression(filter)
                        .build();

                Iterable<Object> loadedObj = tx.loadObjects(asyncQueryCollection, scope);
                return loadedObj;
            } catch (Exception e) {
                log.error("Exception: {}", e.getMessage());
            }
            return null;
        });
        Iterator<Object> itr = loaded.iterator();
        long currentTime = new Date().getTime();
        while(itr.hasNext()) {
            AsyncQuery query = (AsyncQuery) itr.next();

            if(isTimedOut(currentTime, query)) {
                log.info("Updating Async Query Status to TIMEDOUT");
                asyncDbUtil.updateAsyncQuery(query.getId(), (asyncQueryObj) -> {
                    asyncQueryObj.setStatus(QueryStatus.TIMEDOUT);
                    });
            }
        }
    }

    private boolean isTimedOut(long currentTime, AsyncQuery query) {
        long differenceMillies = Math.abs(currentTime - query.getCreatedOn().getTime());
        long differenceMinutes = TimeUnit.MINUTES.convert(differenceMillies, TimeUnit.MILLISECONDS);

        // Check if its twice as long as max run time. It means the host/app crashed or restarted.
        return (differenceMinutes > maxRunTimeMinutes * 2);
    }
}