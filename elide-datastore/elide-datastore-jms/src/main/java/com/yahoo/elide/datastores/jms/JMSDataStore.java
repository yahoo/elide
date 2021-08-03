/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.jms;

import com.yahoo.elide.core.datastore.DataStore;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.type.Type;

import java.util.Set;

public class JMSDataStore implements DataStore {
    Set<Type<?>> models;

    public JMSDataStore(Set<Type<?>> models) {
        this.models = models;
    }

    @Override
    public void populateEntityDictionary(EntityDictionary dictionary) {
        for (Type<?> model : models) {
            dictionary.bindEntity(model);
        }
    }

    @Override
    public DataStoreTransaction beginTransaction() {
        return new JMSDataStoreTransaction();
    }

    @Override
    public DataStoreTransaction beginReadTransaction() {
        return new JMSDataStoreTransaction();
    }
}
