/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.async.service.dao;

import static com.paiondata.elide.core.dictionary.EntityDictionary.NO_VERSION;

import com.paiondata.elide.ElideSettings;
import com.paiondata.elide.async.models.AsyncApi;
import com.paiondata.elide.async.models.AsyncApiResult;
import com.paiondata.elide.async.models.QueryStatus;
import com.paiondata.elide.core.RequestScope;
import com.paiondata.elide.core.datastore.DataStore;
import com.paiondata.elide.core.datastore.DataStoreTransaction;
import com.paiondata.elide.core.filter.expression.FilterExpression;
import com.paiondata.elide.core.request.EntityProjection;
import com.paiondata.elide.core.request.route.Route;
import com.paiondata.elide.jsonapi.JsonApiRequestScope;
import com.paiondata.elide.jsonapi.models.JsonApiDocument;

import jakarta.inject.Singleton;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Iterator;
import java.util.UUID;

/**
 * Utility class which implements AsyncApiDao.
 */
@Singleton
@Slf4j
@Getter
public class DefaultAsyncApiDao implements AsyncApiDao {

    @Setter private ElideSettings elideSettings;
    @Setter private DataStore dataStore;

    // Default constructor is needed for standalone implementation for override in getAsyncApiDao
    public DefaultAsyncApiDao() {
    }

    public DefaultAsyncApiDao(ElideSettings elideSettings, DataStore dataStore) {
        this.elideSettings = elideSettings;
        this.dataStore = dataStore;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends AsyncApi> T updateStatus(String asyncApiId, QueryStatus status, Class<T> type) {
        T queryObj = (T) executeInTransaction(dataStore, (tx, scope) -> {
            EntityProjection asyncApiIterable = EntityProjection.builder()
                    .type(type)
                    .build();
            T query = (T) tx.loadObject(asyncApiIterable, asyncApiId, scope);
            query.setStatus(status);
            tx.save(query, scope);
            return query;
        });
        return queryObj;
    }

    @Override
    public <T extends AsyncApi> Iterable<T> updateStatusAsyncApiByFilter(FilterExpression filterExpression,
            QueryStatus status, Class<T> type) {
        return updateAsyncApiIterable(filterExpression, asyncApi -> asyncApi.setStatus(status), type);
    }

    /**
     * This method updates a Iterable of AsyncApi objects from database and
     * returns the objects updated.
     * @param filterExpression Filter expression to update AsyncApi Objects based on
     * @param updateFunction Functional interface for updating AsyncApi Object
     * @param type AsyncApi Type Implementation.
     * @return query object list updated
     */
    @SuppressWarnings("unchecked")
    private <T extends AsyncApi> Iterable<T> updateAsyncApiIterable(FilterExpression filterExpression,
            UpdateQuery updateFunction, Class<T> type) {
        log.debug("updateAsyncApiIterable");

        Iterable<T> asyncApiList = null;
        asyncApiList = (Iterable<T>) executeInTransaction(dataStore,
                (tx, scope) -> {
            EntityProjection asyncApiIterable = EntityProjection.builder()
                    .type(type)
                    .filterExpression(filterExpression)
                    .build();

            Iterable<Object> loaded = tx.loadObjects(asyncApiIterable, scope);
            Iterator<Object> itr = loaded.iterator();

            while (itr.hasNext()) {
                T query = (T) itr.next();
                updateFunction.update(query);
                tx.save(query, scope);
            }
            return loaded;
        });
        return asyncApiList;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends AsyncApi> Iterable<T> deleteAsyncApiAndResultByFilter(
            FilterExpression filterExpression, Class<T> type) {
        log.debug("deleteAsyncApiAndResultByFilter");
        Iterable<T> asyncApiList = null;
        asyncApiList = (Iterable<T>) executeInTransaction(dataStore, (tx, scope) -> {
            EntityProjection asyncApiIterable = EntityProjection.builder()
                    .type(type)
                    .filterExpression(filterExpression)
                    .build();

            Iterable<Object> loaded = tx.loadObjects(asyncApiIterable, scope);
            Iterator<Object> itr = loaded.iterator();

            while (itr.hasNext()) {
                T query = (T) itr.next();
                if (query != null) {
                    tx.delete(query, scope);
                }
            }
            return loaded;
        });
        return asyncApiList;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends AsyncApi> T updateAsyncApiResult(AsyncApiResult asyncApiResult,
            String asyncApiId, Class<T> type) {
        log.debug("updateAsyncApiResult");
        T queryObj = (T) executeInTransaction(dataStore, (tx, scope) -> {
            EntityProjection asyncApiIterable = EntityProjection.builder()
                    .type(type)
                    .build();
            T query = (T) tx.loadObject(asyncApiIterable, asyncApiId, scope);
            query.setResult(asyncApiResult);
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
     * @return Object Returns Entity Object (AsyncApiResult or AsyncResult)
     */
    protected Object executeInTransaction(DataStore dataStore, Transactional action) {
        log.debug("executeInTransaction");
        Object result = null;
        try (DataStoreTransaction tx = dataStore.beginTransaction()) {
            JsonApiDocument jsonApiDoc = new JsonApiDocument();
            Route route = Route.builder().path("query").apiVersion(NO_VERSION).build();
            RequestScope scope = JsonApiRequestScope.builder().route(route).dataStoreTransaction(tx)
                    .requestId(UUID.randomUUID()).elideSettings(elideSettings).jsonApiDocument(jsonApiDoc).build();
            result = action.execute(tx, scope);
            tx.flush(scope);
            tx.commit(scope);
        } catch (IOException e) {
            log.error("IOException: {}", e.toString());
            throw new IllegalStateException(e);
        }
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends AsyncApi> Iterable<T> loadAsyncApiByFilter(FilterExpression filterExpression,
            Class<T> type) {
        Iterable<T> asyncApiList = null;
        log.debug("loadAsyncApiByFilter");
        try {
            asyncApiList = (Iterable<T>) executeInTransaction(dataStore, (tx, scope) -> {

                EntityProjection asyncApiIterable = EntityProjection.builder()
                        .type(type)
                        .filterExpression(filterExpression)
                        .build();
                return tx.loadObjects(asyncApiIterable, scope);
            });
        } catch (Exception e) {
            log.error("Exception: {}", e.toString());
            throw new IllegalStateException(e);
        }
        return asyncApiList;
    }
}
