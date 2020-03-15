/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.service;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.UUID;

import javax.inject.Singleton;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import com.yahoo.elide.Elide;
import com.yahoo.elide.async.models.AsyncQuery;
import com.yahoo.elide.async.models.AsyncQueryResult;
import com.yahoo.elide.async.models.QueryStatus;
import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.jsonapi.models.JsonApiDocument;
import com.yahoo.elide.request.EntityProjection;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility class which implements AsyncQueryDAO
 */
@Singleton
@Slf4j
public class DefaultAsyncQueryDAO implements AsyncQueryDAO {

    @Setter private Elide elide;
    @Setter private DataStore dataStore;

    // Default constructor is needed for standalone implementation for override in getAsyncQueryDao
    public DefaultAsyncQueryDAO() {
    }

    public DefaultAsyncQueryDAO(Elide elide, DataStore dataStore) {
    	this.elide = elide;
    	this.dataStore = dataStore;
    }

    @Override
    public AsyncQuery updateStatus(UUID asyncQueryId, QueryStatus status) {
        return updateAsyncQuery(asyncQueryId, (asyncQueryObj) -> {
            asyncQueryObj.setStatus(status);
        });
    }

    /**
     * This method updates the model for AsyncQuery with passed value.
     * @param asyncQueryId Unique UUID for the AsyncQuery Object
     * @param updateFunction Functional interface for updating AsyncQuery Object
     * @return AsyncQuery Object
     */
    private AsyncQuery updateAsyncQuery(UUID asyncQueryId, UpdateQuery updateFunction) {
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
    public Collection<AsyncQuery> updateStatusAsyncQueryCollection(Collection<AsyncQuery> asyncQueryList, QueryStatus status) {
        return updateAsyncQueryCollection(asyncQueryList, (asyncQuery) -> {
            asyncQuery.setStatus(status);
            });
    }

    /**
     * This method updates a collection of AsyncQuery objects from database and
     * returns the objects updated.
     * @param asyncQueryList Iterable list of AsyncQuery objects to be updated
     * @return query object list updated
     */
    private Collection<AsyncQuery> updateAsyncQueryCollection(Collection<AsyncQuery> asyncQueryList, UpdateQuery updateFunction) {
        log.debug("updateAsyncQueryCollection");
        executeInTransaction(dataStore, (tx, scope) -> {
            EntityProjection asyncQueryCollection = EntityProjection.builder()
                    .type(AsyncQuery.class)
                    .build();

            Iterator<AsyncQuery> itr = asyncQueryList.iterator();
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
    public Collection<AsyncQuery> deleteAsyncQueryAndResultCollection(Collection<AsyncQuery> asyncQueryList) {
        log.debug("deleteAsyncQueryAndResultCollection");
        executeInTransaction(dataStore, (tx, scope) -> {
            EntityProjection asyncQueryCollection = EntityProjection.builder()
                    .type(AsyncQuery.class)
                    .build();

            Iterator<AsyncQuery> itr = asyncQueryList.iterator();

            while(itr.hasNext()) {
                AsyncQuery query = (AsyncQuery) itr.next();
                AsyncQuery asyncQuery = (AsyncQuery) tx.loadObject(asyncQueryCollection, query.getId(), scope);
                if(asyncQuery != null) {
                    tx.delete(asyncQuery, scope);
                }
            }

            return asyncQueryList;
        });
        return (Collection<AsyncQuery>) asyncQueryList;
    }

    @Override
    public AsyncQueryResult createAsyncQueryResult(Integer status, String responseBody, AsyncQuery asyncQuery, UUID asyncQueryId) {
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

    @SuppressWarnings("unchecked")
    public Collection<AsyncQuery> loadQueries(String filterExpression) {
        EntityDictionary dictionary = elide.getElideSettings().getDictionary();
        RSQLFilterDialect filterParser = new RSQLFilterDialect(dictionary);

        Collection<AsyncQuery> loaded = (Collection<AsyncQuery>) executeInTransaction(dataStore, (tx, scope) -> {
            try {
                FilterExpression filter = filterParser.parseFilterExpression(filterExpression, AsyncQuery.class, false);

                EntityProjection asyncQueryCollection = EntityProjection.builder()
                        .type(AsyncQuery.class)
                        .filterExpression(filter)
                        .build();

                Iterable<Object> loadedObj = tx.loadObjects(asyncQueryCollection, scope);
                return loadedObj;
            } catch (Exception e) {
                log.error("Exception: {}", e);
            }
            return null;
        });
        return loaded;
    }

    /**
     * This method creates a transaction from the datastore, performs the DB action using
     * a generic functional interface and closes the transaction.
     * @param dataStore Elide datastore retrieved from Elide object
     * @param action Functional interface to perform DB action
     * @return Object Returns Entity Object (AsyncQueryResult or AsyncResult)
     */
    protected Object executeInTransaction(DataStore dataStore, Transactional action) {
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
