/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.multiplex;

import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Allows multiple database handlers to each process their own beans while keeping the main
 * commit in sync across all managers.
 *
 * <p><B>WARNING</B> If a subordinate commit fails, attempts are made to reverse the previous
 * commits.  If these reversals fail, the databases can be left out of sync.
 * <p>
 * For example, a Multiplex of two databases DB1, DB2 might do:
 * <ul>
 * <li>Save to DB1 and DB2
 * <li>Commit DB1 successfully
 * <li>Commit DB2 fails
 * <li>Attempt to reverse DB1 commit fails
 * </ul>
 */
public class MultiplexManager implements DataStore {

    protected final List<DataStore> dataStores;
    protected final ConcurrentHashMap<Class<?>, DataStore> dataStoreMap = new ConcurrentHashMap<>();
    private EntityDictionary dictionary;

    /**
     * Create a single DataStore to handle provided managers within a single transaction.
     * @param dataStores list of sub-managers
     */
    public MultiplexManager(DataStore... dataStores) {
        this.dataStores = Arrays.asList(dataStores);
    }

    @Override
    public void populateEntityDictionary(EntityDictionary dictionary) {
        this.dictionary = dictionary;

        for (DataStore dataStore : dataStores) {
            EntityDictionary subordinateDictionary = new EntityDictionary(dictionary.getCheckMappings());

            dataStore.populateEntityDictionary(subordinateDictionary);
            for (Class<?> cls : subordinateDictionary.getBindings()) {
                // route class to this database manager
                this.dataStoreMap.put(cls, dataStore);
                // bind to multiplex dictionary
                dictionary.bindEntity(cls);
                dictionary.bindInitializer(subordinateDictionary::initializeEntity, cls);
            }
        }
    }

    @Override
    public DataStoreTransaction beginTransaction() {
        return new MultiplexWriteTransaction(this);
    }

    @Override
    public DataStoreTransaction beginReadTransaction() {
        return new MultiplexReadTransaction(this);
    }

    public EntityDictionary getDictionary() {
        return dictionary;
    }

    /**
     * Lookup subordinate database manager for provided entity class.
     * @param <T> type
     * @param cls provided class
     * @return database manager handling this entity
     */
    protected <T> DataStore getSubManager(Class<T> cls) {
        // Follow for this class or super-class for Entity annotation
        Class<T> type = (Class<T>) dictionary.lookupEntityClass(cls);
        return dataStoreMap.get(type);
    }
}
