/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.core.datastore.wrapped;

import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.pagination.Pagination;
import com.yahoo.elide.core.sort.Sorting;
import com.yahoo.elide.security.User;
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
    public User accessUser(Object opaqueUser) {
        return tx.accessUser(opaqueUser);
    }

    @Override
    public void preCommit() {
        tx.preCommit();
    }

    @Override
    public <T> T createNewObject(Class<T> entityClass) {
        return tx.createNewObject(entityClass);
    }

    @Override
    public Object loadObject(Class<?> entityClass, Serializable id, Optional<FilterExpression> filterExpression,
                             RequestScope scope) {
        return tx.loadObject(entityClass, id, filterExpression, scope);
    }

    @Override
    public Object getRelation(DataStoreTransaction relationTx, Object entity, String relationName,
                              Optional<FilterExpression> filterExpression, Optional<Sorting> sorting,
                              Optional<Pagination> pagination, RequestScope scope) {
        return tx.getRelation(relationTx, entity, relationName, filterExpression, sorting, pagination, scope);
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
    public Object getAttribute(Object entity, String attributeName, RequestScope scope) {
        return tx.getAttribute(entity, attributeName, scope);
    }

    @Override
    public void setAttribute(Object entity, String attributeName, Object attributeValue, RequestScope scope) {
        tx.setAttribute(entity, attributeName, attributeValue, scope);
    }

    @Override
    public FeatureSupport supportsFiltering(Class<?> entityClass, FilterExpression expression) {
        return tx.supportsFiltering(entityClass, expression);
    }

    @Override
    public boolean supportsSorting(Class<?> entityClass, Sorting sorting) {
        return tx.supportsSorting(entityClass, sorting);
    }

    @Override
    public boolean supportsPagination(Class<?> entityClass) {
        return tx.supportsPagination(entityClass);
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
    public Iterable<Object> loadObjects(Class<?> entityClass,
                                        Optional<FilterExpression> filterExpression,
                                        Optional<Sorting> sorting,
                                        Optional<Pagination> pagination,
                                        RequestScope requestScope) {
        return tx.loadObjects(entityClass, filterExpression, sorting, pagination, requestScope);
    }

    @Override
    public void close() throws IOException {
        tx.close();
    }
}
