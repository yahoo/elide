/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.service;

import static com.yahoo.elide.core.EntityDictionary.NO_VERSION;

import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.async.models.AsyncAPI;
import com.yahoo.elide.async.models.AsyncAPIResult;
import com.yahoo.elide.async.models.QueryStatus;
import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.RequestScope;
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

    @Setter private ElideSettings elideSettings;
    @Setter private DataStore dataStore;

    // Default constructor is needed for standalone implementation for override in getAsyncQueryDao
    public DefaultAsyncQueryDAO() {
    }

    public DefaultAsyncQueryDAO(ElideSettings elideSettings, DataStore dataStore) {
        this.elideSettings = elideSettings;
        this.dataStore = dataStore;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends AsyncAPI> T updateStatus(String asyncQueryId, QueryStatus status, Class<T> type) {
        T queryObj = (T) executeInTransaction(dataStore, (tx, scope) -> {
            EntityProjection asyncQueryCollection = EntityProjection.builder()
                    .type(type)
                    .build();
            T query = (T) tx.loadObject(asyncQueryCollection, asyncQueryId, scope);
            query.setStatus(status);
            tx.save(query, scope);
            return query;
        });
        return queryObj;
    }

    @Override
    public <T extends AsyncAPI> Collection<T> updateStatusAsyncQueryCollection(FilterExpression filterExpression,
            QueryStatus status, Class<T> type) {
        return updateAsyncQueryCollection(filterExpression, (asyncQuery) -> {
            asyncQuery.setStatus(status);
            }, type);
    }

    /**
     * This method updates a collection of AsyncQuery objects from database and
     * returns the objects updated.
     * @param filterExpression Filter expression to update AsyncQuery Objects based on
     * @param updateFunction Functional interface for updating AsyncQuery Object
     * @param type AsyncAPI Type Implementation.
     * @return query object list updated
     */
    @SuppressWarnings("unchecked")
    private <T extends AsyncAPI> Collection<T> updateAsyncQueryCollection(FilterExpression filterExpression,
            UpdateQuery updateFunction, Class<T> type) {
        log.debug("updateAsyncQueryCollection");

        Collection<T> asyncQueryList = null;
        asyncQueryList = (Collection<T>) executeInTransaction(dataStore,
                (tx, scope) -> {
            EntityProjection asyncQueryCollection = EntityProjection.builder()
                    .type(type)
                    .filterExpression(filterExpression)
                    .build();

            Iterable<Object> loaded = tx.loadObjects(asyncQueryCollection, scope);
            Iterator<Object> itr = loaded.iterator();

            while (itr.hasNext()) {
                T query = (T) itr.next();
                updateFunction.update(query);
                tx.save(query, scope);
            }
            return loaded;
        });
        return asyncQueryList;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends AsyncAPI> Collection<T> deleteAsyncQueryAndResultCollection(
            FilterExpression filterExpression, Class<T> type) {
        log.debug("deleteAsyncQueryAndResultCollection");
        Collection<T> asyncQueryList = null;
        asyncQueryList = (Collection<T>) executeInTransaction(dataStore, (tx, scope) -> {
            EntityProjection asyncQueryCollection = EntityProjection.builder()
                    .type(type)
                    .filterExpression(filterExpression)
                    .build();

            Iterable<Object> loaded = tx.loadObjects(asyncQueryCollection, scope);
            Iterator<Object> itr = loaded.iterator();

            while (itr.hasNext()) {
                T query = (T) itr.next();
                if (query != null) {
                    tx.delete(query, scope);
                }
            }
            return loaded;
        });
        return asyncQueryList;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends AsyncAPI> T updateAsyncQueryResult(AsyncAPIResult asyncQueryResult,
            String asyncQueryId, Class<T> type) {
        log.debug("updateAsyncQueryResult");
        T queryObj = (T) executeInTransaction(dataStore, (tx, scope) -> {
            EntityProjection asyncQueryCollection = EntityProjection.builder()
                    .type(type)
                    .build();
            T query = (T) tx.loadObject(asyncQueryCollection, asyncQueryId, scope);
            query.setResult(asyncQueryResult);
            if (query.getStatus().equals(QueryStatus.CANCELLED)) {
                query.setStatus(QueryStatus.CANCEL_COMPLETE);
            } else if (!(query.getStatus().equals(QueryStatus.CANCEL_COMPLETE))) {
                query.setStatus(QueryStatus.COMPLETE);
            }
            tx.save(query, scope);
            return query;
        });
        return queryObj;
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
            RequestScope scope = new RequestScope("", "query", NO_VERSION, jsonApiDoc,
                    tx, null, queryParams, UUID.randomUUID(), elideSettings);
            result = action.execute(tx, scope);
            tx.flush(scope);
            tx.commit(scope);
        } catch (IOException e) {
            log.error("IOException: {}", e);
            throw new IllegalStateException(e);
        }
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends AsyncAPI> Collection<T> loadAsyncQueryCollection(FilterExpression filterExpression,
            Class<T> type) {
        Collection<T> asyncQueryList = null;
        log.debug("loadAsyncQueryCollection");
        try {
            asyncQueryList = (Collection<T>) executeInTransaction(dataStore, (tx, scope) -> {

                EntityProjection asyncQueryCollection = EntityProjection.builder()
                        .type(type)
                        .filterExpression(filterExpression)
                        .build();
                Iterable<Object> loaded = tx.loadObjects(asyncQueryCollection, scope);
                return loaded;
            });
        } catch (Exception e) {
            log.error("Exception: {}", e);
            throw new IllegalStateException(e);
        }
        return asyncQueryList;
    }
}
