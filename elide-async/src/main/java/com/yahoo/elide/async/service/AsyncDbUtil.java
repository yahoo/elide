/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.service;

import java.io.IOException;
import java.util.UUID;

import javax.inject.Singleton;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import com.yahoo.elide.Elide;
import com.yahoo.elide.async.models.AsyncQuery;
import com.yahoo.elide.async.models.AsyncQueryResult;
import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.jsonapi.models.JsonApiDocument;
import com.yahoo.elide.request.EntityProjection;

import lombok.extern.slf4j.Slf4j;

/**
 * Utility class which uses the elide datastore to modify, update and create
 * AsyncQuery and AsyncQueryResult Objects
 */
@Singleton
@Slf4j
public class AsyncDbUtil {

    private Elide elide;
    private static AsyncDbUtil asyncUtil;
    private DataStore dataStore;

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
        this.dataStore = elide.getDataStore();
    }

    /**
     * This method updates the model for AsyncQuery with passed value.
     * @param asyncQueryId Unique UUID for the AsyncQuery Object
     * @param updateFunction Functional interface for updating AsyncQuery Object
     * @return AsyncQuery Object
     */
    protected AsyncQuery updateAsyncQuery(UUID asyncQueryId, UpdateQuery updateFunction) {
        log.debug("AsyncDbUtil updateAsyncQuery");
        AsyncQuery queryObj = (AsyncQuery) executeInTransaction(dataStore, (tx, scope) -> {
            EntityProjection asyncQueryCollection = EntityProjection.builder()
                    .type(AsyncQuery.class)
                    .build();
            AsyncQuery query = (AsyncQuery) tx.loadObject(asyncQueryCollection, asyncQueryId, scope);
            updateFunction.update(query);
            tx.save(query, scope);
            return query;
        });
        return queryObj;
    }

    /**
     * This method deletes the AsyncQuery object from database.
     * @param asyncQueryId Unique UUID for the AsyncQuery Object
     */
    protected void deleteAsyncQuery(UUID asyncQueryId) {
        log.debug("AsyncDbUtil deleteAsyncQuery");
        executeInTransaction(dataStore, (tx, scope) -> {
            EntityProjection asyncQueryCollection = EntityProjection.builder()
                    .type(AsyncQuery.class)
                    .build();
            AsyncQuery query = (AsyncQuery) tx.loadObject(asyncQueryCollection, asyncQueryId, scope);
            if(query != null) {
                tx.delete(query, scope);
            }
            return query;
        });
    }

    /**
     * This method deletes the AsyncQueryResult object from database.
     * @param asyncQueryResultId Unique UUID for the AsyncQuery Object
     */
    protected void deleteAsyncQueryResult(UUID asyncQueryResultId) {
        log.debug("AsyncDbUtil deleteAsyncQueryResult");
        executeInTransaction(dataStore, (tx, scope) -> {
            EntityProjection asyncQueryResultCollection = EntityProjection.builder()
                    .type(AsyncQueryResult.class)
                    .build();
            AsyncQueryResult queryResult = (AsyncQueryResult) tx.loadObject(asyncQueryResultCollection, asyncQueryResultId, scope);
            if(queryResult != null) {
                tx.delete(queryResult, scope);
            }
            return queryResult;
        });
    }

    /**
     * This method persists the model for AsyncQueryResult
     * @param status ElideResponse status from AsyncQuery
     * @param responseBody ElideResponse responseBody from AsyncQuery
     * @param asyncQuery AsyncQuery object to be associated with the AsyncQueryResult object
     * @param asyncQueryId UUID of the AsyncQuery to be associated with the AsyncQueryResult object
     * @return AsyncQueryResult Object
     */
    protected AsyncQueryResult createAsyncQueryResult(Integer status, String responseBody, AsyncQuery asyncQuery, UUID asyncQueryId) {
        log.debug("AsyncDbUtil createAsyncQueryResult");
        AsyncQueryResult queryResultObj = (AsyncQueryResult) executeInTransaction(dataStore, (tx, scope) -> {
            AsyncQueryResult asyncQueryResult = new AsyncQueryResult();
            asyncQueryResult.setStatus(status);
            asyncQueryResult.setResponseBody(responseBody);
            asyncQueryResult.setContentLength(responseBody.length());
            asyncQueryResult.setId(asyncQueryId);
            asyncQueryResult.setQuery(asyncQuery);
            tx.createObject(asyncQueryResult, scope);
            return asyncQueryResult;
        });
        return queryResultObj;
    }

    /**
     * This method creates a transaction from the datastore, performs the DB action using
     * a generic functional interface and closes the transaction.
     * @param dataStore Elide datastore retrieved from Elide object
     * @param action Functional interface to perform DB action
     * @return Object Returns Entity Object (AsyncQueryResult or AsyncResult)
     */
    public Object executeInTransaction(DataStore dataStore, Transactional action) {
        log.debug("executeInTransaction");
        DataStoreTransaction tx = dataStore.beginTransaction();
        JsonApiDocument jsonApiDoc = new JsonApiDocument();
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<String, String>();
        RequestScope scope = new RequestScope("query", jsonApiDoc, tx, null, queryParams, elide.getElideSettings());
        Object result = null;
        try {
            result = action.execute(tx, scope);
            tx.commit(scope);
            tx.flush(scope);
        } catch (Exception e) {
            log.error("Exception: {}", e.getMessage());
        } finally {
            // Finally block to close a transaction incase of DB Exceptions
            try {
                tx.close();
            } catch (IOException e) {
                log.error("IOException: {}", e.getMessage());
            }
        }
        return result;
    }

}
