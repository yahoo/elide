/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.async.integration.tests;

import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.datastore.DataStoreIterable;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.datastore.wrapped.TransactionWrapper;
import com.yahoo.elide.core.request.EntityProjection;
import org.apache.commons.collections4.CollectionUtils;

import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Data Store Transaction that wraps another transaction and provides delay for testing Async queries.
 */
@Slf4j
public class AsyncDelayStoreTransaction extends TransactionWrapper {

    public AsyncDelayStoreTransaction(DataStoreTransaction tx) {

        super(tx);
    }

    @Override
    public DataStoreIterable<Object> loadObjects(EntityProjection entityProjection, RequestScope scope) {
        try {
            log.debug("LoadObjects Sleep for delay test");

            List<String> sleepTime = scope.getRequestHeaders().get("sleep");
            if (CollectionUtils.isNotEmpty(sleepTime)) {
                Thread.sleep(Integer.parseInt(sleepTime.get(0)));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.debug("Test delay interrupted");
        }
        return super.loadObjects(entityProjection, scope);
    }
}
