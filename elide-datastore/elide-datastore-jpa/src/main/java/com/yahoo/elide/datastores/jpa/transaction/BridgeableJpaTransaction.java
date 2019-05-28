/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.jpa.transaction;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.pagination.Pagination;
import com.yahoo.elide.core.sort.Sorting;
import com.yahoo.elide.datastores.multiplex.BridgeableTransaction;
import com.yahoo.elide.datastores.multiplex.MultiplexTransaction;

import lombok.AccessLevel;
import lombok.Getter;

import java.io.Serializable;
import java.util.Optional;

import javax.persistence.EntityManager;

public class BridgeableJpaTransaction extends AbstractJpaTransaction implements BridgeableTransaction {

    public BridgeableJpaTransaction(EntityManager entityManager) {
        super(entityManager);
    }

    @Override
    public boolean isOpen() {
        return getEm().isOpen();
    }

    @Override
    public void begin() {

    }

    @Override
    public Object bridgeableLoadObject(
            MultiplexTransaction muxTx,
            Object parent,
            String relationName,
            Serializable lookupId,
            Optional<FilterExpression> filterExpression,
            RequestScope scope
    ) {
        EntityDictionary entityDictionary = scope.getDictionary();

        if (lookupId == null) {
            // according to contract, lookupId can be null
            Object parentInstance = muxTx.loadObject(
                    entityDictionary.getEntityClass(parent.getClass().getName()),
                    entityDictionary.getId(parent),
                    filterExpression, scope
            );
            lookupId = entityDictionary.getId(entityDictionary.getField(parent, relationName));
        }

        return muxTx.loadObject(scope.getDictionary().getEntityClass(relationName), lookupId, filterExpression, scope);
    }

    @Override
    public Iterable<Object> bridgeableLoadObjects(
            final MultiplexTransaction muxTx,
            final Object parent,
            final String relationName,
            final Optional<FilterExpression> filterExpression,
            final Optional<Sorting> sorting,
            final Optional<Pagination> pagination,
            final RequestScope scope
    ) {
        return muxTx.loadObjects(
                scope.getDictionary().getEntityClass(relationName),
                filterExpression, sorting,
                pagination,
                scope
        );
    }
}
