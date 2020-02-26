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

import lombok.extern.slf4j.Slf4j;

/**
 * Runnable thread for updating AsyncQueryThread status
 * beyond the max run time and if not terminated by interrupt process
 * due to app/host crash or restart.
 */
@Slf4j
public class AsyncQueryCleanerThread implements Runnable {

    private int maxRunTime;
    private Elide elide;

    AsyncQueryCleanerThread(int maxRunTime, Elide elide) {
        log.debug("New Async Query Cleaner thread created");
        this.maxRunTime = maxRunTime;
        this.elide = elide;
    }

    @Override
    public void run() {
        timeoutAsyncQuery();
    }

    /**
     * This method updates the status of long running async query which
     * were not interrupted due to host crash/app shutdown to TIMEDOUT.
     * */
    private void timeoutAsyncQuery() {
        DataStoreTransaction tx = elide.getDataStore().beginTransaction();

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
            while(itr.hasNext()) {
                AsyncQuery query = (AsyncQuery) itr.next();
                long differenceInMillies = Math.abs((new Date()).getTime() - query.getCreatedOn().getTime());
                long difference = TimeUnit.MINUTES.convert(differenceInMillies, TimeUnit.MILLISECONDS);

                // Check if its twice as long as max run time. It means the host/app crashed or restarted.
                if(difference > maxRunTime * 2) {
                    log.info("Updating Async Query Status to TIMEDOUT");
                    query.setQueryStatus(QueryStatus.TIMEDOUT);
                    tx.save(query, scope);
                    tx.commit(scope);
                    tx.flush(scope);
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
}