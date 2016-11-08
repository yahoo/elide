/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.noop;

import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.exceptions.InvalidOperationException;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.pagination.Pagination;
import com.yahoo.elide.core.sort.Sorting;
import com.yahoo.elide.security.RequestScope;

import java.io.IOException;
import java.io.Serializable;
import java.util.Optional;

/**
 * Noop transaction. Specifically, this transaction does not perform any actions (i.e. no operation).
 */
public class NoopTransaction implements DataStoreTransaction {
    @Override
    public void save(Object entity, RequestScope scope) {

    }

    @Override
    public void delete(Object entity, RequestScope scope) {

    }

    @Override
    public void flush(RequestScope scope) {

    }

    @Override
    public void commit(RequestScope requestScope) {

    }

    @Override
    public void createObject(Object entity, RequestScope scope) {

    }

    @Override
    public Object loadObject(Class<?> entityClass,
                             Serializable id,
                             Optional<FilterExpression> filterExpression,
                             RequestScope scope) {
        // Loads unsupported since nothing is persisted in this store
        throw new InvalidOperationException("Cannot load object of type: " + entityClass);
    }

    @Override
    public Iterable<Object> loadObjects(Class<?> entityClass,
                                        Optional<FilterExpression> filterExpression,
                                        Optional<Sorting> sorting,
                                        Optional<Pagination> pagination,
                                        RequestScope scope) {
        // Loads unsupported since nothing is persisted in this store
        throw new InvalidOperationException("Cannot load object of type: " + entityClass);
    }

    @Override
    public void close() throws IOException {

    }
}
