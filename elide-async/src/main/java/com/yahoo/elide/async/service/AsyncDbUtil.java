/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.service;

import java.io.IOException;
import java.util.UUID;

import javax.inject.Singleton;

import com.yahoo.elide.Elide;
import com.yahoo.elide.async.models.AsyncQuery;
import com.yahoo.elide.async.models.AsyncQueryResult;
import com.yahoo.elide.async.models.QueryStatus;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.request.EntityProjection;

/**
 * Utility class which uses the elide datastore to modify, update and create
 * AsyncQuery and AsyncQueryResult Objects
 */
@Singleton
public class AsyncDbUtil {

    private Elide elide;
    private static AsyncDbUtil asyncUtil;

    protected static AsyncDbUtil getInstance(Elide elide) {
        if (asyncUtil == null) {
            synchronized (AsyncDbUtil.class) {
                asyncUtil = new AsyncDbUtil(elide);
            }
        }
        return asyncUtil;
      }

    protected AsyncDbUtil(Elide elide) {
        this.elide = elide;
    }

    /**
     * This method updates the model for AsyncQuery with passed value.
     * @param updatedAttribute Attribute from asyncQuery which needs to be updated
     * @param asyncQueryId Unique UUID for the AsyncQuery Object
     * @throws IOException IOException from DataStoreTransaction
     * @return AsyncQuery Object
     */
    protected AsyncQuery updateAsyncQuery(Object updatedAttribute, UUID asyncQueryId) throws IOException {
        DataStoreTransaction tx = elide.getDataStore().beginTransaction();
        EntityProjection asyncQueryCollection = EntityProjection.builder()
            .type(AsyncQuery.class)
            .build();
        RequestScope scope = new RequestScope(null, null, tx, null, null, elide.getElideSettings());
        AsyncQuery query = (AsyncQuery) tx.loadObject(asyncQueryCollection, asyncQueryId, scope);
        if (updatedAttribute.getClass().getSimpleName().equals("QueryStatus")) {
            query.setStatus((QueryStatus) updatedAttribute);
        }
        else if (updatedAttribute.getClass().getSimpleName().equals("AsyncQueryResult")) {
            query.setResult((AsyncQueryResult)updatedAttribute);
        }
        tx.save(query, scope);
        tx.commit(scope);
        tx.flush(scope);
        tx.close();
        return query;
    }

    /**
     * This method persists the model for AsyncQueryResult
     * @param status ElideResponse status from AsyncQuery
     * @param responseBody ElideResponse responseBody from AsyncQuery
     * @param asyncQuery AsyncQuery object to be associated with the AsyncQueryResult object
     * @param asyncQueryId UUID of the AsyncQuery to be associated with the AsyncQueryResult object
     * @throws IOException IOException from DataStoreTransaction
     * @return AsyncQueryResult Object
     */
    protected AsyncQueryResult createAsyncQueryResult(Integer status, String responseBody, AsyncQuery asyncQuery, UUID asyncQueryId) throws IOException {
        DataStoreTransaction tx = elide.getDataStore().beginTransaction();

        // Creating new RequestScope for Datastore transaction
        RequestScope scope = new RequestScope(null, null, tx, null, null, elide.getElideSettings());

        AsyncQueryResult asyncQueryResult = new AsyncQueryResult();
        asyncQueryResult.setStatus(status);
        asyncQueryResult.setResponseBody(responseBody);
        asyncQueryResult.setContentLength(responseBody.length());
        asyncQueryResult.setId(asyncQueryId);
        asyncQueryResult.setQuery(asyncQuery);
        tx.createObject(asyncQueryResult, scope);
        tx.save(asyncQueryResult, scope);
        tx.commit(scope);
        tx.flush(scope);
        tx.close();
        return asyncQueryResult;
    }

}
