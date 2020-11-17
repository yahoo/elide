/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.service.dao;

import static com.yahoo.elide.core.EntityDictionary.NO_VERSION;

import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.async.models.AsyncAPI;
import com.yahoo.elide.async.models.AsyncAPIResult;
import com.yahoo.elide.async.models.QueryStatus;
import com.yahoo.elide.core.datastore.DataStore;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
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
 * Utility class which implements AsyncAPIDAO.
 */
@Singleton
@Slf4j
@Getter
public class DefaultAsyncAPIDAO implements AsyncAPIDAO {

    @Setter private ElideSettings elideSettings;
    @Setter private DataStore dataStore;

    // Default constructor is needed for standalone implementation for override in getAsyncAPIDao
    public DefaultAsyncAPIDAO() {
    }

    public DefaultAsyncAPIDAO(ElideSettings elideSettings, DataStore dataStore) {
        this.elideSettings = elideSettings;
        this.dataStore = dataStore;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends AsyncAPI> T updateStatus(String asyncAPIId, QueryStatus status, Class<T> type) {
        T queryObj = (T) executeInTransaction(dataStore, (tx, scope) -> {
            EntityProjection asyncAPICollection = EntityProjection.builder()
                    .type(type)
                    .build();
            T query = (T) tx.loadObject(asyncAPICollection, asyncAPIId, scope);
            query.setStatus(status);
            tx.save(query, scope);
            return query;
        });
        return queryObj;
    }

    @Override
    public <T extends AsyncAPI> Collection<T> updateStatusAsyncAPICollection(FilterExpression filterExpression,
            QueryStatus status, Class<T> type) {
        return updateAsyncAPICollection(filterExpression, (asyncAPI) -> {
            asyncAPI.setStatus(status);
            }, type);
    }

    /**
     * This method updates a collection of AsyncAPI objects from database and
     * returns the objects updated.
     * @param filterExpression Filter expression to update AsyncAPI Objects based on
     * @param updateFunction Functional interface for updating AsyncAPI Object
     * @param type AsyncAPI Type Implementation.
     * @return query object list updated
     */
    @SuppressWarnings("unchecked")
    private <T extends AsyncAPI> Collection<T> updateAsyncAPICollection(FilterExpression filterExpression,
            UpdateQuery updateFunction, Class<T> type) {
        log.debug("updateAsyncAPICollection");

        Collection<T> asyncAPIList = null;
        asyncAPIList = (Collection<T>) executeInTransaction(dataStore,
                (tx, scope) -> {
            EntityProjection asyncAPICollection = EntityProjection.builder()
                    .type(type)
                    .filterExpression(filterExpression)
                    .build();

            Iterable<Object> loaded = tx.loadObjects(asyncAPICollection, scope);
            Iterator<Object> itr = loaded.iterator();

            while (itr.hasNext()) {
                T query = (T) itr.next();
                updateFunction.update(query);
                tx.save(query, scope);
            }
            return loaded;
        });
        return asyncAPIList;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends AsyncAPI> Collection<T> deleteAsyncAPIAndResultCollection(
            FilterExpression filterExpression, Class<T> type) {
        log.debug("deleteAsyncAPIAndResultCollection");
        Collection<T> asyncAPIList = null;
        asyncAPIList = (Collection<T>) executeInTransaction(dataStore, (tx, scope) -> {
            EntityProjection asyncAPICollection = EntityProjection.builder()
                    .type(type)
                    .filterExpression(filterExpression)
                    .build();

            Iterable<Object> loaded = tx.loadObjects(asyncAPICollection, scope);
            Iterator<Object> itr = loaded.iterator();

            while (itr.hasNext()) {
                T query = (T) itr.next();
                if (query != null) {
                    tx.delete(query, scope);
                }
            }
            return loaded;
        });
        return asyncAPIList;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends AsyncAPI> T updateAsyncAPIResult(AsyncAPIResult asyncAPIResult,
            String asyncAPIId, Class<T> type) {
        log.debug("updateAsyncAPIResult");
        T queryObj = (T) executeInTransaction(dataStore, (tx, scope) -> {
            EntityProjection asyncAPICollection = EntityProjection.builder()
                    .type(type)
                    .build();
            T query = (T) tx.loadObject(asyncAPICollection, asyncAPIId, scope);
            query.setResult(asyncAPIResult);
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
     * @return Object Returns Entity Object (AsyncAPIResult or AsyncResult)
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
    public <T extends AsyncAPI> Collection<T> loadAsyncAPICollection(FilterExpression filterExpression,
            Class<T> type) {
        Collection<T> asyncAPIList = null;
        log.debug("loadAsyncAPICollection");
        try {
            asyncAPIList = (Collection<T>) executeInTransaction(dataStore, (tx, scope) -> {

                EntityProjection asyncAPICollection = EntityProjection.builder()
                        .type(type)
                        .filterExpression(filterExpression)
                        .build();
                Iterable<Object> loaded = tx.loadObjects(asyncAPICollection, scope);
                return loaded;
            });
        } catch (Exception e) {
            log.error("Exception: {}", e);
            throw new IllegalStateException(e);
        }
        return asyncAPIList;
    }
}
