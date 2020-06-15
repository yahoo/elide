/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.async.integration.tests;

import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.DataStoreTransactionImplementation;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.request.EntityProjection;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
/**
 * Data Store Transaction that wraps another transaction and provides delay for testing Async queries.
 */
@Slf4j
public class AsyncDelayStoreTransaction extends DataStoreTransactionImplementation implements DataStoreTransaction {

    private final DataStoreTransaction tx;

    public AsyncDelayStoreTransaction(DataStoreTransaction tx) {
            this.tx = tx;
    }
    @Override
    public void close() throws IOException {
        tx.close();
    }

    @Override
    public void save(Object entity, RequestScope scope) {
        tx.save(entity, scope);

    }

    @Override
    public void delete(Object entity, RequestScope scope) {
        tx.delete(entity, scope);
    }

    @Override
    public void flush(RequestScope scope) {
        tx.flush(scope);
    }

    @Override
    public void commit(RequestScope scope) {
        tx.commit(scope);
    }

    @Override
    public void createObject(Object entity, RequestScope scope) {
        tx.createObject(entity, scope);
    }
    @Override
    public Iterable<Object> loadObjects(EntityProjection entityProjection, RequestScope scope) {
        try {
            log.debug("LoadObjects Sleep for delay test");
            if (entityProjection.getType().toString().trim().equals("class example.Book")) {
                Thread.sleep(5000);
            }
        } catch (InterruptedException e) {
            log.debug("Test delay interrupted");
        }
        return tx.loadObjects(entityProjection, scope);
    }
}
