/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.noop;

import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.pagination.Pagination;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.sort.Sorting;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.Optional;

/**
 * Noop transaction. Specifically, this transaction does not perform any actions (i.e. no operation).
 */
@Slf4j
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
        // Loads are supported but empty object (with specified id) is returned.
        // NOTE: This is primarily useful for enabling objects of solely computed properties
        //       to be fetched.
        Object entity;
        try {
            entity = entityClass.newInstance();
        } catch (IllegalAccessException | InstantiationException e) {
            log.error("Could not load object {} through NoopStore", entityClass, e);
            throw new RuntimeException(e);
        }

        // Side-effecting method of the PersistentResource :( however, it enables us to do this without reinventing
        // the wheel. Should probably be refactored eventually nonetheless.
        new PersistentResource<>(entity, (com.yahoo.elide.core.RequestScope) scope).setId(id.toString());

        return entity;
    }

    @Override
    public Iterable<Object> loadObjects(Class<?> entityClass,
                                        Optional<FilterExpression> filterExpression,
                                        Optional<Sorting> sorting,
                                        Optional<Pagination> pagination,
                                        RequestScope scope) {
        // Default behavior: load object 1 and return as an array
        return Collections.singletonList(this.loadObject(entityClass, 1L, filterExpression, scope));
    }

    @Override
    public void close() throws IOException {

    }
}
