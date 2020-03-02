/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.service;

import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import com.yahoo.elide.Elide;
import com.yahoo.elide.async.models.AsyncQuery;
import com.yahoo.elide.async.models.QueryStatus;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.RequestScope;
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

    @Override
    public void run() {
    	deleteAsyncQuery();
        timeoutAsyncQuery();
    }

    /**
     * This method updates the status of long running async query which
     * were not interrupted due to host crash/app shutdown to TIMEDOUT.
     * */
    private void deleteAsyncQuery() {
        DataStoreTransaction tx = elide.getDataStore().beginTransaction();
        AsyncDbUtil asyncDbUtil = AsyncDbUtil.getInstance(elide);
        try {
            EntityDictionary dictionary = elide.getElideSettings().getDictionary();
            RSQLFilterDialect filterParser = new RSQLFilterDialect(dictionary);
            RequestScope scope = new RequestScope(null, null, tx, null, null, elide.getElideSettings());

            FilterExpression filter = filterParser.parseFilterExpression("createdOn=le=" + new Date() , AsyncQuery.class, false);
            log.debug("filter = {}", filter.toString());

            EntityProjection asyncQueryCollection = EntityProjection.builder()
                    .type(AsyncQuery.class)
                    .filterExpression(filter)
                    .build();

            Iterable<Object> loaded = tx.loadObjects(asyncQueryCollection, scope);
            Iterator<Object> itr = loaded.iterator();
            long currentTime = new Date().getTime();
            while(itr.hasNext()) {
                AsyncQuery query = (AsyncQuery) itr.next();

                if(isTimedOut(currentTime, query)) {
                    log.info("Updating Async Query Status to DELETE");
                    asyncDbUtil.updateAsyncQuery(QueryStatus.TIMEDOUT, query.getId());
                }
            }
        }
        catch (Exception e) {
            log.error("Exception: {}", e.getMessage());
        }
        finally {
            try {
                tx.close();
            } catch (IOException e) {
                log.error("IOException: {}", e.getMessage());
            }
        }
    }
    
    /**
     * This method updates the status of long running async query which
     * were not interrupted due to host crash/app shutdown to TIMEDOUT.
     * */
    private void timeoutAsyncQuery() {
        DataStoreTransaction tx = elide.getDataStore().beginTransaction();
        AsyncDbUtil asyncDbUtil = AsyncDbUtil.getInstance(elide);
        try {
            EntityDictionary dictionary = elide.getElideSettings().getDictionary();
            RSQLFilterDialect filterParser = new RSQLFilterDialect(dictionary);
            RequestScope scope = new RequestScope(null, null, tx, null, null, elide.getElideSettings());

            FilterExpression filter = filterParser.parseFilterExpression("status=in=(" + QueryStatus.PROCESSING.toString() + ","
                + QueryStatus.QUEUED.toString() + ")", AsyncQuery.class, false);

            EntityProjection asyncQueryCollection = EntityProjection.builder()
                    .type(AsyncQuery.class)
                    .filterExpression(filter)
                    .build();

            Iterable<Object> loaded = tx.loadObjects(asyncQueryCollection, scope);
            Iterator<Object> itr = loaded.iterator();
            long currentTime = new Date().getTime();
            while(itr.hasNext()) {
                AsyncQuery query = (AsyncQuery) itr.next();

                if(isTimedOut(currentTime, query)) {
                    log.info("Updating Async Query Status to TIMEDOUT");
                    asyncDbUtil.updateAsyncQuery(QueryStatus.TIMEDOUT, query.getId());
                }
            }
        }
        catch (Exception e) {
            log.error("Exception: {}", e.getMessage());
        }
        finally {
            try {
                tx.close();
            } catch (IOException e) {
                log.error("IOException: {}", e.getMessage());
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