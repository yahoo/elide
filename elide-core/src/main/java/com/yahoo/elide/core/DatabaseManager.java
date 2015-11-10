/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import javax.ws.rs.core.MultivaluedMap;

/**
 * Database interface library.
 */
public interface DatabaseManager {

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
    DatabaseTransaction beginTransaction();

    /**
     * Begin read-only transaction.
     * Default to regular transaction.
     *
     * @return the database transaction
     */
    default DatabaseTransaction beginReadTransaction(MultivaluedMap<String, String> queryParams) {
        return beginTransaction();
    }
}
