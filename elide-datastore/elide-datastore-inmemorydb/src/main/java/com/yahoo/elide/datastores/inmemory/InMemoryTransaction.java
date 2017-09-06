/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.inmemory;

import com.yahoo.elide.core.EntityDictionary;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * InMemoryDataStore transaction handler.
 * @deprecated Use {@link com.yahoo.elide.core.datastore.inmemory.InMemoryTransaction}
 */
@Deprecated
public class InMemoryTransaction extends com.yahoo.elide.core.datastore.inmemory.InMemoryTransaction {
    public InMemoryTransaction(Map<Class<?>, Map<String, Object>> dataStore,
                               EntityDictionary dictionary, Map<Class<?>, AtomicLong> typeIds) {
        super(dataStore, dictionary, typeIds);
    }
}
