/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.spring.datastore.config;

import com.paiondata.elide.core.datastore.DataStore;
import com.paiondata.elide.datastores.multiplex.MultiplexManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Builder used a build the DataStore.
 * <p>
 * By default this will use the {@link MultiplexManager} when there are multiple
 * DataStores.
 */
public class DataStoreBuilder {
    private final List<DataStore> dataStores = new ArrayList<>();
    private Function<DataStore[], DataStore> multiplexer = MultiplexManager::new;

    public DataStoreBuilder dataStores(List<DataStore> dataStores) {
        this.dataStores.clear();
        this.dataStores.addAll(dataStores);
        return this;
    }

    public DataStoreBuilder dataStores(Consumer<List<DataStore>> customizer) {
        customizer.accept(this.dataStores);
        return this;
    }

    public DataStoreBuilder dataStore(DataStore dataStore) {
        this.dataStores.add(dataStore);
        return this;
    }

    public DataStoreBuilder multiplexer(Function<DataStore[], DataStore> multiplexer) {
        this.multiplexer = Objects.requireNonNull(multiplexer, "Multiplexer cannot be null");
        return this;
    }

    public DataStore build() {
        if (this.dataStores.isEmpty()) {
            return null;
        }
        if (this.dataStores.size() == 1) {
            return this.dataStores.get(0);
        }
        return multiplexer.apply(this.dataStores.toArray(DataStore[]::new));
    }
}
