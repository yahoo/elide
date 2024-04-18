/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.async.integration.tests;

import com.paiondata.elide.core.RequestScope;
import com.paiondata.elide.core.datastore.DataStoreIterable;
import com.paiondata.elide.core.datastore.DataStoreTransaction;
import com.paiondata.elide.core.datastore.wrapped.TransactionWrapper;
import com.paiondata.elide.core.request.EntityProjection;
import org.apache.commons.collections4.CollectionUtils;

import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
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
    public <T> DataStoreIterable<T> loadObjects(EntityProjection entityProjection, RequestScope scope) {
        try {
            log.debug("LoadObjects Sleep for delay test");

            List<String> sleepTime = scope.getRoute().getHeaders().getOrDefault("sleep", Collections.emptyList());
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
