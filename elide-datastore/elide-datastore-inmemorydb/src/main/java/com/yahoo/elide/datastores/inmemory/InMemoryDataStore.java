/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.inmemory;

import com.yahoo.elide.core.datastore.inmemory.HashMapDataStore;

/**
 * Simple non-persistent in-memory database.
 * @deprecated Use {@link HashMapDataStore}
 */
@Deprecated
public class InMemoryDataStore extends HashMapDataStore {
    public InMemoryDataStore(Package beanPackage) {
        super(beanPackage);
    }
}
