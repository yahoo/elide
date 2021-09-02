/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.inmemory;

import com.yahoo.elide.core.datastore.inmemory.HashMapDataStore;
import com.yahoo.elide.core.utils.ClassScanner;

/**
 * Simple non-persistent in-memory database.
 */
@Deprecated
public class InMemoryDataStore extends HashMapDataStore {
    public InMemoryDataStore(ClassScanner scanner, Package beanPackage) {
        super(scanner, beanPackage);
    }
}
