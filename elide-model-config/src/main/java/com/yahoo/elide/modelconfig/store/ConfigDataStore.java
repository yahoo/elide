/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.modelconfig.store;

import com.yahoo.elide.core.datastore.DataStore;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.type.ClassType;
import com.yahoo.elide.modelconfig.io.FileLoader;
import com.yahoo.elide.modelconfig.store.models.ConfigFile;

/**
 * Elide DataStore which loads/persists HJSON configuration files as Elide models.
 */
public class ConfigDataStore implements DataStore {

    private final FileLoader fileLoader;

    public ConfigDataStore(String configRoot) {
        this.fileLoader = new FileLoader(configRoot);
    }

    @Override
    public void populateEntityDictionary(EntityDictionary dictionary) {
        dictionary.bindEntity(ClassType.of(ConfigFile.class));
    }

    @Override
    public DataStoreTransaction beginTransaction() {
        return new ConfigDataStoreTransaction(fileLoader);
    }

    @Override
    public DataStoreTransaction beginReadTransaction() {
        return new ConfigDataStoreTransaction(fileLoader);
    }
}
