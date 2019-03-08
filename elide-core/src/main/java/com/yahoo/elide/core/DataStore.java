/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

/**
 * Database interface library.
 */
public interface DataStore {

    /**
     * Load entity dictionary with JPA annotated beans.
     *
     * @param dictionary the dictionary
     */
    void populateEntityDictionary(EntityDictionary dictionary);

    /**
     * Begin transaction.
     *
     * @return the database transaction
     */
    DataStoreTransaction beginTransaction();

    /**
     * Begin read-only transaction.  Default to regular transaction.
     *
     * @return the database transaction
     */
    default DataStoreTransaction beginReadTransaction() {
        return beginTransaction();
    }

    default boolean supportsFiltering() {
        return true;
    }

    default boolean supportsSorting() {
        return true;
    }

    default boolean supportsPagination() {
        return true;
    }
}
