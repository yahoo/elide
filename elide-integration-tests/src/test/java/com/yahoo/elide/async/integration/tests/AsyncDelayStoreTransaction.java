/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.async.integration.tests;

import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.datastore.wrapped.TransactionWrapper;
import com.yahoo.elide.core.request.EntityProjection;
import lombok.extern.slf4j.Slf4j;

/**
 * Data Store Transaction that wraps another transaction and provides delay for testing Async queries.
 */
@Slf4j
public class AsyncDelayStoreTransaction extends TransactionWrapper {

    private Integer testDelay;
    protected static Boolean sleep = false;

    public AsyncDelayStoreTransaction(DataStoreTransaction tx, Integer testDelay) {

        super(tx);
        this.testDelay = testDelay;
    }
    @Override
    public Iterable<Object> loadObjects(EntityProjection entityProjection, RequestScope scope) {
        try {
            log.debug("LoadObjects Sleep for delay test");
            if (sleep) {
                Thread.sleep(testDelay);
            }
        } catch (InterruptedException e) {
            log.debug("Test delay interrupted");
        }
        return super.loadObjects(entityProjection, scope);
    }
}
