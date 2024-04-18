/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.datastores.inmemory;

import com.paiondata.elide.core.datastore.inmemory.HashMapDataStore;
import com.paiondata.elide.core.utils.ClassScanner;

/**
 * Simple non-persistent in-memory database.  Use HashMapDataStore instead.
 */
@Deprecated
public class InMemoryDataStore extends HashMapDataStore {
    public InMemoryDataStore(ClassScanner scanner, Package beanPackage) {
        super(scanner, beanPackage);
    }
}
