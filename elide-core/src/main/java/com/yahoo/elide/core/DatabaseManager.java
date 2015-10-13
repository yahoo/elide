/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

/**
 * Database interface library
 */
public abstract class DatabaseManager {

    /**
     * Load entity dictionary with JPA annotated beans.
     * @param dictionary the dictionary
     */
    public abstract void populateEntityDictionary(EntityDictionary dictionary);

    /**
     * Begin transaction.
     *
     * @return the database transaction
     */
    public abstract DatabaseTransaction beginTransaction();
}
