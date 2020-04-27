/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.service;

import com.yahoo.elide.Elide;
import com.yahoo.elide.async.models.AsyncQuery;
import com.yahoo.elide.async.models.AsyncQueryResult;
import com.yahoo.elide.async.models.QueryStatus;
import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.filter.dialect.ParseException;
import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.jsonapi.models.JsonApiDocument;
import com.yahoo.elide.request.EntityProjection;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.UUID;

import javax.inject.Singleton;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

/**
 * Utility class which implements AsyncQueryDAO.
 */
@Singleton
@Slf4j
@Getter
public class DefaultAsyncQueryDAO implements AsyncQueryDAO {

    @Setter private Elide elide;
    @Setter private DataStore dataStore;
    @Setter private EntityDictionary dictionary;
    @Setter private RSQLFilterDialect filterParser;

    // Default constructor is needed for standalone implementation for override in getAsyncQueryDao
    public DefaultAsyncQueryDAO() {
    }

    public DefaultAsyncQueryDAO(Elide elide, DataStore dataStore) {
        this.elide = elide;
        this.dataStore = dataStore;
        dictionary = elide.getElideSettings().getDictionary();
        filterParser = new RSQLFilterDialect(dictionary);
    }

    @Override
    public AsyncQuery updateStatus(AsyncQuery asyncQuery, QueryStatus status) {
        return updateAsyncQuery(asyncQuery, (asyncQueryObj) -> {
            asyncQueryObj.setStatus(status);
        });
    }

    /**
     * This method updates the model for AsyncQuery with passed value.
     * @param asyncQuery The AsyncQuery Object which will be updated
     * @param updateFunction Functional interface for updating AsyncQuery Object
     * @return AsyncQuery Object
     */
    private AsyncQuery updateAsyncQuery(AsyncQuery asyncQuery, UpdateQuery updateFunction) {
        log.debug("updateAsyncQuery");
        AsyncQuery queryObj = (AsyncQuery) executeInTransaction(dataStore, (tx, scope) -> {
            updateFunction.update(asyncQuery);
            tx.save(asyncQuery, scope);
            return asyncQuery;
        });
        return queryObj;
    }

    @Override
    public Collection<AsyncQuery> updateStatusAsyncQueryCollection(String filterExpression,
            QueryStatus status) {
        return updateAsyncQueryCollection(filterExpression, (asyncQuery) -> {
            asyncQuery.setStatus(status);
            });
    }

    /**
     * This method updates a collection of AsyncQuery objects from database and
     * returns the objects updated.
     * @param filterExpression Filter expression to update AsyncQuery Objects based on
     * @param updateFunction Functional interface for updating AsyncQuery Object
     * @return query object list updated
     */
    @SuppressWarnings("unchecked")
    private Collection<AsyncQuery> updateAsyncQueryCollection(String filterExpression,
            UpdateQuery updateFunction) {
        log.debug("updateAsyncQueryCollection");

        Collection<AsyncQuery> asyncQueryList = null;

        try {
             FilterExpression filter = filterParser.parseFilterExpression(filterExpression,
                    AsyncQuery.class, false);
             asyncQueryList = (Collection<AsyncQuery>) executeInTransaction(dataStore,
                        (tx, scope) -> {
                 EntityProjection asyncQueryCollection = EntityProjection.builder()
                         .type(AsyncQuery.class)
                         .filterExpression(filter)
                         .build();

                 Iterable<Object> loaded = tx.loadObjects(asyncQueryCollection, scope);
                 Iterator<Object> itr = loaded.iterator();

                 while (itr.hasNext()) {
                     AsyncQuery query = (AsyncQuery) itr.next();
                     updateFunction.update(query);
                     tx.save(query, scope);
                 }
                 return loaded;
             });
        } catch (ParseException e) {
            log.error("Exception: {}", e);
        }
        return asyncQueryList;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<AsyncQuery> deleteAsyncQueryAndResultCollection(String filterExpression) {
        log.debug("deleteAsyncQueryAndResultCollection");

        Collection<AsyncQuery> asyncQueryList = null;

        try {
            FilterExpression filter = filterParser.parseFilterExpression(filterExpression,
                    AsyncQuery.class, false);
            asyncQueryList = (Collection<AsyncQuery>) executeInTransaction(dataStore, (tx, scope) -> {

                EntityProjection asyncQueryCollection = EntityProjection.builder()
                        .type(AsyncQuery.class)
                        .filterExpression(filter)
                        .build();

                Iterable<Object> loaded = tx.loadObjects(asyncQueryCollection, scope);
                Iterator<Object> itr = loaded.iterator();

                while (itr.hasNext()) {
                    AsyncQuery query = (AsyncQuery) itr.next();
                    if (query != null) {
                        tx.delete(query, scope);
                    }
                }
                return loaded;
            });
        } catch (ParseException e) {
            log.error("Exception: {}", e);
        }
        return asyncQueryList;
    }

    @Override
    public AsyncQueryResult createAsyncQueryResult(Integer status, String responseBody,
            AsyncQuery asyncQuery, UUID asyncQueryId) {
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
            tx.flush(scope);
            tx.commit(scope);
        } catch (IOException e) {
            log.error("IOException: {}", e);
        } catch (Exception e) {
            log.error("Exception: {}", e);
        }
        return result;
    }
}
