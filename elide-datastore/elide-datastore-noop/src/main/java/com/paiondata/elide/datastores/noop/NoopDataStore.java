/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.datastores.noop;

import com.paiondata.elide.core.datastore.DataStore;
import com.paiondata.elide.core.datastore.DataStoreTransaction;
import com.paiondata.elide.core.dictionary.EntityDictionary;

import java.util.ArrayList;
import java.util.Collection;

/**
 * The Noop Datastore allows ephemeral beans to be accessed via Elide. Specifically, beans which you
 * want to make callable endpoints to perform some function but do not wish to persist their values.
 */
public class NoopDataStore implements DataStore {
    protected final ArrayList<Class> entityClasses;

    /**
     * Create a new no-op data store.
     *
     * @param entityClasses Entity classes controlled by this datastore.
     */
    public NoopDataStore(Collection<Class> entityClasses) {
        this.entityClasses = new ArrayList<>(entityClasses);
    }

    @Override
    public void populateEntityDictionary(EntityDictionary dictionary) {
        entityClasses.forEach(dictionary::bindEntity);
    }

    @Override
    public DataStoreTransaction beginTransaction() {
        return new NoopTransaction();
    }
}
