/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.core.datastore.wrapped;

import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.request.Attribute;
import com.yahoo.elide.core.request.EntityProjection;
import com.yahoo.elide.core.request.Relationship;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.IOException;
import java.io.Serializable;
import java.util.Optional;
import java.util.Set;

/**
 * Delegates all calls to a wrapped transaction.
 */
@Data
@AllArgsConstructor
public abstract class TransactionWrapper implements DataStoreTransaction {
    protected DataStoreTransaction tx;

    @Override
    public void preCommit() {
        tx.preCommit();
    }

    @Override
    public <T> T createNewObject(Class<T> entityClass) {
        return tx.createNewObject(entityClass);
    }

    @Override
    public Object loadObject(EntityProjection projection, Serializable id,
                             RequestScope scope) {
        return tx.loadObject(projection, id, scope);
    }

    @Override
    public Object getRelation(DataStoreTransaction relationTx, Object entity,
                              Relationship relationship, RequestScope scope) {
        return tx.getRelation(relationTx, entity, relationship, scope);
    }

    @Override
    public void updateToManyRelation(DataStoreTransaction relationTx, Object entity, String relationName,
                                     Set<Object> newRelationships, Set<Object> deletedRelationships,
                                     RequestScope scope) {
        tx.updateToManyRelation(relationTx, entity, relationName, newRelationships, deletedRelationships, scope);

    }

    @Override
    public void updateToOneRelation(DataStoreTransaction relationTx, Object entity,
                                    String relationName, Object relationshipValue, RequestScope scope) {
        tx.updateToOneRelation(relationTx, entity, relationName, relationshipValue, scope);
    }

    @Override
    public Object getAttribute(Object entity, Attribute attribute, RequestScope scope) {
        return tx.getAttribute(entity, attribute, scope);
    }

    @Override
    public void setAttribute(Object entity, Attribute attribute, RequestScope scope) {
        tx.setAttribute(entity, attribute, scope);
    }

    @Override
    public FeatureSupport supportsFiltering(RequestScope scope, Optional<Object> parent, EntityProjection projection) {
        return tx.supportsFiltering(scope, parent, projection);
    }

    @Override
    public boolean supportsSorting(RequestScope scope, Optional<Object> parent, EntityProjection projection) {
        return tx.supportsSorting(scope, parent, projection);
    }

    @Override
    public boolean supportsPagination(RequestScope scope, Optional<Object> parent, EntityProjection projection) {
        return tx.supportsPagination(scope, parent, projection);
    }

    @Override
    public void save(Object o, RequestScope requestScope) {
        tx.save(o, requestScope);
    }

    @Override
    public void delete(Object o, RequestScope requestScope) {
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
    public Iterable<Object> loadObjects(EntityProjection projection, RequestScope scope) {
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
}
