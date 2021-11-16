/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.modelconfig.store;

import com.yahoo.elide.core.datastore.DataStore;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.type.ClassType;
import com.yahoo.elide.modelconfig.io.FileLoader;
import com.yahoo.elide.modelconfig.store.models.ConfigFile;
import com.yahoo.elide.modelconfig.validator.Validator;

/**
 * Elide DataStore which loads/persists HJSON configuration files as Elide models.
 */
public class ConfigDataStore implements DataStore {

    private final FileLoader fileLoader;
    private final Validator validator;

    public ConfigDataStore(
            String configRoot,
            Validator validator
    ) {
        this.fileLoader = new FileLoader(configRoot);
        this.validator = validator;
    }

    @Override
    public void populateEntityDictionary(EntityDictionary dictionary) {
        dictionary.bindEntity(ClassType.of(ConfigFile.class));
    }

    @Override
    public ConfigDataStoreTransaction beginTransaction() {
        return new ConfigDataStoreTransaction(fileLoader, false, validator);
    }

    @Override
    public ConfigDataStoreTransaction beginReadTransaction() {
        return new ConfigDataStoreTransaction(fileLoader, true, validator);
    }
}
