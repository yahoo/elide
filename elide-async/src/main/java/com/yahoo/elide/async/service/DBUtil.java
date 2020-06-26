/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.async.service;

import static com.yahoo.elide.core.EntityDictionary.NO_VERSION;

import com.yahoo.elide.Elide;
import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.jsonapi.models.JsonApiDocument;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

/**
 * Utility class which implements a static method executeInTransaction.
 */
@Slf4j
public class DBUtil {

    /**
     * This method creates a transaction from the datastore, performs the DB action using
     * a generic functional interface and closes the transaction.
     * @param dataStore Elide datastore retrieved from Elide object
     * @param action Functional interface to perform DB action
     * @return Object Returns Entity Object (AsyncQueryResult or AsyncResult)
     */
    protected static Object executeInTransaction(Elide elide, DataStore dataStore, Transactional action) {
        log.debug("executeInTransaction");
        Object result = null;
        try (DataStoreTransaction tx = dataStore.beginTransaction()) {
            JsonApiDocument jsonApiDoc = new JsonApiDocument();
            MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<String, String>();
            RequestScope scope = new RequestScope("query", NO_VERSION, jsonApiDoc,
                    tx, null, queryParams, elide.getElideSettings());
            result = action.execute(tx, scope);
            tx.flush(scope);
            tx.commit(scope);
        } catch (IOException e) {
            log.error("IOException: {}", e);
            throw new IllegalStateException(e);
        }
        return result;
    }
}
