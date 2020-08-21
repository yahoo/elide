/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.service;

import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.async.models.AsyncQuery;
import com.yahoo.elide.async.models.AsyncQueryResult;
import com.yahoo.elide.async.models.QueryStatus;
import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.request.EntityProjection;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.Iterator;

import javax.inject.Singleton;

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

    @Override
    public AsyncQuery updateStatus(String asyncQueryId, QueryStatus status) {
        AsyncQuery queryObj = (AsyncQuery) DBUtil.executeInTransaction(elideSettings,
                dataStore, (tx, scope) -> {
            EntityProjection asyncQueryCollection = EntityProjection.builder()
                    .type(AsyncQuery.class)
                    .build();
            AsyncQuery query = (AsyncQuery) tx.loadObject(asyncQueryCollection, asyncQueryId, scope);
            query.setStatus(status);
            tx.save(query, scope);
            return query;
        });
        return queryObj;
    }

    @Override
    public Collection<AsyncQuery> updateStatusAsyncQueryCollection(FilterExpression filterExpression,
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
    private Collection<AsyncQuery> updateAsyncQueryCollection(FilterExpression filterExpression,
            UpdateQuery updateFunction) {
        log.debug("updateAsyncQueryCollection");

        Collection<AsyncQuery> asyncQueryList = null;
        asyncQueryList = (Collection<AsyncQuery>) DBUtil.executeInTransaction(elideSettings, dataStore,
                    (tx, scope) -> {
            EntityProjection asyncQueryCollection = EntityProjection.builder()
                    .type(AsyncQuery.class)
                    .filterExpression(filterExpression)
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
        return asyncQueryList;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<AsyncQuery> deleteAsyncQueryAndResultCollection(FilterExpression filterExpression) {
        log.debug("deleteAsyncQueryAndResultCollection");
        Collection<AsyncQuery> asyncQueryList = null;
        asyncQueryList = (Collection<AsyncQuery>) DBUtil.executeInTransaction(elideSettings, dataStore,
                (tx, scope) -> {
            EntityProjection asyncQueryCollection = EntityProjection.builder()
                    .type(AsyncQuery.class)
                    .filterExpression(filterExpression)
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
        return asyncQueryList;
    }

    @Override
    public AsyncQuery updateAsyncQueryResult(AsyncQueryResult asyncQueryResult, String asyncQueryId) {
        log.debug("updateAsyncQueryResult");
        AsyncQuery queryObj = (AsyncQuery) DBUtil.executeInTransaction(elideSettings,
                dataStore, (tx, scope) -> {
            EntityProjection asyncQueryCollection = EntityProjection.builder()
                    .type(AsyncQuery.class)
                    .build();
            AsyncQuery query = (AsyncQuery) tx.loadObject(asyncQueryCollection, asyncQueryId, scope);
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

    @Override
    @SuppressWarnings("unchecked")
    public Collection<AsyncQuery> loadAsyncQueryCollection(FilterExpression filterExpression) {
        Collection<AsyncQuery> asyncQueryList = null;
        log.debug("loadAsyncQueryCollection");
        try {
            asyncQueryList = (Collection<AsyncQuery>) DBUtil.executeInTransaction(elideSettings, dataStore,
                    (tx, scope) -> {
                EntityProjection asyncQueryCollection = EntityProjection.builder()
                        .type(AsyncQuery.class)
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
