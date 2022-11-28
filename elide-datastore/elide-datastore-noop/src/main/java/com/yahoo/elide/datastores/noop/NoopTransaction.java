/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.noop;

import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.datastore.DataStoreIterable;
import com.yahoo.elide.core.datastore.DataStoreIterableBuilder;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.request.EntityProjection;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;

/**
 * Noop transaction. Specifically, this transaction does not perform any actions (i.e. no operation).
 */
@Slf4j
public class NoopTransaction implements DataStoreTransaction {
    /**
     * No-op transaction, do nothing.
     * @param entity - the object to save.
     * @param scope - contains request level metadata.
     */
    @Override
    public <T> void save(T entity, RequestScope scope) {
        // No-op transaction, do nothing.
    }

    /**
     * No-op transaction, do nothing.
     * @param entity - the object to delete.
     * @param scope - contains request level metadata.
     */
    @Override
    public <T> void delete(T entity, RequestScope scope) {
        // No-op transaction, do nothing.
    }

    /**
     * No-op transaction, do nothing.
     * @param scope the request scope for the current request
     */
    @Override
    public void flush(RequestScope scope) {
        // No-op transaction, do nothing.
    }

    /**
     * No-op transaction, do nothing.
     * @param scope the request scope for the current request
     */
    @Override
    public void commit(RequestScope scope) {
        // No-op transaction, do nothing.
    }

    /**
     * No-op transaction, do nothing.
     * @param entity - the object to create in the data store.
     * @param scope - contains request level metadata.
     */
    @Override
    public <T> void createObject(T entity, RequestScope scope) {
        // No-op transaction, do nothing.
    }

    /**
     * No-op transaction, do nothing.
     * @param projection the projection to query
     * @param id - the ID of the object to load.
     * @param scope - the current request scope. It is optional for the data store to attempt evaluation.
     * @return a new persistent resource with a new instance of {@code entityClass}
     */
    @Override
    public <T> T loadObject(EntityProjection projection,
                             Serializable id,
                             RequestScope scope) {
        // Loads are supported but empty object (with specified id) is returned.
        // NOTE: This is primarily useful for enabling objects of solely computed properties to be fetched.
        Object entity;
        try {
            entity = projection.getType().newInstance();
        } catch (IllegalAccessException | InstantiationException e) {
            log.error("Could not load object {} through NoopStore", projection.getType(), e);
            throw new RuntimeException(e);
        }

        String uuid = scope.getUUIDFor(entity);
        // Side-effecting method of the PersistentResource :( however, it enables us to do this without reinventing
        // the wheel. Should probably be refactored eventually nonetheless.
        new PersistentResource<>(entity, uuid, scope).setId(id.toString());

        return (T) entity;
    }

    /**
     * No-op transaction, do nothing.
     * @param projection - the projection to load
     * @param scope - contains request level metadata.
     * @return a {@link Collections#singletonList} with a new persistent resource with id 1
     */
    @Override
    public <T> DataStoreIterable<T> loadObjects(EntityProjection projection,
                                                RequestScope scope) {
        // Default behavior: load object 1 and return as an array
        return new DataStoreIterableBuilder(
                Collections.singletonList(this.loadObject(projection, 1L, scope))).build();
    }

    /**
     * No-op transaction, do nothing.
     */
    @Override
    public void close() throws IOException {
        // No-op transaction, do nothing.
    }

    /**
     * No-op transaction, do nothing.
     */
    @Override
    public void cancel(RequestScope scope) {
        // No-op transaction, do nothing.
    }
}
