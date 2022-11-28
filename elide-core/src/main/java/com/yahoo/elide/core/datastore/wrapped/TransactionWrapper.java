/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.core.datastore.wrapped;

import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.datastore.DataStoreIterable;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.request.Attribute;
import com.yahoo.elide.core.request.EntityProjection;
import com.yahoo.elide.core.request.Relationship;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.IOException;
import java.io.Serializable;
import java.util.Set;

/**
 * Delegates all calls to a wrapped transaction.
 */
@Data
@AllArgsConstructor
public abstract class TransactionWrapper implements DataStoreTransaction {
    protected DataStoreTransaction tx;

    @Override
    public void preCommit(RequestScope scope) {
        tx.preCommit(scope);
    }

    @Override
    public <T> T loadObject(EntityProjection projection, Serializable id,
                             RequestScope scope) {
        return tx.loadObject(projection, id, scope);
    }

    @Override
    public <T, R> DataStoreIterable<R> getToManyRelation(DataStoreTransaction relationTx, T entity,
                                                              Relationship relationship, RequestScope scope) {
        return tx.getToManyRelation(relationTx, entity, relationship, scope);
    }

    @Override
    public <T, R> R getToOneRelation(DataStoreTransaction relationTx, T entity,
                                     Relationship relationship, RequestScope scope) {
        return tx.getToOneRelation(relationTx, entity, relationship, scope);
    }

    @Override
    public <T, R> void updateToManyRelation(DataStoreTransaction relationTx, T entity, String relationName,
                                     Set<R> newRelationships, Set<R> deletedRelationships,
                                     RequestScope scope) {
        tx.updateToManyRelation(relationTx, entity, relationName, newRelationships, deletedRelationships, scope);

    }

    @Override
    public <T, R> void updateToOneRelation(DataStoreTransaction relationTx, T entity,
                                    String relationName, R relationshipValue, RequestScope scope) {
        tx.updateToOneRelation(relationTx, entity, relationName, relationshipValue, scope);
    }

    @Override
    public <T, R> R getAttribute(T entity, Attribute attribute, RequestScope scope) {
        return tx.getAttribute(entity, attribute, scope);
    }

    @Override
    public <T> void setAttribute(T entity, Attribute attribute, RequestScope scope) {
        tx.setAttribute(entity, attribute, scope);
    }

    @Override
    public <T> void save(T o, RequestScope requestScope) {
        tx.save(o, requestScope);
    }

    @Override
    public <T> void delete(T o, RequestScope requestScope) {
        tx.delete(o, requestScope);
    }

    @Override
    public void flush(RequestScope requestScope) {
        tx.flush(requestScope);
    }

    @Override
    public void commit(RequestScope requestScope) {
        tx.commit(requestScope);
    }

    @Override
    public void createObject(Object o, RequestScope requestScope) {
        tx.createObject(o, requestScope);
    }

    @Override
    public <T> DataStoreIterable<T> loadObjects(EntityProjection projection, RequestScope scope) {
        return tx.loadObjects(projection, scope);
    }

    @Override
    public void close() throws IOException {
        tx.close();
    }

    @Override
    public void cancel(RequestScope scope) {
        tx.cancel(scope);
    }

    @Override
    public <T> T getProperty(String propertyName) {
        return tx.getProperty(propertyName);
    }
}
