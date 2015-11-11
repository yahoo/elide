/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import com.yahoo.elide.security.User;

import java.io.Closeable;
import java.io.Serializable;
import java.util.Collection;

/**
 * Wraps the Database Transaction type.
 */
public interface DataStoreTransaction extends Closeable {

    /**
     * Wrap the opaque user.
     *
     * @param opaqueUser the opaque user
     * @return wrapped user context
     */
    default User accessUser(Object opaqueUser) {
        return new User(opaqueUser);
    }

    /**
     * Save entity to database table.
     *
     * @param entity record to save
     */
    void save(Object entity);

    /**
     * Delete entity from database table.
     *
     * @param entity record to delete
     */
    void delete(Object entity);

    /**
     * Write any outstanding entities before processing response.
     */
    default void flush() {
    }

    /**
     * End the current transaction.
     */
    void commit();

    /**
     * Create new entity record.
     *
     * @param entityClass the entity class
     * @return new record
     */
    <T> T createObject(Class<T> entityClass);

    /**
     * Read entity record from database table.
     *
     * @param entityClass the entity class
     * @param id ID of object
     * @return record
     */
    <T> T loadObject(Class<T> entityClass, Serializable id);

    /**
     * Read entity records from database table.
     * <P>TODO Want to support filtering, limiting, and sorting?
     * @param entityClass the entity class
     * @return records
     */
    <T> Iterable<T> loadObjects(Class<T> entityClass);

    /**
     * Read entity records from database table with applied criteria.
     *
     * @param entityClass the entity class
     * @param filterScope scope for filter processing
     * @return records
     */
    default <T> Iterable<T> loadObjects(Class<T> entityClass, FilterScope<T> filterScope) {
        // default to ignoring criteria
        return loadObjects(entityClass);
    }

    /**
     * Filter a collection by the Predicates in filterScope.
     *
     * @param collection the collection to filter
     * @param type
     * @param filterScope the FilterScope containing the set of Predicates
     * @return the filtered collection
     */
    default Collection filterCollection(Collection collection, String type, FilterScope<?> filterScope) {
        return collection;
    }
}
