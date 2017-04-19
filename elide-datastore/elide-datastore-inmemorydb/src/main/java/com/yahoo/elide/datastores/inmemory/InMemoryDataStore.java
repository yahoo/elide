/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.inmemory;

/**
 * Simple non-persistent in-memory database.
 * @deprecated Use {@link com.yahoo.elide.core.datastore.inmemory.InMemoryDataStore}
 */
@Deprecated
public class InMemoryDataStore extends com.yahoo.elide.core.datastore.inmemory.InMemoryDataStore {
    public InMemoryDataStore(Package beanPackage) {
        super(beanPackage);
    }
}
