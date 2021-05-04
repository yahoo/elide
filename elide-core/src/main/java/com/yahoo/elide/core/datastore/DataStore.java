/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.datastore;

import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.security.PermissionExecutor;
import com.yahoo.elide.core.security.executors.ActivePermissionExecutor;

import java.util.function.Function;

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

    default Function<RequestScope, PermissionExecutor> getPermissionExecutorFunction() {
        return ActivePermissionExecutor::new;
    }
}
