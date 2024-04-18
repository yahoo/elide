/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.datastores.multiplex;

import com.paiondata.elide.core.RequestScope;
import com.paiondata.elide.core.datastore.DataStore;
import com.paiondata.elide.core.datastore.DataStoreTransaction;
import com.paiondata.elide.core.dictionary.EntityBinding;
import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.security.PermissionExecutor;
import com.paiondata.elide.core.type.Type;
import com.paiondata.elide.core.utils.ObjectCloner;
import com.paiondata.elide.core.utils.ObjectCloners;

import lombok.AccessLevel;
import lombok.Setter;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

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
public final class MultiplexManager implements DataStore {

    protected final List<DataStore> dataStores;
    protected final ConcurrentHashMap<Type<?>, DataStore> dataStoreMap = new ConcurrentHashMap<>();
    protected final Predicate<DataStore> applyCompensatingTransactions;
    protected final ObjectCloner objectCloner;

    @Setter(AccessLevel.PROTECTED)
    private EntityDictionary dictionary;

    /**
     * Create a single DataStore to handle provided managers within a single transaction.
     * @param dataStores list of sub-managers
     */
    public MultiplexManager(DataStore... dataStores) {
        this(ObjectCloners::clone, dataStore -> true, dataStores);
    }

    /**
     * Create a single DataStore to handle provided managers within a single
     * transaction.
     *
     * @param objectCloner to use for cloning objects to apply to compensating
     *                     transaction
     * @param dataStores   list of sub-managers
     */
    public MultiplexManager(ObjectCloner objectCloner, DataStore... dataStores) {
        this(objectCloner, dataStore -> true, dataStores);
    }

    /**
     * Create a single DataStore to handle provided managers within a single
     * transaction.
     *
     * @param objectCloner                  to use for cloning objects to apply to
     *                                      compensating transaction
     * @param applyCompensatingTransactions apply compensating transactions on
     *                                      rollback to previously committed
     *                                      datastores
     * @param dataStores                    list of sub-managers
     */
    public MultiplexManager(ObjectCloner objectCloner, Predicate<DataStore> applyCompensatingTransactions,
            DataStore... dataStores) {
        this.objectCloner = objectCloner;
        this.dataStores = Arrays.asList(dataStores);
        this.applyCompensatingTransactions = applyCompensatingTransactions;
    }

    protected boolean isApplyCompensatingTransactions(DataStore dataStore) {
        return this.applyCompensatingTransactions.test(dataStore);
    }

    @Override
    public void populateEntityDictionary(EntityDictionary dictionary) {
        this.dictionary = dictionary;

        for (DataStore dataStore : dataStores) {
            EntityDictionary subordinateDictionary = new EntityDictionary(
                    dictionary.getCheckMappings(),
                    dictionary.getRoleChecks(),
                    dictionary.getInjector(),
                    dictionary.getSerdeLookup(),
                    dictionary.getEntitiesToExclude(),
                    dictionary.getScanner());

            dataStore.populateEntityDictionary(subordinateDictionary);
            for (EntityBinding binding : subordinateDictionary.getBindings(false)) {
                // route class to this database manager
                this.dataStoreMap.put(binding.entityClass, dataStore);

                // bind to multiplex dictionary
                dictionary.bindEntity(binding);
                subordinateDictionary.getApiVersions().forEach(dictionary.getApiVersions()::add);
            }

            for (Map.Entry<Type<?>, Function<RequestScope, PermissionExecutor>> entry
                    : subordinateDictionary.getEntityPermissionExecutor().entrySet()) {
                dictionary.bindPermissionExecutor(entry.getKey(), entry.getValue());

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
    protected <T> DataStore getSubManager(Type<T> cls) {
        // Follow for this class or super-class for Entity annotation
        Type<T> type = (Type<T>) dictionary.lookupBoundClass(cls);
        return dataStoreMap.get(type);
    }
}
