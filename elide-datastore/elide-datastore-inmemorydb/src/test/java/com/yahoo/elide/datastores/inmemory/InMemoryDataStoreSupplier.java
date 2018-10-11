/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.inmemory;

import com.yahoo.elide.core.DataStore;

import example.Parent;

import java.util.function.Supplier;

/**
 * Supplier of InMemory Data Store.
 */
public class InMemoryDataStoreSupplier implements Supplier<DataStore> {
    @Override
    public DataStore get() {
        return new InMemoryDataStore(Parent.class.getPackage());
    }
}
