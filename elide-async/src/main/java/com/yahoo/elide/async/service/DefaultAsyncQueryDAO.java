/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.service;

import java.io.IOException;
import java.util.Iterator;
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
 * Utility class which implements AsyncQueryDAO
 */
@Singleton
@Slf4j
public class DefaultAsyncQueryDAO implements AsyncQueryDAO {

    private Elide elide;
    private DataStore dataStore;

    public DefaultAsyncQueryDAO() {}

    public DefaultAsyncQueryDAO(Elide elide, DataStore dataStore) {
    	this.elide = elide;
    	this.dataStore = dataStore;
    }

    @Override
    public void setElide(Elide elide) {
        this.elide = elide;
    }

    @Override
    public void setDataStore(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    @Override
    public AsyncQuery updateAsyncQuery(UUID asyncQueryId, UpdateQuery updateFunction) {
        log.debug("updateAsyncQuery");
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

    @Override
    public Iterable<Object> updateAsyncQueryCollection(Iterable<Object> asyncQueryList, UpdateQuery updateFunction) {
        log.debug("updateAsyncQueryCollection");
        executeInTransaction(dataStore, (tx, scope) -> {
            EntityProjection asyncQueryCollection = EntityProjection.builder()
                    .type(AsyncQuery.class)
                    .build();

            Iterator<Object> itr = asyncQueryList.iterator();
            while(itr.hasNext()) {
                AsyncQuery query = (AsyncQuery) itr.next();
                AsyncQuery asyncQuery = (AsyncQuery) tx.loadObject(asyncQueryCollection, query.getId(), scope);
                updateFunction.update(asyncQuery);
                tx.save(asyncQuery, scope);
            }
            return asyncQueryList;
        });
        return asyncQueryList;
    }

    @Override
    public Iterable<Object> deleteAsyncQueryAndResultCollection(Iterable<Object> asyncQueryList) {
        log.debug("deleteAsyncQueryAndResultCollection");
        executeInTransaction(dataStore, (tx, scope) -> {
            EntityProjection asyncQueryCollection = EntityProjection.builder()
                    .type(AsyncQuery.class)
                    .build();

            Iterator<Object> itr = asyncQueryList.iterator();

            while(itr.hasNext()) {
                AsyncQuery query = (AsyncQuery) itr.next();
                AsyncQuery asyncQuery = (AsyncQuery) tx.loadObject(asyncQueryCollection, query.getId(), scope);
                if(asyncQuery != null) {
                    tx.delete(asyncQuery, scope);
                }
            }

            return asyncQueryList;
        });
        return asyncQueryList;
    }

    @Override
    public AsyncQueryResult setAsyncQueryAndResult(Integer status, String responseBody, AsyncQuery asyncQuery, UUID asyncQueryId) {
        log.debug("createAsyncQueryResult");
        AsyncQueryResult queryResultObj = (AsyncQueryResult) executeInTransaction(dataStore, (tx, scope) -> {
            AsyncQueryResult asyncQueryResult = new AsyncQueryResult();
            asyncQueryResult.setStatus(status);
            asyncQueryResult.setResponseBody(responseBody);
            asyncQueryResult.setContentLength(responseBody.length());
            asyncQueryResult.setQuery(asyncQuery);
            asyncQueryResult.setId(asyncQueryId);
            asyncQuery.setResult(asyncQueryResult);
            tx.createObject(asyncQueryResult, scope);
            tx.save(asyncQuery, scope);
            return asyncQueryResult;
        });
        return queryResultObj;
    }

    @Override
    public Object executeInTransaction(DataStore dataStore, Transactional action) {
        log.debug("executeInTransaction");
        Object result = null;
        try (DataStoreTransaction tx = dataStore.beginTransaction()) {
	        JsonApiDocument jsonApiDoc = new JsonApiDocument();
	        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<String, String>();
	        RequestScope scope = new RequestScope("query", jsonApiDoc, tx, null, queryParams, elide.getElideSettings());
            result = action.execute(tx, scope);
            tx.commit(scope);
            tx.flush(scope);
        } catch (IOException e) {
            log.error("IOException: {}", e);
        } catch (Exception e) {
            log.error("Exception: {}", e);
        }
        return result;
    }

}
